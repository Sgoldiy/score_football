package com.example.footballapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.footballapp.data.local.AppSettingsDataStore
import com.example.footballapp.data.util.ApiResult
import com.example.footballapp.domain.model.LeagueInfo
import com.example.footballapp.domain.model.TeamInfo
import com.example.footballapp.domain.repository.FootballRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class SearchUiState {
    object Idle : SearchUiState()
    object Loading : SearchUiState()
    data class Success(
        val teams: List<TeamInfo>,
        val leagues: List<LeagueInfo>
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
            .debounce(300)
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
        
        // Search teams and leagues in parallel
        viewModelScope.launch {
            try {
                val teamsResult = repository.searchTeams(query)
                val leaguesResult = repository.getLeagues() // API-Football search for leagues is sometimes limited, using getLeagues and filtering as fallback
                
                val filteredLeagues = if (leaguesResult is ApiResult.Success) {
                    leaguesResult.data.filter { it.name.contains(query, ignoreCase = true) }
                } else emptyList()

                val teams = if (teamsResult is ApiResult.Success) teamsResult.data else emptyList()

                if (teams.isEmpty() && filteredLeagues.isEmpty()) {
                    _uiState.value = SearchUiState.Error("No results found for \"$query\"")
                } else {
                    _uiState.value = SearchUiState.Success(teams, filteredLeagues)
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
