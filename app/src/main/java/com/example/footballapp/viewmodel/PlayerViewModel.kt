package com.example.footballapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.footballapp.data.model.PlayerProfileStatisticsResponse
import com.example.footballapp.data.model.PlayerSidelined
import com.example.footballapp.data.model.PlayerTrophy
import com.example.footballapp.data.repository.PlayerRepository
import com.example.footballapp.data.util.ApiResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val repository: PlayerRepository
) : ViewModel() {

    private val _playerState = MutableStateFlow<ApiResult<PlayerData>>(ApiResult.Loading)
    val playerState: StateFlow<ApiResult<PlayerData>> = _playerState

    fun loadPlayerData(playerId: Int, season: Int) {
        viewModelScope.launch {
            _playerState.value = ApiResult.Loading
            while (true) {
                try {
                    val statsDef = async { repository.getPlayerStats(playerId, season) }
                    val trophiesDef = async { repository.getTrophies(playerId) }
                    val sidelinedDef = async { repository.getSidelined(playerId) }

                    val stats = statsDef.await()
                    val trophies = trophiesDef.await()
                    val sidelined = sidelinedDef.await()

                    if (stats is ApiResult.Success && trophies is ApiResult.Success && sidelined is ApiResult.Success) {
                        _playerState.value = ApiResult.Success(
                            PlayerData(stats.data, trophies.data, sidelined.data)
                        )
                    } else {
                        _playerState.value = ApiResult.Error("Failed to load player data")
                    }
                } catch (e: Exception) {
                    _playerState.value = ApiResult.Error(e.message ?: "Unknown error")
                }
                delay(600_000) // 10 minutes refresh
            }
        }
    }
}

data class PlayerData(
    val stats: List<PlayerProfileStatisticsResponse>,
    val trophies: List<PlayerTrophy>,
    val sidelined: List<PlayerSidelined>
)
