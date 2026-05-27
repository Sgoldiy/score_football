package com.example.footballapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.footballapp.data.model.PlayerProfileStatisticsResponse
import com.example.footballapp.data.remote.ApiService
import com.example.footballapp.data.util.ApiResult
import com.example.footballapp.data.util.SeasonUtils
import com.example.footballapp.domain.model.LeagueInfo
import com.example.footballapp.domain.model.Match
import com.example.footballapp.domain.repository.FootballRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
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
        val topScorers: List<PlayerProfileStatisticsResponse> = emptyList()
    ) : HomeUiState()
    data class Error(val message: String) : HomeUiState()
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: FootballRepository,
    private val apiService: ApiService
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState

    private var refreshJob: Job? = null

    init {
        loadHomeData()
    }

    fun loadHomeData() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val season = SeasonUtils.currentSeasonStartYear()

            // Fetch top scorers from all top-5 European leagues concurrently
            val top5LeagueIds = listOf(39, 140, 78, 135, 61) // EPL, La Liga, Bundesliga, Serie A, Ligue 1
            val scorersList = try {
                coroutineScope {
                    top5LeagueIds
                        .map { leagueId -> async { runCatching { apiService.getTopScorers(leagueId, season).response }.getOrDefault(emptyList()) } }
                        .flatMap { it.await() }
                        .sortedByDescending { it.statistics?.firstOrNull()?.goals?.total ?: 0 }
                        .take(10)
                }
            } catch (e: Exception) {
                emptyList()
            }

            repository.getFixturesByDate(today).collect { result ->
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
                        
                        // Priority leagues for featured
                        val priorityLeagues = setOf(39, 140, 135, 78, 61, 2, 3)
                        val featured = matches.sortedWith(
                            compareByDescending<Match> { it.isLive }
                                .thenByDescending { it.league.id in priorityLeagues }
                                .thenBy { it.timestamp }
                        ).take(10)

                        val topLeagues = matches.map { it.league }.distinctBy { it.id }
                            .sortedByDescending { it.id in priorityLeagues }

                        _uiState.value = HomeUiState.Success(
                            featuredMatches = featured,
                            liveMatches = live,
                            upcomingMatches = upcoming,
                            finishedMatches = finished,
                            topLeagues = topLeagues,
                            isLive = live.isNotEmpty(),
                            topScorers = scorersList
                        )
                    }
                    is ApiResult.Error -> {
                        if (_uiState.value !is HomeUiState.Success) {
                            _uiState.value = HomeUiState.Error(result.message)
                        }
                    }
                }
            }
        }
        
        // Start live matches polling if needed
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
}

