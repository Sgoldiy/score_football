package com.footballpluse.footballapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.footballpluse.footballapp.data.util.ApiResult
import com.footballpluse.footballapp.domain.model.PlayerDetail
import com.footballpluse.footballapp.domain.repository.FootballRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val repository: FootballRepository
) : ViewModel() {

    private val _playerState = MutableStateFlow<ApiResult<PlayerDetail>>(ApiResult.Loading)
    val playerState: StateFlow<ApiResult<PlayerDetail>> = _playerState

    fun loadPlayerData(playerId: Int, season: Int) {
        viewModelScope.launch {
            _playerState.value = ApiResult.Loading
            _playerState.value = repository.getPlayerDetail(playerId, season)
        }
    }
}
