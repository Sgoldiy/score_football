package com.example.footballapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.footballapp.data.util.ApiResult
import com.example.footballapp.domain.model.MatchDetail
import com.example.footballapp.domain.repository.FootballRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FixtureDetailViewModel @Inject constructor(
    private val repository: FootballRepository
) : ViewModel() {

    private val _detailState = MutableStateFlow<ApiResult<MatchDetail>>(ApiResult.Loading)
    val detailState: StateFlow<ApiResult<MatchDetail>> = _detailState

    fun loadFixtureDetails(fixtureId: Int) {
        viewModelScope.launch {
            _detailState.value = ApiResult.Loading
            _detailState.value = repository.getMatchDetail(fixtureId)
        }
    }
}
