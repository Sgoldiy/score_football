package com.example.footballapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.footballapp.data.local.AppSettingsDataStore
import com.example.footballapp.data.util.ApiResult
import com.example.footballapp.domain.model.LeagueInfo
import com.example.footballapp.domain.model.TeamInfo
import com.example.footballapp.domain.repository.FootballRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FavoritesState(
    val favoriteLeagues: Set<Int> = emptySet(),
    val favoriteTeams: Set<Int> = emptySet(),
    val leagueDetails: List<LeagueInfo> = emptyList(),
    val teamDetails: List<TeamInfo> = emptyList(),
    val isLoading: Boolean = false
)

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val dataStore: AppSettingsDataStore,
    private val repository: FootballRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FavoritesState())
    val uiState: StateFlow<FavoritesState> = _uiState.asStateFlow()

    init {
        combine(
            dataStore.followedLeagues,
            dataStore.followedTeams
        ) { leagues, teams ->
            FavoritesState(favoriteLeagues = leagues, favoriteTeams = teams)
        }.onEach { state ->
            _uiState.value = state.copy(isLoading = true)
            resolveNames(state.favoriteLeagues, state.favoriteTeams)
        }.launchIn(viewModelScope)
    }

    private suspend fun resolveNames(leagueIds: Set<Int>, teamIds: Set<Int>) {
        var leagueDetails = emptyList<LeagueInfo>()
        var teamDetails = emptyList<TeamInfo>()

        if (leagueIds.isNotEmpty()) {
            val leaguesResult = repository.getLeagues()
            if (leaguesResult is ApiResult.Success) {
                leagueDetails = leaguesResult.data.filter { it.id in leagueIds }
            }
        }

        if (teamIds.isNotEmpty()) {
            teamDetails = teamIds.mapNotNull { id ->
                val result = repository.searchTeams("")
                if (result is ApiResult.Success) {
                    result.data.find { it.id == id }
                } else null
            }
        }

        _uiState.update {
            it.copy(leagueDetails = leagueDetails, teamDetails = teamDetails, isLoading = false)
        }
    }

    fun toggleLeague(leagueId: Int) {
        viewModelScope.launch {
            dataStore.toggleLeagueFollowed(leagueId)
        }
    }

    fun toggleTeam(teamId: Int) {
        viewModelScope.launch {
            dataStore.toggleTeamFollowed(teamId)
        }
    }
}
