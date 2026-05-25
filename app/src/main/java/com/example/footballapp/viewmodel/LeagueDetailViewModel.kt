package com.example.footballapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    val fixtures: ApiResult<List<Match>> = ApiResult.Loading
)

@HiltViewModel
class LeagueDetailViewModel @Inject constructor(
    private val repository: FootballRepository
) : ViewModel() {

    private val _state = MutableStateFlow(LeagueDetailState())
    val state: StateFlow<LeagueDetailState> = _state.asStateFlow()

    fun load(leagueId: Int) {
        _state.update { LeagueDetailState() }

        viewModelScope.launch {
            val leaguesResult = repository.getLeagues()
            val league = when (leaguesResult) {
                is ApiResult.Success -> leaguesResult.data.find { it.id == leagueId }
                else -> null
            }
            _state.update { it.copy(leagueInfo = league) }

            val season = league?.season ?: Calendar.getInstance().get(Calendar.YEAR)

            repository.getStandings(leagueId, season).collectLatest { standings ->
                _state.update { it.copy(standings = standings) }
            }
        }

        viewModelScope.launch {
            val leaguesResult = repository.getLeagues()
            val league = when (leaguesResult) {
                is ApiResult.Success -> leaguesResult.data.find { it.id == leagueId }
                else -> null
            }

            val season = league?.season ?: Calendar.getInstance().get(Calendar.YEAR)

            repository.getFixturesByLeagueSeason(leagueId, season).collectLatest { fixtures ->
                _state.update { it.copy(fixtures = fixtures) }
            }
        }
    }
}
