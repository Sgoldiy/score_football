package com.example.footballapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.footballapp.data.model.FixtureResponse
import com.example.footballapp.data.model.PlayerProfileStatisticsResponse
import com.example.footballapp.data.remote.ApiService
import com.example.footballapp.data.repository.FixturesRepository
import com.example.footballapp.data.util.ApiResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: FixturesRepository,
    private val apiService: ApiService
) : ViewModel() {

    private val _homeState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val homeState: StateFlow<HomeUiState> = _homeState

    init {
        fetchHomeContent()
    }

    fun fetchHomeContent() {
        viewModelScope.launch {
            _homeState.value = HomeUiState.Loading
            
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val result = repository.getFixturesByDate(today)
            
            when (result) {
                is ApiResult.Success -> {
                    val allMatches = result.data
                    val liveMatches = allMatches.filter { it.isLiveMatch() }.sortedWith(
                        compareByDescending<FixtureResponse> { isPriorityLeague(it) }
                            .thenByDescending { it.fixture?.status?.elapsed ?: 0 }
                    )
                    val upcomingMatches = allMatches.filter { it.isUpcomingMatch() }
                        .sortedBy { it.fixture?.timestamp ?: Long.MAX_VALUE }
                    val finishedMatches = allMatches.filter { it.isFinishedMatch() }
                        .sortedByDescending { it.fixture?.timestamp ?: 0L }
                    val priorityMatches = allMatches.sortedWith(
                        compareByDescending<FixtureResponse> { it.isLiveMatch() }
                            .thenByDescending { isPriorityLeague(it) }
                            .thenBy { it.fixture?.timestamp ?: Long.MAX_VALUE }
                    )
                    val topLeagues = allMatches
                        .mapNotNull { it.league }
                        .distinctBy { it.id }
                        .sortedWith(compareByDescending<com.example.footballapp.data.model.League> {
                            it.id in PRIORITY_LEAGUE_IDS
                        }.thenBy { it.name.orEmpty() })
                    val leaderboardLeague = priorityMatches.firstOrNull { it.league?.id in PRIORITY_LEAGUE_IDS }?.league
                        ?: priorityMatches.firstOrNull()?.league
                    val topPlayers = runCatching {
                        if (leaderboardLeague?.id != null && leaderboardLeague.season != null) {
                            apiService.getTopScorers(leaderboardLeague.id, leaderboardLeague.season).response
                        } else {
                            emptyList()
                        }
                    }.getOrDefault(emptyList())

                    _homeState.value = HomeUiState.Success(
                        featuredMatches = when {
                            liveMatches.isNotEmpty() -> liveMatches
                            priorityMatches.isNotEmpty() -> priorityMatches
                            else -> allMatches
                        },
                        liveMatches = liveMatches,
                        upcomingMatches = upcomingMatches,
                        finishedMatches = finishedMatches,
                        topLeagues = topLeagues,
                        topPlayers = topPlayers,
                        isLive = liveMatches.isNotEmpty()
                    )
                }
                is ApiResult.Error -> {
                    _homeState.value = HomeUiState.Error(result.message)
                }
                ApiResult.Loading -> {
                    _homeState.value = HomeUiState.Loading
                }
            }
        }
    }

    private fun isPriorityLeague(match: FixtureResponse): Boolean {
        val leagueId = match.league?.id
        if (leagueId in PRIORITY_LEAGUE_IDS) return true

        val leagueName = match.league?.name.orEmpty().lowercase(Locale.ROOT)
        return PRIORITY_LEAGUE_NAME_HINTS.any { leagueName.contains(it) }
    }

    private fun FixtureResponse.isLiveMatch(): Boolean {
        return fixture?.status?.short in LIVE_STATUS
    }

    private fun FixtureResponse.isUpcomingMatch(): Boolean {
        return fixture?.status?.short in UPCOMING_STATUS
    }

    private fun FixtureResponse.isFinishedMatch(): Boolean {
        return fixture?.status?.short in FINISHED_STATUS
    }

    companion object {
        // API-Football league ids: top five leagues + UCL + UEL.
        private val PRIORITY_LEAGUE_IDS = setOf(39, 140, 135, 78, 61, 2, 3)
        private val PRIORITY_LEAGUE_NAME_HINTS = listOf(
            "premier league",
            "la liga",
            "serie a",
            "bundesliga",
            "ligue 1",
            "uefa champions league",
            "uefa europa league"
        )
        private val LIVE_STATUS = setOf("1H", "2H", "HT", "ET", "BT", "P", "INT", "LIVE")
        private val UPCOMING_STATUS = setOf("TBD", "NS")
        private val FINISHED_STATUS = setOf("FT", "AET", "PEN")
    }
}

sealed class HomeUiState {
    object Loading : HomeUiState()
    data class Success(
        val featuredMatches: List<FixtureResponse>,
        val liveMatches: List<FixtureResponse>,
        val upcomingMatches: List<FixtureResponse>,
        val finishedMatches: List<FixtureResponse>,
        val topLeagues: List<com.example.footballapp.data.model.League>,
        val topPlayers: List<PlayerProfileStatisticsResponse>,
        val isLive: Boolean
    ) : HomeUiState()
    data class Error(val message: String) : HomeUiState()
}
