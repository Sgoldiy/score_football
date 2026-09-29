package com.footballpluse.footballapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.footballpluse.footballapp.data.model.*
import com.footballpluse.footballapp.data.util.UiState
import com.footballpluse.footballapp.domain.repository.FootballRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/** FC team-page deep stats: shooting, possession, xG and goal-timing per period. */
data class TeamAdvancedStatsUi(
    val teamName: String,
    val matchesWithStats: Int?,
    val shotsAvg: Double?,
    val shotsOnTargetAvg: Double?,
    val shotsAgainstAvg: Double?,
    val possessionAvg: Double?,
    val cornersAvg: Double?,
    val xgAvg: Double?,
    val xgAgainstAvg: Double?,
    val timeBins: List<String>,
    val goalBins: List<Int>,
    val firstGoalBins: List<Pair<String, Int>>,
    val seasonLabel: String?
)

@HiltViewModel
class ClubInfoViewModel @Inject constructor(
    private val repository: FootballRepository,
    private val fcApi: com.footballpluse.footballapp.data.remote.FcApiService
) : ViewModel() {

    private val _teamInfo = MutableStateFlow<UiState<TeamInfoResponse>>(UiState.Loading)
    val teamInfo: StateFlow<UiState<TeamInfoResponse>> = _teamInfo

    private val _teamStats = MutableStateFlow<UiState<TeamStatistics>>(UiState.Loading)
    val teamStats: StateFlow<UiState<TeamStatistics>> = _teamStats

    private val _squad = MutableStateFlow<UiState<List<SquadResponse>>>(UiState.Loading)
    val squad: StateFlow<UiState<List<SquadResponse>>> = _squad

    private val _coach = MutableStateFlow<UiState<List<Coach>>>(UiState.Loading)
    val coach: StateFlow<UiState<List<Coach>>> = _coach

    private val _recentFixtures = MutableStateFlow<UiState<List<FixtureResponse>>>(UiState.Loading)
    val recentFixtures: StateFlow<UiState<List<FixtureResponse>>> = _recentFixtures

    private val _topScorers = MutableStateFlow<UiState<List<PlayerProfileStatisticsResponse>>>(UiState.Loading)
    val topScorers: StateFlow<UiState<List<PlayerProfileStatisticsResponse>>> = _topScorers

    private val _advancedStats = MutableStateFlow<UiState<TeamAdvancedStatsUi>>(UiState.Loading)
    val advancedStats: StateFlow<UiState<TeamAdvancedStatsUi>> = _advancedStats

    fun loadClubData(teamId: Int, leagueId: Int) {
        viewModelScope.launch {
            launch { fetchTeamInfo(teamId) }
            launch { fetchTeamStats(teamId, leagueId) }
            launch { fetchSquad(teamId) }
            launch { fetchCoach(teamId) }
            launch { fetchRecentFixtures(teamId, leagueId) }
            launch { fetchTopScorers(leagueId) }
            launch { fetchAdvancedStats(teamId, leagueId) }
        }
    }

    /**
     * Real FC team-page stats (xG, possession, shots, goal timing). The team name
     * comes from the id bridge and the league from the team-league index that the
     * standings/fixtures flows keep warm; both are seeded for the popular clubs.
     */
    private suspend fun fetchAdvancedStats(teamId: Int, leagueId: Int) {
        try {
            val name = com.footballpluse.footballapp.data.remote.FcTeamIds.name(teamId)
            if (name.isNullOrBlank()) {
                _advancedStats.value = UiState.Error("Club not covered by the data provider")
                return
            }
            var slugs = com.footballpluse.footballapp.data.remote.FcTeamLeagueIndex.leaguesOf(name)
            if (slugs.isEmpty() && leagueId != 0) {
                com.footballpluse.footballapp.data.remote.FcLeagueCatalog.byId(leagueId)?.slug?.let { slugs = listOf(it) }
            }
            if (slugs.isEmpty()) {
                _advancedStats.value = UiState.Error("No league data for this club yet")
                return
            }
            val lg = com.footballpluse.footballapp.data.remote.FcLeagueCatalog.bySlug(slugs.first())!!
            val season = com.footballpluse.footballapp.data.remote.FcLeagueCatalog.seasonLabel(
                lg, com.footballpluse.footballapp.data.util.SeasonUtils.currentSeasonStartYear()
            )
            val page = fcApi.getTeamPage(
                lg.slug,
                com.footballpluse.footballapp.data.remote.ApiConfig.teamSlug(name) ?: name.lowercase(),
                season
            )
            _advancedStats.value = UiState.Success(
                TeamAdvancedStatsUi(
                    teamName = page.team,
                    matchesWithStats = page.stats?.matchesWithStats,
                    shotsAvg = page.stats?.shotsAvg,
                    shotsOnTargetAvg = page.stats?.shotsOnTargetAvg,
                    shotsAgainstAvg = page.stats?.shotsAgainstAvg,
                    possessionAvg = page.stats?.possessionAvg,
                    cornersAvg = page.stats?.cornersAvg,
                    xgAvg = page.stats?.xgAvg,
                    xgAgainstAvg = page.stats?.xgAgainstAvg,
                    timeBins = page.timeBins,
                    goalBins = page.goalBins,
                    firstGoalBins = page.firstGoalBins.map { it.time to it.count },
                    seasonLabel = page.season
                )
            )
        } catch (e: Exception) {
            _advancedStats.value = UiState.Error(e.message ?: "Advanced stats unavailable")
        }
    }

    private suspend fun fetchTeamInfo(teamId: Int) {
        try {
            val info = repository.getTeamInfoDirect(teamId)
            _teamInfo.value = UiState.Success(info)
        } catch (e: Exception) {
            _teamInfo.value = UiState.Error(e.message ?: "Error loading team info")
        }
    }

    private suspend fun fetchTeamStats(teamId: Int, leagueId: Int) {
        try {
            val stats = repository.getTeamStatisticsDirect(
                teamId, leagueId,
                com.footballpluse.footballapp.data.util.SeasonUtils.currentSeasonStartYear()
            )
            _teamStats.value = UiState.Success(stats)
        } catch (e: Exception) {
            _teamStats.value = UiState.Error(e.message ?: "Error loading team stats")
        }
    }

    private suspend fun fetchSquad(teamId: Int) {
        try {
            val squadList = repository.getTeamSquadDirect(teamId)
            _squad.value = UiState.Success(squadList)
        } catch (e: Exception) {
            _squad.value = UiState.Error(e.message ?: "Error loading squad")
        }
    }

    private suspend fun fetchCoach(teamId: Int) {
        try {
            val coaches = repository.getTeamCoachesDirect(teamId)
            _coach.value = UiState.Success(coaches)
        } catch (e: Exception) {
            _coach.value = UiState.Error(e.message ?: "Error loading coach")
        }
    }

    private suspend fun fetchRecentFixtures(teamId: Int, leagueId: Int) {
        try {
            val fixtures = repository.getRecentFixturesDirect(
                teamId, leagueId,
                com.footballpluse.footballapp.data.util.SeasonUtils.currentSeasonStartYear()
            )
            _recentFixtures.value = UiState.Success(fixtures)
        } catch (e: Exception) {
            _recentFixtures.value = UiState.Error(e.message ?: "Error loading fixtures")
        }
    }

    private suspend fun fetchTopScorers(leagueId: Int) {
        try {
            val scorers = repository.getTopScorersDirect(
                leagueId,
                com.footballpluse.footballapp.data.util.SeasonUtils.currentSeasonStartYear()
            )
            _topScorers.value = UiState.Success(scorers.take(5))
        } catch (e: Exception) {
            _topScorers.value = UiState.Error(e.message ?: "Error loading top scorers")
        }
    }
}
