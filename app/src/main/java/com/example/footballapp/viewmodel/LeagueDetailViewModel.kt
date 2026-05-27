package com.example.footballapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.footballapp.data.model.PlayerProfileStatisticsResponse
import com.example.footballapp.data.remote.ApiService
import com.example.footballapp.data.util.ApiResult
import com.example.footballapp.domain.model.LeagueInfo
import com.example.footballapp.domain.model.Match
import com.example.footballapp.domain.model.StandingItem
import com.example.footballapp.domain.repository.FootballRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

data class LeagueDetailState(
    val leagueInfo: LeagueInfo? = null,
    val standings: ApiResult<List<StandingItem>> = ApiResult.Loading,
    val fixtures: ApiResult<List<Match>> = ApiResult.Loading,
    val topScorers: ApiResult<List<PlayerProfileStatisticsResponse>> = ApiResult.Loading,
    val topAssists: ApiResult<List<PlayerProfileStatisticsResponse>> = ApiResult.Loading
)

@HiltViewModel
class LeagueDetailViewModel @Inject constructor(
    private val repository: FootballRepository,
    private val apiService: ApiService
) : ViewModel() {

    private val _state = MutableStateFlow(LeagueDetailState())
    val state: StateFlow<LeagueDetailState> = _state.asStateFlow()

    fun load(leagueId: Int, season: Int = 2024) {
        _state.update { LeagueDetailState() }

        viewModelScope.launch {
            val leaguesResult = repository.getLeagues()
            val league = when (leaguesResult) {
                is ApiResult.Success -> leaguesResult.data.find { it.id == leagueId }
                else -> null
            }
            _state.update { it.copy(leagueInfo = league) }

            // Project-wide season policy: default to 2024 (2024/25), except competitions like World Cup which pass 2026.
            val activeSeason = season

            // Fetch Top Scorers
            launch {
                try {
                    val scorersResponse = apiService.getTopScorers(leagueId, activeSeason)
                    _state.update { it.copy(topScorers = ApiResult.Success(scorersResponse.response)) }
                } catch (e: Exception) {
                    _state.update { it.copy(topScorers = ApiResult.Error(e.message ?: "Failed to load scorers")) }
                }
            }

            // Fetch Top Assists
            launch {
                try {
                    val assistsResponse = apiService.getTopAssists(leagueId, activeSeason)
                    _state.update { it.copy(topAssists = ApiResult.Success(assistsResponse.response)) }
                } catch (e: Exception) {
                    _state.update { it.copy(topAssists = ApiResult.Error(e.message ?: "Failed to load assists")) }
                }
            }

            // Fetch Standings
            launch {
                repository.getStandings(leagueId, activeSeason).collectLatest { standings ->
                    _state.update { it.copy(standings = standings) }
                }
            }

            // Fetch Fixtures
            launch {
                repository.getFixturesByLeagueSeason(leagueId, activeSeason).collectLatest { fixtures ->
                    _state.update { it.copy(fixtures = fixtures) }
                }
            }
        }
    }
}

