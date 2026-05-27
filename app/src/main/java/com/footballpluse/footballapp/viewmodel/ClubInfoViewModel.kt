package com.footballpluse.footballapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.footballpluse.footballapp.data.model.*
import com.footballpluse.footballapp.data.util.UiState
import com.footballpluse.footballapp.domain.repository.FootballRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class ClubInfoViewModel @Inject constructor(
    private val repository: FootballRepository
) : ViewModel() {

    private val _teamInfo = MutableStateFlow<UiState<TeamInfoResponse>>(UiState.Loading)
    val teamInfo: StateFlow<UiState<TeamInfoResponse>> = _teamInfo

    private val _teamStats = MutableStateFlow<UiState<TeamStatistics>>(UiState.Loading)
    val teamStats: StateFlow<UiState<TeamStatistics>> = _teamStats

    private val _squad = MutableStateFlow<UiState<List<SquadResponse>>>(UiState.Loading)
    val squad: StateFlow<UiState<List<SquadResponse>>> = _squad

    private val _coach = MutableStateFlow<UiState<List<Coach>>>(UiState.Loading)
    val coach: StateFlow<UiState<List<Coach>>> = _coach

    private val _recentFixtures = MutableStateFlow<UiState<List<FixtureResponse>>>(UiState.Loading)
    val recentFixtures: StateFlow<UiState<List<FixtureResponse>>> = _recentFixtures

    private val _topScorers = MutableStateFlow<UiState<List<PlayerProfileStatisticsResponse>>>(UiState.Loading)
    val topScorers: StateFlow<UiState<List<PlayerProfileStatisticsResponse>>> = _topScorers

    fun loadClubData(teamId: Int, leagueId: Int) {
        viewModelScope.launch {
            launch { fetchTeamInfo(teamId) }
            launch { fetchTeamStats(teamId, leagueId) }
            launch { fetchSquad(teamId) }
            launch { fetchCoach(teamId) }
            launch { fetchRecentFixtures(teamId, leagueId) }
            launch { fetchTopScorers(leagueId) }
        }
    }

    private suspend fun fetchTeamInfo(teamId: Int) {
        try {
            val info = repository.getTeamInfoDirect(teamId)
            _teamInfo.value = UiState.Success(info)
        } catch (e: Exception) {
            _teamInfo.value = UiState.Error(e.message ?: "Error loading team info")
        }
    }

    private suspend fun fetchTeamStats(teamId: Int, leagueId: Int) {
        try {
            val stats = repository.getTeamStatisticsDirect(teamId, leagueId, 2025)
            _teamStats.value = UiState.Success(stats)
        } catch (e: Exception) {
            _teamStats.value = UiState.Error(e.message ?: "Error loading team stats")
        }
    }

    private suspend fun fetchSquad(teamId: Int) {
        try {
            val squadList = repository.getTeamSquadDirect(teamId)
            _squad.value = UiState.Success(squadList)
        } catch (e: Exception) {
            _squad.value = UiState.Error(e.message ?: "Error loading squad")
        }
    }

    private suspend fun fetchCoach(teamId: Int) {
        try {
            val coaches = repository.getTeamCoachesDirect(teamId)
            _coach.value = UiState.Success(coaches)
        } catch (e: Exception) {
            _coach.value = UiState.Error(e.message ?: "Error loading coach")
        }
    }

    private suspend fun fetchRecentFixtures(teamId: Int, leagueId: Int) {
        try {
            val fixtures = repository.getRecentFixturesDirect(teamId, leagueId, 2025)
            _recentFixtures.value = UiState.Success(fixtures)
        } catch (e: Exception) {
            _recentFixtures.value = UiState.Error(e.message ?: "Error loading fixtures")
        }
    }

    private suspend fun fetchTopScorers(leagueId: Int) {
        try {
            val scorers = repository.getTopScorersDirect(leagueId, 2025)
            _topScorers.value = UiState.Success(scorers.take(5))
        } catch (e: Exception) {
            _topScorers.value = UiState.Error(e.message ?: "Error loading top scorers")
        }
    }
}
