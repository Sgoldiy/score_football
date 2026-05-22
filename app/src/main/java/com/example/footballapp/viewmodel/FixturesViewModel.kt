package com.example.footballapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.footballapp.data.util.ApiResult
import com.example.footballapp.domain.model.Match
import com.example.footballapp.domain.repository.FootballRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FixturesViewModel @Inject constructor(
    private val repository: FootballRepository
) : ViewModel() {

    private val _fixturesState = MutableStateFlow<ApiResult<List<Match>>>(ApiResult.Loading)
    val fixturesState: StateFlow<ApiResult<List<Match>>> = _fixturesState

    fun getFixturesByDate(date: String) {
        viewModelScope.launch {
            repository.getFixturesByDate(date).collectLatest {
                _fixturesState.value = it
            }
        }
    }

    fun getFixturesByLeagueSeason(leagueId: Int, season: Int) {
        viewModelScope.launch {
            repository.getFixturesByLeagueSeason(leagueId, season).collectLatest {
                _fixturesState.value = it
            }
        }
    }
}
