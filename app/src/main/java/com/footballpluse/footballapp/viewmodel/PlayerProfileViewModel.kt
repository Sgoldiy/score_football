package com.footballpluse.footballapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.footballpluse.footballapp.data.model.Injury
import com.footballpluse.footballapp.data.model.PlayerProfileStatisticsResponse
import com.footballpluse.footballapp.data.model.PlayerTrophy
import com.footballpluse.footballapp.data.repository.PlayerRepository
import com.footballpluse.footballapp.data.util.ApiResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class PlayerProfileUiState {
    object Loading : PlayerProfileUiState()
    data class Error(val message: String) : PlayerProfileUiState()
    data class Success(
        val player: PlayerProfileStatisticsResponse,
        val profileData: PlayerProfileData
    ) : PlayerProfileUiState()
}

data class PlayerProfileData(
    val trophies: List<PlayerTrophy> = emptyList(),
    val injuries: List<Injury> = emptyList()
)

@HiltViewModel
class PlayerProfileViewModel @Inject constructor(
    private val repository: PlayerRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<PlayerProfileUiState>(PlayerProfileUiState.Loading)
    val uiState: StateFlow<PlayerProfileUiState> = _uiState

    private var currentPlayerId: Int? = null

    fun loadPlayer(playerId: Int) {
        if (currentPlayerId == playerId) return
        currentPlayerId = playerId

        viewModelScope.launch {
            _uiState.value = PlayerProfileUiState.Loading
            try {
                val statsDef = async { repository.getPlayerStats(playerId, 2025) }
                val trophiesDef = async { repository.getTrophies(playerId) }
                val sidelinedDef = async { repository.getSidelined(playerId) }

                val statsResult = statsDef.await()
                val trophiesResult = trophiesDef.await()
                val sidelinedResult = sidelinedDef.await()

                val stats = (statsResult as? ApiResult.Success)?.data.orEmpty()
                val trophies = (trophiesResult as? ApiResult.Success)?.data.orEmpty()
                val sidelined = (sidelinedResult as? ApiResult.Success)?.data.orEmpty()

                if (stats.isNotEmpty()) {
                    _uiState.value = PlayerProfileUiState.Success(
                        player = stats.first(),
                        profileData = PlayerProfileData(
                            trophies = trophies,
                            injuries = sidelined.mapNotNull { sidelined ->
                                val player = stats.first().player
                                if (player != null) {
                                    Injury(
                                        player = player,
                                        team = null,
                                        fixture = null,
                                        league = null
                                    )
                                } else null
                            }
                        )
                    )
                } else {
                    _uiState.value = PlayerProfileUiState.Error("No player data available")
                }
            } catch (e: Exception) {
                _uiState.value = PlayerProfileUiState.Error(e.message ?: "Unknown error")
            }
        }
    }
}
