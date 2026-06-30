package com.footballpluse.footballapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.footballpluse.footballapp.data.mapper.*
import com.footballpluse.footballapp.data.model.PlayerProfileStatisticsResponse
import com.footballpluse.footballapp.data.remote.ApiService
import com.footballpluse.footballapp.data.util.ApiResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TopPlayersViewModel @Inject constructor(
    private val apiService: ApiService
) : ViewModel() {

    data class CompetitionTab(
        val id: String,
        val label: String,
        val leagueIds: List<Int>,
        val season: Int
    )

    val tabs = listOf(
        CompetitionTab("europe", "Europe", listOf(152, 302, 207, 175, 168), 2025),
        CompetitionTab("premier_league", "Premier League", listOf(152), 2025),
        CompetitionTab("la_liga", "La Liga", listOf(302), 2025),
        CompetitionTab("serie_a", "Serie A", listOf(207), 2025),
        CompetitionTab("bundesliga", "Bundesliga", listOf(175), 2025),
        CompetitionTab("ligue_1", "Ligue 1", listOf(168), 2025),
        CompetitionTab("ucl", "UCL", listOf(3), 2025),
        CompetitionTab("uel", "UEL", listOf(4), 2025),
        CompetitionTab("uecl", "UECL", listOf(683), 2025),
        CompetitionTab("world_cup", "World Cup", listOf(28), 2026),
        CompetitionTab("euro", "Euro", listOf(1), 2024),
        CompetitionTab("copa_america", "Copa America", listOf(9), 2024),
    )

    private val _tabData = MutableStateFlow<Map<String, ApiResult<TopPlayersData>>>(emptyMap())
    val tabData: StateFlow<Map<String, ApiResult<TopPlayersData>>> = _tabData

    private val _selectedTabIndex = MutableStateFlow(0)
    val selectedTabIndex: StateFlow<Int> = _selectedTabIndex

    init {
        selectTab(0)
    }

    fun selectTab(index: Int) {
        if (index !in tabs.indices) return
        _selectedTabIndex.value = index
        val tab = tabs[index]
        if (_tabData.value[tab.id] == null || _tabData.value[tab.id] is ApiResult.Loading) {
            loadDataForTab(tab)
        }
    }

    private fun loadDataForTab(tab: CompetitionTab) {
        viewModelScope.launch {
            _tabData.value = _tabData.value + (tab.id to ApiResult.Loading)
            try {
                if (tab.leagueIds.size > 1) {
                    val scorersDefs = tab.leagueIds.map { leagueId ->
                        async {
                            apiService.getTopScorers(leagueId.toString())
                                .map { it.toPlayerProfileStatisticsResponse() }
                        }
                    }
                    val assistsDefs = tab.leagueIds.map { leagueId ->
                        async { emptyList<PlayerProfileStatisticsResponse>() }
                    }
                    val yellowDefs = tab.leagueIds.map { leagueId ->
                        async { emptyList<PlayerProfileStatisticsResponse>() }
                    }
                    val redDefs = tab.leagueIds.map { leagueId ->
                        async { emptyList<PlayerProfileStatisticsResponse>() }
                    }

                    _tabData.value = _tabData.value + (tab.id to ApiResult.Success(
                        TopPlayersData(
                            scorers = scorersDefs.flatMap { it.await() }
                                .distinctBy { it.player?.id }
                                .sortedByDescending { it.statistics?.firstOrNull()?.goals?.total ?: 0 }
                                .take(30),
                            assists = assistsDefs.flatMap { it.await() }
                                .distinctBy { it.player?.id }
                                .sortedByDescending { it.statistics?.firstOrNull()?.goals?.assists ?: 0 }
                                .take(30),
                            yellowCards = yellowDefs.flatMap { it.await() }
                                .distinctBy { it.player?.id }
                                .sortedByDescending { it.statistics?.firstOrNull()?.cards?.yellow ?: 0 }
                                .take(20),
                            redCards = redDefs.flatMap { it.await() }
                                .distinctBy { it.player?.id }
                                .sortedByDescending { it.statistics?.firstOrNull()?.cards?.red ?: 0 }
                                .take(20)
                        )
                    ))
                } else {
                    val leagueId = tab.leagueIds.first()
                    val scorersDef = async {
                        apiService.getTopScorers(leagueId.toString())
                            .map { it.toPlayerProfileStatisticsResponse() }
                    }
                    val assistsDef = async { emptyList<PlayerProfileStatisticsResponse>() }
                    val yellowDef = async { emptyList<PlayerProfileStatisticsResponse>() }
                    val redDef = async { emptyList<PlayerProfileStatisticsResponse>() }

                    val scorers = scorersDef.await().sortedByDescending { it.statistics?.firstOrNull()?.goals?.total ?: 0 }.take(30)
                    val assists = assistsDef.await()
                    val yellow = yellowDef.await()
                    val red = redDef.await()

                    _tabData.value = _tabData.value + (tab.id to ApiResult.Success(
                        TopPlayersData(
                            scorers = scorers,
                            assists = assists,
                            yellowCards = yellow,
                            redCards = red
                        )
                    ))
                }
            } catch (e: Exception) {
                _tabData.value = _tabData.value + (tab.id to ApiResult.Error(e.message ?: "Unknown error"))
            }
        }
    }
}

data class TopPlayersData(
    val scorers: List<PlayerProfileStatisticsResponse>,
    val assists: List<PlayerProfileStatisticsResponse>,
    val yellowCards: List<PlayerProfileStatisticsResponse>,
    val redCards: List<PlayerProfileStatisticsResponse>
) {
    fun topRated(): List<PlayerProfileStatisticsResponse> {
        return (scorers + assists)
            .distinctBy { it.player?.id }
            .sortedByDescending { it.statistics?.firstOrNull()?.games?.rating?.toFloatOrNull() ?: 0f }
            .take(20)
    }
}
