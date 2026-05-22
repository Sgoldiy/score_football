package com.example.footballapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.footballapp.data.local.AppSettingsDataStore
import com.example.footballapp.data.model.FixtureResponse
import com.example.footballapp.data.repository.FixturesRepository
import com.example.footballapp.data.util.ApiResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FavoritesState(
    val favoriteLeagues: Set<Int> = emptySet(),
    val favoriteTeams: Set<Int> = emptySet(),
    val upcomingMatches: ApiResult<List<FixtureResponse>> = ApiResult.Success(emptyList())
)

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val dataStore: AppSettingsDataStore,
    private val fixturesRepository: FixturesRepository
) : ViewModel() {

    val uiState: StateFlow<FavoritesState> = combine(
        dataStore.followedLeagues,
        dataStore.followedTeams
    ) { leagues, teams ->
        FavoritesState(favoriteLeagues = leagues, favoriteTeams = teams)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FavoritesState())

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
