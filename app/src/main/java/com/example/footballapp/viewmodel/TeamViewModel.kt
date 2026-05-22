package com.example.footballapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.footballapp.data.util.ApiResult
import com.example.footballapp.domain.model.TeamDetail
import com.example.footballapp.domain.repository.FootballRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TeamViewModel @Inject constructor(
    private val repository: FootballRepository
) : ViewModel() {

    private val _teamState = MutableStateFlow<ApiResult<TeamDetail>>(ApiResult.Loading)
    val teamState: StateFlow<ApiResult<TeamDetail>> = _teamState

    fun loadTeamData(teamId: Int, leagueId: Int, season: Int) {
        viewModelScope.launch {
            _teamState.value = ApiResult.Loading
            _teamState.value = repository.getTeamDetail(teamId, leagueId, season)
        }
    }
}
