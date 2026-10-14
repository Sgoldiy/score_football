package com.footballpluse.footballapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.footballpluse.footballapp.data.local.AppSettingsDataStore
import com.footballpluse.footballapp.data.util.ApiResult
import com.footballpluse.footballapp.domain.model.LeagueInfo
import com.footballpluse.footballapp.domain.model.TeamInfo
import com.footballpluse.footballapp.domain.repository.FootballRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class SearchUiState {
    object Idle : SearchUiState()
    object Loading : SearchUiState()
    data class Success(
        val teams: List<TeamInfo>,
        val leagues: List<LeagueInfo>,
        val players: List<com.footballpluse.footballapp.domain.model.PlayerInfo> = emptyList()
    ) : SearchUiState()
    data class Error(val message: String) : SearchUiState()
}

@OptIn(FlowPreview::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: FootballRepository,
    private val settingsDataStore: AppSettingsDataStore
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _uiState = MutableStateFlow<SearchUiState>(SearchUiState.Idle)
    val uiState: StateFlow<SearchUiState> = _uiState

    val recentSearches = settingsDataStore.recentSearches.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    init {
        _searchQuery
            .debounce(400)
            .filter { it.length >= 3 }
            .distinctUntilChanged()
            .onEach { query ->
                performSearch(query)
            }
            .launchIn(viewModelScope)
    }

    fun onQueryChanged(query: String) {
        _searchQuery.value = query
        if (query.isEmpty()) {
            _uiState.value = SearchUiState.Idle
        }
    }

    private suspend fun performSearch(query: String) {
        _uiState.value = SearchUiState.Loading
        settingsDataStore.addRecentSearch(query)
        
        // Search teams, leagues, and players in parallel
        coroutineScope {
            try {
                val teamsDeferred = async { repository.searchTeams(query) }
                val leaguesDeferred = async { repository.getLeagues() }
                val playersDeferred = async { repository.searchPlayers(query) }

                val leaguesResult = leaguesDeferred.await()
                val teamsResult = teamsDeferred.await()
                val playersResult = playersDeferred.await()

                val filteredLeagues = if (leaguesResult is ApiResult.Success<List<LeagueInfo>>) {
                    leaguesResult.data.filter { it.name.contains(query, ignoreCase = true) }
                } else emptyList()

                val teams = if (teamsResult is ApiResult.Success<List<TeamInfo>>) teamsResult.data else emptyList()
                val players = if (playersResult is ApiResult.Success<List<com.footballpluse.footballapp.domain.model.PlayerInfo>>) playersResult.data else emptyList()

                if (teams.isEmpty() && filteredLeagues.isEmpty() && players.isEmpty()) {
                    _uiState.value = SearchUiState.Error("No results found for \"$query\"")
                } else {
                    _uiState.value = SearchUiState.Success(teams, filteredLeagues, players)
                }
            } catch (e: Exception) {
                _uiState.value = SearchUiState.Error(e.message ?: "Search failed")
            }
        }
    }

    fun clearRecentSearches() {
        viewModelScope.launch {
            settingsDataStore.clearRecentSearches()
        }
    }
}
