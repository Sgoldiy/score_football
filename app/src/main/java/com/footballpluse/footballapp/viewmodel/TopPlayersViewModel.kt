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

    // FootballCharts covers domestic leagues only - UCL/UEL/UECL/World Cup/Euro
    // tabs from the previous provider were removed because that source cannot
    // serve them (their old ids now belong to unrelated FC leagues).
    val tabs = listOf(
        CompetitionTab("europe", "Europe", listOf(152, 302, 207, 175, 168), com.footballpluse.footballapp.data.util.SeasonUtils.currentSeasonStartYear()),
        CompetitionTab("premier_league", "Premier League", listOf(152), com.footballpluse.footballapp.data.util.SeasonUtils.currentSeasonStartYear()),
        CompetitionTab("la_liga", "La Liga", listOf(302), com.footballpluse.footballapp.data.util.SeasonUtils.currentSeasonStartYear()),
        CompetitionTab("serie_a", "Serie A", listOf(207), com.footballpluse.footballapp.data.util.SeasonUtils.currentSeasonStartYear()),
        CompetitionTab("bundesliga", "Bundesliga", listOf(175), com.footballpluse.footballapp.data.util.SeasonUtils.currentSeasonStartYear()),
        CompetitionTab("ligue_1", "Ligue 1", listOf(168), com.footballpluse.footballapp.data.util.SeasonUtils.currentSeasonStartYear()),
        CompetitionTab("eredivisie", "Eredivisie", listOf(88), com.footballpluse.footballapp.data.util.SeasonUtils.currentSeasonStartYear()),
        CompetitionTab("liga_portugal", "Liga Portugal", listOf(94), com.footballpluse.footballapp.data.util.SeasonUtils.currentSeasonStartYear()),
        CompetitionTab("saudi", "Saudi League", listOf(203), com.footballpluse.footballapp.data.util.SeasonUtils.currentSeasonStartYear()),
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
                    val leagueDefs = tab.leagueIds.map { leagueId ->
                        async {
                            val scorers = try {
                                apiService.getTopScorers(leagueId.toString())
                                    .map { it.toPlayerProfileStatisticsResponse() }
                            } catch (e: Exception) {
                                emptyList<PlayerProfileStatisticsResponse>()
                            }
                            val squad = try {
                                apiService.getTeams(leagueId.toString()).flatMap { team ->
                                    team.players?.map { p ->
                                        p.copy(
                                            team_name = p.team_name ?: team.team_name,
                                            team_key = p.team_key ?: team.team_key
                                        ).toPlayerProfileStatisticsResponse()
                                    } ?: emptyList()
                                }
                            } catch (e: Exception) {
                                emptyList<PlayerProfileStatisticsResponse>()
                            }
                            scorers to squad
                        }
                    }

                    val results = leagueDefs.map { it.await() }
                    val allScorers = results.flatMap { it.first }
                    val allSquad = results.flatMap { it.second }

                    if (allScorers.isEmpty() && allSquad.isEmpty()) {
                        _tabData.value = _tabData.value + (tab.id to ApiResult.Error("No data available for this competition"))
                        return@launch
                    }

                    val squadByName = allSquad.associateBy { it.player?.name?.lowercase()?.trim().orEmpty() }
                    val squadById = allSquad.associateBy { it.player?.id ?: 0 }

                    val enrichedScorers = allScorers.map { scorer ->
                        val matched = (scorer.player?.id?.let { squadById[it] })
                            ?: squadByName[scorer.player?.name?.lowercase()?.trim().orEmpty()]
                        if (matched != null) {
                            val sStat = scorer.statistics?.firstOrNull()
                            val mStat = matched.statistics?.firstOrNull()
                            val mergedStat = sStat?.copy(
                                goals = sStat.goals?.copy(
                                    assists = (mStat?.goals?.assists ?: 0).takeIf { it > 0 } ?: sStat.goals?.assists
                                ),
                                games = sStat.games?.copy(
                                    rating = mStat?.games?.rating?.takeIf { it.isNotBlank() && it != "0.0" } ?: sStat.games?.rating
                                ),
                                cards = sStat.cards?.copy(
                                    yellow = (mStat?.cards?.yellow ?: 0).takeIf { it > 0 } ?: sStat.cards?.yellow,
                                    red = (mStat?.cards?.red ?: 0).takeIf { it > 0 } ?: sStat.cards?.red
                                ),
                                team = sStat.team ?: mStat?.team
                            )
                            scorer.copy(
                                player = scorer.player?.copy(
                                    photo = scorer.player?.photo?.takeIf { it.isNotBlank() } ?: matched.player?.photo
                                ),
                                statistics = if (mergedStat != null) listOf(mergedStat) else scorer.statistics
                            )
                        } else scorer
                    }

                    val combined = (enrichedScorers + allSquad).distinctBy { it.player?.id ?: it.player?.name }

                    val scorersList = (if (enrichedScorers.isNotEmpty()) enrichedScorers else combined)
                        .filter { (it.statistics?.firstOrNull()?.goals?.total ?: 0) > 0 }
                        .sortedByDescending { it.statistics?.firstOrNull()?.goals?.total ?: 0 }
                        .take(30)

                    val assistsList = combined
                        .filter { (it.statistics?.firstOrNull()?.goals?.assists ?: 0) > 0 }
                        .sortedByDescending { it.statistics?.firstOrNull()?.goals?.assists ?: 0 }
                        .take(30)

                    val yellowCardsList = combined
                        .filter { (it.statistics?.firstOrNull()?.cards?.yellow ?: 0) > 0 }
                        .sortedByDescending { it.statistics?.firstOrNull()?.cards?.yellow ?: 0 }
                        .take(20)

                    val redCardsList = combined
                        .filter { (it.statistics?.firstOrNull()?.cards?.red ?: 0) > 0 }
                        .sortedByDescending { it.statistics?.firstOrNull()?.cards?.red ?: 0 }
                        .take(20)

                    _tabData.value = _tabData.value + (tab.id to ApiResult.Success(
                        TopPlayersData(
                            scorers = scorersList.ifEmpty { combined.take(30) },
                            assists = assistsList,
                            yellowCards = yellowCardsList,
                            redCards = redCardsList
                        )
                    ))
                } else {
                    val leagueId = tab.leagueIds.first()
                    val scorersDef = async {
                        try {
                            apiService.getTopScorers(leagueId.toString())
                                .map { it.toPlayerProfileStatisticsResponse() }
                        } catch (e: Exception) {
                            emptyList<PlayerProfileStatisticsResponse>()
                        }
                    }
                    val squadDef = async {
                        try {
                            apiService.getTeams(leagueId.toString()).flatMap { team ->
                                team.players?.map { p ->
                                    p.copy(
                                        team_name = p.team_name ?: team.team_name,
                                        team_key = p.team_key ?: team.team_key
                                    ).toPlayerProfileStatisticsResponse()
                                } ?: emptyList()
                            }
                        } catch (e: Exception) {
                            emptyList<PlayerProfileStatisticsResponse>()
                        }
                    }

                    val scorers = scorersDef.await()
                    val squadPlayers = squadDef.await()

                    if (scorers.isEmpty() && squadPlayers.isEmpty()) {
                        _tabData.value = _tabData.value + (tab.id to ApiResult.Error("No data available for this competition"))
                        return@launch
                    }

                    val squadByName = squadPlayers.associateBy { it.player?.name?.lowercase()?.trim().orEmpty() }
                    val squadById = squadPlayers.associateBy { it.player?.id ?: 0 }

                    val enrichedScorers = scorers.map { scorer ->
                        val matched = (scorer.player?.id?.let { squadById[it] })
                            ?: squadByName[scorer.player?.name?.lowercase()?.trim().orEmpty()]
                        if (matched != null) {
                            val sStat = scorer.statistics?.firstOrNull()
                            val mStat = matched.statistics?.firstOrNull()
                            val mergedStat = sStat?.copy(
                                goals = sStat.goals?.copy(
                                    assists = (mStat?.goals?.assists ?: 0).takeIf { it > 0 } ?: sStat.goals?.assists
                                ),
                                games = sStat.games?.copy(
                                    rating = mStat?.games?.rating?.takeIf { it.isNotBlank() && it != "0.0" } ?: sStat.games?.rating
                                ),
                                cards = sStat.cards?.copy(
                                    yellow = (mStat?.cards?.yellow ?: 0).takeIf { it > 0 } ?: sStat.cards?.yellow,
                                    red = (mStat?.cards?.red ?: 0).takeIf { it > 0 } ?: sStat.cards?.red
                                ),
                                team = sStat.team ?: mStat?.team
                            )
                            scorer.copy(
                                player = scorer.player?.copy(
                                    photo = scorer.player?.photo?.takeIf { it.isNotBlank() } ?: matched.player?.photo
                                ),
                                statistics = if (mergedStat != null) listOf(mergedStat) else scorer.statistics
                            )
                        } else scorer
                    }

                    val combined = (enrichedScorers + squadPlayers).distinctBy { it.player?.id ?: it.player?.name }

                    val scorersList = (if (enrichedScorers.isNotEmpty()) enrichedScorers else combined)
                        .filter { (it.statistics?.firstOrNull()?.goals?.total ?: 0) > 0 }
                        .sortedByDescending { it.statistics?.firstOrNull()?.goals?.total ?: 0 }
                        .take(30)

                    val assistsList = combined
                        .filter { (it.statistics?.firstOrNull()?.goals?.assists ?: 0) > 0 }
                        .sortedByDescending { it.statistics?.firstOrNull()?.goals?.assists ?: 0 }
                        .take(30)

                    val yellowCardsList = combined
                        .filter { (it.statistics?.firstOrNull()?.cards?.yellow ?: 0) > 0 }
                        .sortedByDescending { it.statistics?.firstOrNull()?.cards?.yellow ?: 0 }
                        .take(20)

                    val redCardsList = combined
                        .filter { (it.statistics?.firstOrNull()?.cards?.red ?: 0) > 0 }
                        .sortedByDescending { it.statistics?.firstOrNull()?.cards?.red ?: 0 }
                        .take(20)

                    _tabData.value = _tabData.value + (tab.id to ApiResult.Success(
                        TopPlayersData(
                            scorers = scorersList.ifEmpty { combined.take(30) },
                            assists = assistsList,
                            yellowCards = yellowCardsList,
                            redCards = redCardsList
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
