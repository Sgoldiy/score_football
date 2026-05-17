package com.example.footballapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.footballapp.data.model.Standing
import com.example.footballapp.data.repository.StandingsRepository
import com.example.footballapp.data.util.ApiResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StandingsViewModel @Inject constructor(
    private val repository: StandingsRepository
) : ViewModel() {

    private val _standingsState = MutableStateFlow<ApiResult<List<Standing>>>(ApiResult.Loading)
    val standingsState: StateFlow<ApiResult<List<Standing>>> = _standingsState

    fun getStandings(leagueId: Int, season: Int) {
        viewModelScope.launch {
            _standingsState.value = ApiResult.Loading
            while (true) {
                _standingsState.value = repository.getStandings(leagueId, season)
                delay(300_000) // 5 minutes refresh
            }
        }
    }
}
