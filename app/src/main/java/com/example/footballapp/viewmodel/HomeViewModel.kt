package com.example.footballapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.footballapp.data.model.FixtureResponse
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
    private val repository: FixturesRepository
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
                    val liveMatches = allMatches.filter { 
                        val status = it.fixture?.status?.short
                        status == "1H" || status == "2H" || status == "HT" || status == "ET" || status == "P" || status == "BT"
                    }.sortedWith(
                        compareByDescending<FixtureResponse> { isPriorityLeague(it) }
                            .thenByDescending { it.fixture?.status?.elapsed ?: 0 }
                    )
                    
                    if (liveMatches.isNotEmpty()) {
                        _homeState.value = HomeUiState.Success(
                            featuredMatches = liveMatches,
                            isLive = true
                        )
                    } else {
                        // If no live matches, show completed matches from today or just all matches
                        val completedMatches = allMatches.filter { it.fixture?.status?.short == "FT" || it.fixture?.status?.short == "AET" || it.fixture?.status?.short == "PEN" }
                        _homeState.value = HomeUiState.Success(
                            featuredMatches = if (completedMatches.isNotEmpty()) completedMatches else allMatches,
                            isLive = false
                        )
                    }
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
    }
}

sealed class HomeUiState {
    object Loading : HomeUiState()
    data class Success(
        val featuredMatches: List<FixtureResponse>,
        val isLive: Boolean
    ) : HomeUiState()
    data class Error(val message: String) : HomeUiState()
}
