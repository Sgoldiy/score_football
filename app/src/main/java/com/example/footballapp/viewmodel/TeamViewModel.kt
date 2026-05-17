package com.example.footballapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.footballapp.data.model.TeamStatistics
import com.example.footballapp.data.model.Transfer
import com.example.footballapp.data.model.Venue
import com.example.footballapp.data.repository.TeamRepository
import com.example.footballapp.data.util.ApiResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TeamViewModel @Inject constructor(
    private val repository: TeamRepository
) : ViewModel() {

    private val _teamState = MutableStateFlow<ApiResult<TeamData>>(ApiResult.Loading)
    val teamState: StateFlow<ApiResult<TeamData>> = _teamState

    fun loadTeamData(teamId: Int, leagueId: Int, season: Int) {
        viewModelScope.launch {
            _teamState.value = ApiResult.Loading
            while (true) {
                try {
                    val infoDef = async { repository.getTeamInfo(teamId) }
                    val statsDef = async { repository.getTeamStatistics(teamId, leagueId, season) }
                    val venuesDef = async { repository.getVenues(teamId) }
                    val squadDef = async { repository.getSquad(teamId) }
                    val coachesDef = async { repository.getCoaches(teamId) }
                    val transfersDef = async { repository.getTransfers(teamId) }

                    val info = infoDef.await()
                    val stats = statsDef.await()
                    val venues = venuesDef.await()
                    val squad = squadDef.await()
                    val coaches = coachesDef.await()
                    val transfers = transfersDef.await()

                    if (info is ApiResult.Success && stats is ApiResult.Success && venues is ApiResult.Success &&
                        squad is ApiResult.Success && coaches is ApiResult.Success && transfers is ApiResult.Success) {
                        _teamState.value = ApiResult.Success(
                            TeamData(info.data, stats.data, venues.data, squad.data, coaches.data, transfers.data)
                        )
                    } else {
                        _teamState.value = ApiResult.Error("Failed to load team data")
                    }
                } catch (e: Exception) {
                    _teamState.value = ApiResult.Error(e.message ?: "Unknown error")
                }
                delay(600_000) // 10 minutes refresh
            }
        }
    }
}

data class TeamData(
    val teamInfo: List<Any>,
    val statistics: TeamStatistics,
    val venues: List<Venue>,
    val squad: List<Any>,
    val coaches: List<Any>,
    val transfers: List<Transfer>
)
