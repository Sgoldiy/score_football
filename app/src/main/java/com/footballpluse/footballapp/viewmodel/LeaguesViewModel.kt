package com.footballpluse.footballapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.footballpluse.footballapp.data.mapper.*
import com.footballpluse.footballapp.data.model.Country
import com.footballpluse.footballapp.data.model.LeagueResponse
import com.footballpluse.footballapp.data.model.FixtureResponse
import com.footballpluse.footballapp.data.repository.LeagueRepository
import com.footballpluse.footballapp.data.remote.ApiService
import com.footballpluse.footballapp.data.local.DataStoreManager
import com.footballpluse.footballapp.data.util.ApiResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class LeagueTab {
    POPULAR, ALL, LIVE, DOMESTIC, CUPS, INTERNATIONAL, YOUTH, WOMEN
}

enum class LeagueSortOrder {
    ALPHABETICAL, MOST_MATCHES_TODAY, LIVE_FIRST, FAVORITES_FIRST
}

data class LeagueFilter(
    val hasMatchesToday: Boolean = false,
    val hasLiveMatches: Boolean = false,
    val favoritedOnly: Boolean = false,
    val continents: Set<String> = emptySet()
)

data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)

data class League(
    val id: Int,
    val name: String,
    val country: String,
    val logoUrl: String,
    val season: String,
    val leagueType: String,
    val isInternational: Boolean,
    val currentRound: String?,
    val teamCount: Int?,
    val liveCount: Int,
    val todayCount: Int,
    val isFavorited: Boolean
)

@HiltViewModel
class LeaguesViewModel @Inject constructor(
    private val repository: LeagueRepository,
    private val apiService: ApiService,
    private val dataStoreManager: DataStoreManager
) : ViewModel() {

    companion object {
        // Shared process-level cache for league details (currentRound, teamCount)
        private val stageAndTeamCache = java.util.concurrent.ConcurrentHashMap<Int, Pair<String, Int>>()
    }

    private val activeJobs = java.util.concurrent.ConcurrentHashMap<Int, Job>()

    private val _selectedTab = MutableStateFlow(LeagueTab.POPULAR)
    val selectedTab: StateFlow<LeagueTab> = _selectedTab.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _sortOrder = MutableStateFlow(LeagueSortOrder.ALPHABETICAL)
    val sortOrder: StateFlow<LeagueSortOrder> = _sortOrder.asStateFlow()

    private val _activeFilters = MutableStateFlow(LeagueFilter())
    val activeFilters: StateFlow<LeagueFilter> = _activeFilters.asStateFlow()

    private val _rawLeagues = MutableStateFlow<List<LeagueResponse>>(emptyList())
    private val _liveCountMap = MutableStateFlow<Map<Int, Int>>(emptyMap())
    private val _todayCountMap = MutableStateFlow<Map<Int, Int>>(emptyMap())
    private val _totalLiveCount = MutableStateFlow(0)
    val totalLiveCount: StateFlow<Int> = _totalLiveCount.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _triggerDetailsUpdate = MutableStateFlow(0L)

    private val _tempSortOrder = MutableStateFlow(LeagueSortOrder.ALPHABETICAL)
    val tempSortOrder: StateFlow<LeagueSortOrder> = _tempSortOrder.asStateFlow()

    private val _tempFilters = MutableStateFlow(LeagueFilter())
    val tempFilters: StateFlow<LeagueFilter> = _tempFilters.asStateFlow()

    val allLeagues: StateFlow<List<League>> = combine(
        combine(_rawLeagues, _liveCountMap, _todayCountMap) { raw, live, today -> Triple(raw, live, today) },
        combine(dataStoreManager.followedLeagues, dataStoreManager.favouriteLeagueId) { followed, onboardingFav -> Pair(followed, onboardingFav) },
        _triggerDetailsUpdate
    ) { group1, group2, _ ->
        val rawLeagues = group1.first
        val liveCounts = group1.second
        val todayCounts = group1.third

        val followedIds = group2.first
        val onboardingFavId = group2.second

        val activeFavIds = if (followedIds.isEmpty()) setOf(onboardingFavId) else followedIds

        rawLeagues
            .map { item ->
                val id = item.league?.id ?: 0
                val cachedDetails = stageAndTeamCache[id]
                val type = item.league?.type ?: "League"
                val countryName = item.country?.name ?: "International"
                val isInternational = item.league?.country == "World" || countryName == "World" || item.country?.code == null

                League(
                    id = id,
                    name = item.league?.name ?: "Unknown League",
                    country = countryName,
                    logoUrl = item.league?.logo ?: "",
                    season = item.seasons?.find { it.current == true }?.year?.toString() ?: item.league?.season?.toString() ?: "2025",
                    leagueType = type,
                    isInternational = isInternational,
                    currentRound = cachedDetails?.first,
                    teamCount = cachedDetails?.second,
                    liveCount = liveCounts[id] ?: 0,
                    todayCount = todayCounts[id] ?: 0,
                    isFavorited = id in activeFavIds
                )
            }
            .distinctBy { it.id }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val popularLeagues: StateFlow<List<League>> = allLeagues.map { list ->
        val topCompetitionsIds = setOf(152, 302, 207, 175, 168, 3, 4, 683, 1, 28)
        list.filter { it.id in topCompetitionsIds || it.isFavorited }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val liveLeagues: StateFlow<List<League>> = allLeagues.map { list ->
        list.filter { it.liveCount > 0 }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val domesticLeagues: StateFlow<List<League>> = allLeagues.map { list ->
        list.filter { it.leagueType == "League" }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val cupLeagues: StateFlow<List<League>> = allLeagues.map { list ->
        list.filter { it.leagueType == "Cup" }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val internationalLeagues: StateFlow<List<League>> = allLeagues.map { list ->
        list.filter { it.isInternational }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val youthLeagues: StateFlow<List<League>> = allLeagues.map { list ->
        list.filter {
            it.name.contains("U18", ignoreCase = true) ||
            it.name.contains("U21", ignoreCase = true) ||
            it.name.contains("U23", ignoreCase = true) ||
            it.name.contains("Youth", ignoreCase = true)
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val womenLeagues: StateFlow<List<League>> = allLeagues.map { list ->
        list.filter {
            it.name.contains("Women", ignoreCase = true) ||
            it.name.contains("WSL", ignoreCase = true) ||
            it.name.contains("NWSL", ignoreCase = true)
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // Compatibility flow for LeaguesScreen loading/error states
    val leaguesState: StateFlow<ApiResult<LeaguesData>> = combine(
        _rawLeagues,
        _isLoading,
        _errorMessage
    ) { rawLeagues, loading, error ->
        if (error != null) {
            ApiResult.Error(error)
        } else if (loading) {
            ApiResult.Loading
        } else {
            ApiResult.Success(
                LeaguesData(
                    leagues = rawLeagues,
                    countries = emptyList(),
                    seasons = emptyList()
                )
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ApiResult.Loading)

    init {
        loadLeaguesData()
        startLiveRefreshLoop()
    }

    fun onTabSelected(tab: LeagueTab) {
        _selectedTab.value = tab
    }

    fun onSearch(query: String) {
        _searchQuery.value = query
    }

    fun onToggleFavorite(leagueId: Int) {
        viewModelScope.launch {
            val current = dataStoreManager.followedLeagues.first()
            val onboardingFav = dataStoreManager.favouriteLeagueId.first()
            val baseSet = current.ifEmpty { setOf(onboardingFav) }
            val updated = baseSet.toMutableSet()
            if (updated.contains(leagueId)) {
                updated.remove(leagueId)
            } else {
                updated.add(leagueId)
            }
            dataStoreManager.setLeaguesFollowed(updated)
        }
    }

    fun toggleFavorite(leagueId: Int) {
        onToggleFavorite(leagueId)
    }

    fun updateTempSortOrder(order: LeagueSortOrder) {
        _tempSortOrder.value = order
    }

    fun updateTempFilters(filters: LeagueFilter) {
        _tempFilters.value = filters
    }

    fun onOpenFilterSheet() {
        _tempSortOrder.value = _sortOrder.value
        _tempFilters.value = _activeFilters.value
    }

    fun applyFilters(sort: LeagueSortOrder, filters: LeagueFilter) {
        _sortOrder.value = sort
        _activeFilters.value = filters
    }

    fun onApplyFilters(sort: LeagueSortOrder, filters: LeagueFilter) {
        applyFilters(sort, filters)
    }

    fun loadExtraDetails(leagueId: Int) {
        if (stageAndTeamCache.containsKey(leagueId)) return
        if (activeJobs.containsKey(leagueId)) return

        val job = viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val teamCount = try {
                    val standingsRes = apiService.getStandings(leagueId.toString())
                    standingsRes.size
                } catch (e: Exception) {
                    try {
                        val teamsRes = apiService.getTeams(leagueId = leagueId.toString())
                        teamsRes.size
                    } catch (_: Exception) { 20 }
                }

                stageAndTeamCache[leagueId] = Pair("Regular Season", teamCount)
                _triggerDetailsUpdate.update { it + 1 }
            } catch (e: Exception) {
                // Ignore background errors
            } finally {
                activeJobs.remove(leagueId)
            }
        }
        activeJobs[leagueId] = job
    }

    private fun startLiveRefreshLoop() {
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(60_000)
                refreshFixtureCountsOnly()
            }
        }
    }

    private suspend fun refreshFixtureCountsOnly() = coroutineScope {
        try {
            val todayStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
            val liveDeferred = async(kotlinx.coroutines.Dispatchers.IO) {
                try { apiService.getLivescore().toFixtureResponseList() } catch (e: Exception) { emptyList<FixtureResponse>() }
            }
            val todayDeferred = async(kotlinx.coroutines.Dispatchers.IO) {
                try { apiService.getEvents(from = todayStr, to = todayStr).toFixtureResponseList() } catch (e: Exception) { emptyList<FixtureResponse>() }
            }

            val liveFixtures = liveDeferred.await()
            val todayFixtures = todayDeferred.await()

            val liveCounts = liveFixtures
                .filter { it.league != null }
                .groupBy { it.league!!.id }
                .mapValues { it.value.size }

            val todayCounts = todayFixtures
                .filter { it.league != null }
                .groupBy { it.league!!.id }
                .mapValues { it.value.size }

            _liveCountMap.value = liveCounts
            _todayCountMap.value = todayCounts
            _totalLiveCount.value = liveFixtures.size
        } catch (e: Exception) {
            // Ignore background refresh errors
        }
    }

    fun loadLeaguesData() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val todayStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
                
                // Launch network calls concurrently
                val leaguesDeferred = async { repository.getLeagues() }
                val liveDeferred = async {
                    try {
                        apiService.getLivescore().toFixtureResponseList()
                    } catch (e: Exception) {
                        emptyList<FixtureResponse>()
                    }
                }
                val todayDeferred = async {
                    try {
                        apiService.getEvents(from = todayStr, to = todayStr).toFixtureResponseList()
                    } catch (e: Exception) {
                        emptyList<FixtureResponse>()
                    }
                }

                val leaguesResult = leaguesDeferred.await()
                
                if (leaguesResult is ApiResult.Success) {
                    _rawLeagues.value = leaguesResult.data
                    _isLoading.value = false // Emit primary data ASAP

                    // Then update counts when they arrive
                    val liveFixtures = liveDeferred.await()
                    val todayFixtures = todayDeferred.await()

                    val liveCounts = liveFixtures
                        .filter { it.league != null }
                        .groupBy { it.league!!.id }
                        .mapValues { it.value.size }

                    val todayCounts = todayFixtures
                        .filter { it.league != null }
                        .groupBy { it.league!!.id }
                        .mapValues { it.value.size }

                    _liveCountMap.value = liveCounts
                    _todayCountMap.value = todayCounts
                    _totalLiveCount.value = liveFixtures.size
                } else if (leaguesResult is ApiResult.Error) {
                    _errorMessage.value = leaguesResult.message
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Unknown error"
                _isLoading.value = false
            }
        }
    }

    private fun getContinentForCountry(countryName: String): String {
        return when (countryName) {
            "England", "Spain", "Italy", "Germany", "France", "Netherlands", "Portugal", "Belgium", "Scotland", "Wales", "Turkey", "Greece", "Austria", "Switzerland", "Croatia", "Denmark", "Ukraine", "Russia", "Poland", "Sweden", "Norway", "Europe", "UEFA" -> "Europe"
            "Brazil", "Argentina", "Colombia", "Chile", "Uruguay", "Ecuador", "Paraguay", "Peru", "Bolivia", "Venezuela", "CONMEBOL" -> "South America"
            "USA", "Mexico", "Canada", "Costa Rica", "Jamaica", "Honduras", "Panama", "CONCACAF" -> "North America"
            "Saudi Arabia", "Japan", "South Korea", "China", "Australia", "Iran", "Qatar", "UAE", "India", "Asia", "AFC" -> "Asia"
            "Egypt", "Morocco", "Algeria", "Tunisia", "Senegal", "Nigeria", "Cameroon", "Ghana", "South Africa", "Ivory Coast", "Africa", "CAF" -> "Africa"
            "New Zealand", "Fiji", "Oceania", "OFC" -> "Oceania"
            else -> "Europe" // Fallback
        }
    }
}

data class LeaguesData(
    val leagues: List<LeagueResponse>,
    val countries: List<Country>,
    val seasons: List<Int>
)
