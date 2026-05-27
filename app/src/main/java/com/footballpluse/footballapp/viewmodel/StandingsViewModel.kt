package com.footballpluse.footballapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.footballpluse.footballapp.data.util.ApiResult
import com.footballpluse.footballapp.domain.model.StandingItem
import com.footballpluse.footballapp.domain.repository.FootballRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StandingsViewModel @Inject constructor(
    private val repository: FootballRepository
) : ViewModel() {

    private val _standingsState = MutableStateFlow<ApiResult<List<StandingItem>>>(ApiResult.Loading)
    val standingsState: StateFlow<ApiResult<List<StandingItem>>> = _standingsState

    fun getStandings(leagueId: Int, season: Int) {
        viewModelScope.launch {
            repository.getStandings(leagueId, season).collectLatest {
                _standingsState.value = it
            }
        }
    }
}
