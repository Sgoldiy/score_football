package com.example.footballapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.footballapp.data.model.PlayerStatistics
import com.example.footballapp.data.repository.PlayerRepository
import com.example.footballapp.data.remote.ApiService
import com.example.footballapp.data.util.ApiResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TopPlayersViewModel @Inject constructor(
    private val apiService: ApiService
) : ViewModel() {

    private val _topPlayersState = MutableStateFlow<ApiResult<TopPlayersData>>(ApiResult.Loading)
    val topPlayersState: StateFlow<ApiResult<TopPlayersData>> = _topPlayersState

    fun loadTopPlayers(leagueId: Int, season: Int) {
        viewModelScope.launch {
            _topPlayersState.value = ApiResult.Loading
            while (true) {
                try {
                    val scorersDef = async { apiService.getTopScorers(leagueId, season) }
                    val assistsDef = async { apiService.getTopAssists(leagueId, season) }
                    val yellowCardsDef = async { apiService.getTopYellowCards(leagueId, season) }
                    val redCardsDef = async { apiService.getTopRedCards(leagueId, season) }

                    val scorers = scorersDef.await()
                    val assists = assistsDef.await()
                    val yellow = yellowCardsDef.await()
                    val red = redCardsDef.await()

                    // Here we extract from ApiResponse directly as we used apiService directly for this VM
                    _topPlayersState.value = ApiResult.Success(
                        TopPlayersData(
                            scorers.response,
                            assists.response,
                            yellow.response,
                            red.response
                        )
                    )
                } catch (e: Exception) {
                    _topPlayersState.value = ApiResult.Error(e.message ?: "Unknown error")
                }
                delay(1_800_000) // 30 minutes refresh
            }
        }
    }
}

data class TopPlayersData(
    val scorers: List<PlayerStatistics>,
    val assists: List<PlayerStatistics>,
    val yellowCards: List<PlayerStatistics>,
    val redCards: List<PlayerStatistics>
)
