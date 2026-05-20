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
import javax.inject.Inject

@HiltViewModel
class FixturesViewModel @Inject constructor(
    private val repository: FixturesRepository
) : ViewModel() {

    private val _fixturesState = MutableStateFlow<ApiResult<List<FixtureResponse>>>(ApiResult.Loading)
    val fixturesState: StateFlow<ApiResult<List<FixtureResponse>>> = _fixturesState

    fun getFixturesByDate(date: String) {
        viewModelScope.launch {
            _fixturesState.value = ApiResult.Loading
            _fixturesState.value = repository.getFixturesByDate(date)
        }
    }

    fun getFixturesByLeagueSeason(leagueId: Int, season: Int) {
        viewModelScope.launch {
            _fixturesState.value = ApiResult.Loading
            _fixturesState.value = repository.getFixturesByLeagueSeason(leagueId, season)
        }
    }
}
