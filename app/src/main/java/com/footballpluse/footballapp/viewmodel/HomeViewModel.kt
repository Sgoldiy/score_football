package com.footballpluse.footballapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.footballpluse.footballapp.data.mapper.*
import com.footballpluse.footballapp.data.model.ApiTeam
import com.footballpluse.footballapp.data.model.LeagueResponse
import com.footballpluse.footballapp.data.model.PlayerProfileStatisticsResponse
import com.footballpluse.footballapp.data.remote.ApiService
import com.footballpluse.footballapp.data.util.ApiResult
import com.footballpluse.footballapp.data.util.SeasonUtils
import com.footballpluse.footballapp.domain.model.LeagueInfo
import com.footballpluse.footballapp.domain.model.Match
import com.footballpluse.footballapp.domain.repository.FootballRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

sealed class HomeUiState {
    object Loading : HomeUiState()
    data class Success(
        val featuredMatches: List<Match>,
        val liveMatches: List<Match>,
        val upcomingMatches: List<Match>,
        val finishedMatches: List<Match>,
        val topLeagues: List<LeagueInfo>,
        val isLive: Boolean,
        val topScorers: List<PlayerProfileStatisticsResponse> = emptyList(),
        val favouriteLeagues: List<LeagueResponse> = emptyList(),
        val favouriteLeagueId: Int = 152,
        val favouriteLeagueName: String = "Premier League",
        val allApiLeagues: List<LeagueInfo> = emptyList()
    ) : HomeUiState()
    data class Error(val message: String) : HomeUiState()
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: FootballRepository,
    private val apiService: ApiService,
    private val teamRepository: com.footballpluse.footballapp.data.repository.TeamRepository,
    private val billingRepository: com.footballpluse.footballapp.data.repository.BillingRepository,
    private val dataStoreManager: com.footballpluse.footballapp.data.local.DataStoreManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState

    private val _formMap = MutableStateFlow<Map<Int, String>>(emptyMap())
    val formMap: StateFlow<Map<Int, String>> = _formMap.asStateFlow()

    private val _scorers = MutableStateFlow<List<PlayerProfileStatisticsResponse>>(emptyList())
    private val _allLeagues = MutableStateFlow<List<com.footballpluse.footballapp.data.model.ApiLeague>>(emptyList())
    val allApiLeagues: StateFlow<List<LeagueInfo>> = _allLeagues
        .map { list -> list.map { it.toLeagueInfo() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isPremium: StateFlow<Boolean> = billingRepository.isPurchased
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private var refreshJob: Job? = null

    init {
        loadHomeData()
    }

    fun fetchFormIfNeeded(teamId: Int, leagueId: Int, season: Int) {
        if (teamId == 0) return
        if (_formMap.value.containsKey(teamId)) return
        viewModelScope.launch {
            if (_formMap.value.containsKey(teamId)) return@launch
            
            // Temporary placeholder to prevent redundant requests
            _formMap.update { it + (teamId to "") }
            
            val result = teamRepository.getTeamStatistics(teamId, leagueId, season)
            if (result is ApiResult.Success) {
                val form = result.data.form ?: ""
                if (form.isNotEmpty()) {
                    _formMap.update { it + (teamId to form) }
                } else {
                    _formMap.update { it - teamId }
                }
            } else {
                _formMap.update { it - teamId }
            }
        }
    }

    fun triggerPurchase() {
        viewModelScope.launch {
            billingRepository.setPurchased(true)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun loadHomeData() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            
            // Launch background tasks for scorers in parallel
            launch {
                try {
                    val top5LeagueIds = listOf(152, 302, 175, 207, 168)
                    val scorers = coroutineScope {
                        top5LeagueIds.map { leagueId ->
                            async {
                                try {
                                    apiService.getTopScorers(leagueId.toString()).map { it.toPlayerProfileStatisticsResponse() }
                                } catch (_: Exception) {
                                    emptyList<PlayerProfileStatisticsResponse>()
                                }
                            }
                        }.awaitAll().flatten()
                            .sortedByDescending { it.statistics?.firstOrNull()?.goals?.total ?: 0 }
                            .take(10)
                    }
                    _scorers.value = scorers
                } catch (_: Exception) {}
            }

            // Launch background task for leagues
            launch {
                try {
                    val leagues = apiService.getLeagues()
                    _allLeagues.value = leagues
                } catch (_: Exception) {}
            }

            try {
                val prefFlow = combine(
                    dataStoreManager.followedLeagues,
                    dataStoreManager.favouriteLeagueId,
                    dataStoreManager.favouriteLeagueName
                ) { followed, favId, favName -> Triple(followed, favId, favName) }

                combine(
                    repository.getFixturesByDate(today),
                    prefFlow,
                    _scorers,
                    _allLeagues
                ) { result, pref, scorersList, allLeagues ->
                    val (followed, onboardingFavId, onboardingFavName) = pref
                    when (result) {
                        is ApiResult.Loading -> {
                            if (_uiState.value !is HomeUiState.Success) {
                                _uiState.value = HomeUiState.Loading
                            }
                        }
                        is ApiResult.Success -> {
                            val matches = result.data
                            val live = matches.filter { it.isLive }
                            val upcoming = matches.filter { it.status.short == "NS" || it.status.short == "TBD" }
                            val finished = matches.filter { it.status.short in listOf("FT", "AET", "PEN") }

                            val priorityLeagues = setOf(152, 302, 207, 175, 168, 88, 94, 203, 144, 187, 3, 4, 848, 28, 1, 5, 6, 15, 9)
                            val featured = matches.sortedWith(
                                compareByDescending<Match> { it.isLive }
                                    .thenByDescending { it.league.id in priorityLeagues }
                                    .thenBy { it.timestamp }
                            ).take(10)

                            val topLeagues = matches.map { it.league }.distinctBy { it.id }
                                .sortedByDescending { it.id in priorityLeagues }

                            val favLeagueIds = followed.ifEmpty { 
                                if (onboardingFavId != 0) setOf(onboardingFavId) else emptySet() 
                            }
                            
                            val favLeaguesList = allLeagues.filter { it.league_id?.toIntOrNull() in favLeagueIds }.map { it.toLeagueResponse() }
                            val apiTopLeagues = allLeagues
                                .filter { it.league_id?.toIntOrNull() in priorityLeagues }
                                .map { it.toLeagueInfo() }

                            val finalTopLeagues = (topLeagues + apiTopLeagues).distinctBy { it.id }
                            val allLeaguesInfo = allLeagues.map { it.toLeagueInfo() }

                            _uiState.value = HomeUiState.Success(
                                featuredMatches = featured,
                                liveMatches = live,
                                upcomingMatches = upcoming,
                                finishedMatches = finished,
                                topLeagues = finalTopLeagues,
                                isLive = live.isNotEmpty(),
                                topScorers = scorersList,
                                favouriteLeagues = favLeaguesList,
                                favouriteLeagueId = onboardingFavId,
                                favouriteLeagueName = onboardingFavName,
                                allApiLeagues = allLeaguesInfo
                            )
                        }
                        is ApiResult.Error -> {
                            val priorityLeagues = setOf(152, 302, 207, 175, 168, 88, 94, 203, 144, 187, 3, 4, 848, 28, 1, 5, 6, 15, 9)
                            
                            val apiTopLeagues = allLeagues
                                .filter { it.league_id?.toIntOrNull() in priorityLeagues }
                                .map { it.toLeagueInfo() }

                            val favLeagueIds = followed.ifEmpty { 
                                if (onboardingFavId != 0) setOf(onboardingFavId) else emptySet() 
                            }
                            val favLeaguesList = allLeagues.filter { it.league_id?.toIntOrNull() in favLeagueIds }.map { it.toLeagueResponse() }
                            val allLeaguesInfo = allLeagues.map { it.toLeagueInfo() }

                            if (scorersList.isNotEmpty() || apiTopLeagues.isNotEmpty()) {
                                _uiState.value = HomeUiState.Success(
                                    featuredMatches = emptyList(),
                                    liveMatches = emptyList(),
                                    upcomingMatches = emptyList(),
                                    finishedMatches = emptyList(),
                                    topLeagues = apiTopLeagues,
                                    isLive = false,
                                    topScorers = scorersList,
                                    favouriteLeagues = favLeaguesList,
                                    favouriteLeagueId = onboardingFavId,
                                    favouriteLeagueName = onboardingFavName,
                                    allApiLeagues = allLeaguesInfo
                                )
                            } else if (_uiState.value !is HomeUiState.Success) {
                                _uiState.value = HomeUiState.Error(result.message)
                            }
                        }
                    }
                }.collect()
            } catch (e: Exception) {
                if (_uiState.value !is HomeUiState.Success) {
                    _uiState.value = HomeUiState.Error(e.message ?: "Failed to load data")
                }
            }
        }
        
        startLiveUpdates()
    }

    private var liveUpdatesJob: Job? = null
    private fun startLiveUpdates() {
        liveUpdatesJob?.cancel()
        liveUpdatesJob = repository.getLiveMatches()
            .onEach { result ->
                if (result is ApiResult.Success) {
                    val currentState = _uiState.value
                    if (currentState is HomeUiState.Success) {
                        _uiState.value = currentState.copy(
                            liveMatches = result.data,
                            isLive = result.data.isNotEmpty()
                        )
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    private fun enrichWithImages(
        scorers: List<PlayerProfileStatisticsResponse>,
        teams: List<ApiTeam>
    ): List<PlayerProfileStatisticsResponse> {
        val teamBadgeMap = mutableMapOf<String, String>()
        val playerImageMap = mutableMapOf<String, String>()

        teams.forEach { team ->
            team.team_name?.let { name ->
                team.team_badge?.let { badge -> teamBadgeMap[name] = badge }
            }
            team.players?.forEach { player ->
                player.player_name?.let { name ->
                    player.player_image?.let { image -> playerImageMap[name] = image }
                }
            }
        }

        if (teamBadgeMap.isEmpty() && playerImageMap.isEmpty()) return scorers

        return scorers.map { scorer ->
            val stats = scorer.statistics?.firstOrNull()
            val needsPhoto = scorer.player?.photo == null
            val needsLogo = stats?.team?.logo == null
            if (!needsPhoto && !needsLogo) return@map scorer

            scorer.copy(
                player = if (needsPhoto) scorer.player?.copy(
                    photo = playerImageMap[scorer.player?.name]
                        ?: scorer.player?.photo
                ) else scorer.player,
                statistics = if (needsLogo) scorer.statistics?.map { s ->
                    s.copy(
                        team = s.team?.copy(
                            logo = teamBadgeMap[s.team?.name]
                                ?: s.team?.logo
                        )
                    )
                } else scorer.statistics
            )
        }
    }
}

