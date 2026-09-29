package com.footballpluse.footballapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.footballpluse.footballapp.data.mapper.*
import com.footballpluse.footballapp.data.model.FixtureResponse
import com.footballpluse.footballapp.data.model.PlayerProfileStatisticsResponse
import com.footballpluse.footballapp.data.model.StandingRecord
import com.footballpluse.footballapp.data.remote.ApiService
import com.footballpluse.footballapp.data.util.ApiResult
import com.footballpluse.footballapp.domain.model.LeagueInfo
import com.footballpluse.footballapp.domain.model.StandingItem
import com.footballpluse.footballapp.domain.repository.FootballRepository
import com.footballpluse.footballapp.ui.screens.leagues.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LeagueDetailUiState(
    val leagueInfo: LeagueInfo? = null,
    val standings: ApiResult<List<StandingRowUiModel>> = ApiResult.Loading,
    val fixtures: ApiResult<List<FixtureUiModel>> = ApiResult.Loading,
    val topScorers: ApiResult<List<PlayerStatUiModel>> = ApiResult.Loading,
    val topAssists: ApiResult<List<PlayerStatUiModel>> = ApiResult.Loading,
    val topYellowCards: ApiResult<List<PlayerStatUiModel>> = ApiResult.Loading,
    val topRedCards: ApiResult<List<PlayerStatUiModel>> = ApiResult.Loading,
    val teams: ApiResult<List<TeamUiModel>> = ApiResult.Loading,
    val seasonStats: SeasonStatsUiModel? = null,
    val h2hData: ApiResult<H2HUiModel> = ApiResult.Loading,
    val selectedTeamA: TeamUiModel? = null,
    val selectedTeamB: TeamUiModel? = null
)

@HiltViewModel
class LeagueDetailViewModel @Inject constructor(
    private val repository: FootballRepository,
    private val apiService: ApiService
) : ViewModel() {

    private val _state = MutableStateFlow(LeagueDetailUiState())
    val state: StateFlow<LeagueDetailUiState> = _state.asStateFlow()

    private var leagueId: Int = 0
    private var season: Int = 2025


    fun load(leagueId: Int, season: Int) {
        this.leagueId = leagueId
        this.season = season
        _state.update { LeagueDetailUiState() }

        viewModelScope.launch {
            val leaguesResult = repository.getLeagues()
            val league = when (leaguesResult) {
                is ApiResult.Success -> leaguesResult.data.find { it.id == leagueId }
                else -> null
            }
            _state.update { it.copy(leagueInfo = league) }

            launch { loadStandings(leagueId, season) }
            launch { loadFixtures(leagueId, season) }
            launch {                loadTopScorers(leagueId, season) }
            launch { loadTopAssists(leagueId, season) }
            launch { loadTopYellowCards(leagueId, season) }
            launch { loadTopRedCards(leagueId, season) }
            launch { loadTeams(leagueId, season) }
        }
    }

    fun selectTeamA(team: TeamUiModel?) {
        _state.update { it.copy(selectedTeamA = team) }
        val a = team ?: return
        val b = _state.value.selectedTeamB ?: return
        loadH2H(a.id, b.id)
    }

    fun selectTeamB(team: TeamUiModel?) {
        _state.update { it.copy(selectedTeamB = team) }
        val b = team ?: return
        val a = _state.value.selectedTeamA ?: return
        loadH2H(a.id, b.id)
    }
    private fun loadH2H(teamAId: Int, teamBId: Int) {
        _state.update { it.copy(h2hData = ApiResult.Loading) }
        viewModelScope.launch {
            try {
                val fixtures = apiService.getHeadToHead(
                    firstTeamId = teamAId.toString(),
                    secondTeamId = teamBId.toString()
                ).allEvents().toFixtureResponseList()
                val teamAWins = fixtures.count { f ->
                    when {
                        f.teams?.home?.id == teamAId && f.teams?.home?.winner == true -> true
                        f.teams?.away?.id == teamAId && f.teams?.away?.winner == true -> true
                        else -> false
                    }
                }
                val teamBWins = fixtures.count { f ->
                    when {
                        f.teams?.home?.id == teamBId && f.teams?.home?.winner == true -> true
                        f.teams?.away?.id == teamBId && f.teams?.away?.winner == true -> true
                        else -> false
                    }
                }
                val draws = fixtures.count { f ->
                    f.goals?.home != null && f.goals?.away != null &&
                            f.goals.home == f.goals.away &&
                            (f.fixture?.status?.short in listOf("FT", "AET", "PEN"))
                }

                val meetings = fixtures.map { f ->
                    val homeId = f.teams?.home?.id ?: 0
                    val awayId = f.teams?.away?.id ?: 0
                    val score = "${f.goals?.home ?: "-"} - ${f.goals?.away ?: "-"}"
                    val winnerId = when {
                        f.teams?.home?.winner == true -> homeId
                        f.teams?.away?.winner == true -> awayId
                        else -> null
                    }
                    PastMeeting(
                        date = f.fixture?.date?.take(10) ?: "",
                        score = score,
                        competition = f.league?.name ?: "",
                        winnerId = winnerId
                    )
                }

                val goalsA = fixtures.sumOf { f ->
                    when {
                        f.teams?.home?.id == teamAId -> f.goals?.home ?: 0
                        f.teams?.away?.id == teamAId -> f.goals?.away ?: 0
                        else -> 0
                    }
                }
                val goalsB = fixtures.sumOf { f ->
                    when {
                        f.teams?.home?.id == teamBId -> f.goals?.home ?: 0
                        f.teams?.away?.id == teamBId -> f.goals?.away ?: 0
                        else -> 0
                    }
                }
                val comparisons = listOf(
                    ComparisonStat("Wins", teamAWins.toFloat(), teamBWins.toFloat(), "$teamAWins", "$teamBWins"),
                    ComparisonStat("Goals", goalsA.toFloat(), goalsB.toFloat(), "$goalsA", "$goalsB")
                )

                val model = H2HUiModel(
                    teamAWins = teamAWins,
                    teamBWins = teamBWins,
                    draws = draws,
                    lastMeetings = meetings,
                    comparisonStats = comparisons
                )
                _state.update { it.copy(h2hData = ApiResult.Success(model)) }
            } catch (e: Exception) {
                _state.update { it.copy(h2hData = ApiResult.Error(e.message ?: "Failed")) }
            }
        }
    }

    private suspend fun loadStandings(leagueId: Int, season: Int) {
        repository.getStandings(leagueId, season).collectLatest { result ->
            when (result) {
                is ApiResult.Success -> {
                    val rawRecords = try {
                        apiService.getStandings(leagueId.toString())
                            .toStanding().league?.standings?.firstOrNull() ?: emptyList()
                    } catch (_: Exception) { emptyList() }

                    val uiModels = result.data.map { standing ->
                        val record = rawRecords.find { it.team?.id == standing.team.id }
                        standing.toUiModel(record)
                    }
                    _state.update { it.copy(standings = ApiResult.Success(uiModels)) }
                    computeSeasonStats(ApiResult.Success(uiModels), _state.value.fixtures)
                }
                is ApiResult.Error -> _state.update { it.copy(standings = result) }
                is ApiResult.Loading -> _state.update { it.copy(standings = ApiResult.Loading) }
            }
        }
    }

    private fun StandingItem.toUiModel(record: StandingRecord?): StandingRowUiModel {
        return StandingRowUiModel(
            rank = rank, team = TeamUiModel(team.id, team.name, team.logo),
            points = points, goalsDiff = goalsDiff, played = played,
            win = win, draw = draw, lose = lose,
            goalsFor = goalsFor, goalsAgainst = goalsAgainst, form = form,
            homePlayed = record?.home?.played ?: 0,
            homeWon = record?.home?.win ?: 0,
            homeDraw = record?.home?.draw ?: 0,
            homeLost = record?.home?.lose ?: 0,
            homeGoalsFor = record?.home?.goals?.goalsFor ?: 0,
            homeGoalsAgainst = record?.home?.goals?.against ?: 0,
            awayPlayed = record?.away?.played ?: 0,
            awayWon = record?.away?.win ?: 0,
            awayDraw = record?.away?.draw ?: 0,
            awayLost = record?.away?.lose ?: 0,
            awayGoalsFor = record?.away?.goals?.goalsFor ?: 0,
            awayGoalsAgainst = record?.away?.goals?.against ?: 0
        )
    }

    /**
     * get_events requires a date window (from/to); use the season span with a
     * rolling-window fallback.
     */
    private suspend fun leagueEvents(leagueId: Int, season: Int): List<com.footballpluse.footballapp.data.model.ApiEvent> {
        return try {
            apiService.getEvents(from = "$season-07-01", to = "${season + 1}-06-30", leagueId = leagueId.toString())
        } catch (e: Exception) {
            apiService.getEvents(
                from = com.footballpluse.footballapp.data.repository.FootballRepositoryImpl.daysFromToday(-200),
                to = com.footballpluse.footballapp.data.repository.FootballRepositoryImpl.daysFromToday(200),
                leagueId = leagueId.toString()
            )
        }
    }

    private suspend fun loadFixtures(leagueId: Int, season: Int) {
        try {
            _state.update { it.copy(fixtures = ApiResult.Loading) }
            val uiModels = leagueEvents(leagueId, season)
                .toFixtureResponseList().map { it.toFixtureUiModel() }

            _state.update { it.copy(fixtures = ApiResult.Success(uiModels)) }
            computeSeasonStats(_state.value.standings, ApiResult.Success(uiModels))
        } catch (e: Exception) {
            // Fallback: try repository
            repository.getFixturesByLeagueSeason(leagueId, season).collectLatest { result ->
                when (result) {
                    is ApiResult.Success -> {
                        val uiModels = result.data.map { match ->
                            FixtureUiModel(
                                id = match.id.toString(),
                                homeTeam = TeamUiModel(match.homeTeam.id, match.homeTeam.name, match.homeTeam.logo),
                                awayTeam = TeamUiModel(match.awayTeam.id, match.awayTeam.name, match.awayTeam.logo),
                                homeScore = match.homeScore,
                                awayScore = match.awayScore,
                                status = when {
                                    match.isLive -> MatchStatusUi.LIVE
                                    match.status.short in listOf("FT", "AET", "PEN") -> MatchStatusUi.COMPLETED
                                    else -> MatchStatusUi.UPCOMING
                                },
                                minute = match.elapsed,
                                goalEvents = emptyList(),
                                yellowCards = 0, redCards = 0, attendance = null,
                                kickoffTime = try {
                                    java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                                        .format(java.util.Date(match.timestamp * 1000L))
                                } catch (_: Exception) { null }
                            )
                        }
                        _state.update { it.copy(fixtures = ApiResult.Success(uiModels)) }
                        computeSeasonStats(_state.value.standings, ApiResult.Success(uiModels))
                    }
                    is ApiResult.Error -> _state.update { it.copy(fixtures = result) }
                    is ApiResult.Loading -> _state.update { it.copy(fixtures = ApiResult.Loading) }
                }
            }
        }
    }

    private fun FixtureResponse.toFixtureUiModel(): FixtureUiModel {
        val homeTeamId = teams?.home?.id ?: 0
        val awayTeamId = teams?.away?.id ?: 0
        val goalEvents = (events ?: emptyList())
            .filter { it.type == "Goal" }
            .map { event ->
                GoalEvent(
                    minute = event.time?.elapsed ?: 0,
                    playerName = event.player?.name ?: "Unknown",
                    teamId = event.team?.id ?: 0,
                    isHome = event.team?.id == homeTeamId
                )
            }
        val cards = (events ?: emptyList()).filter { it.type == "Card" }
        val yellows = cards.count { it.detail?.contains("yellow", ignoreCase = true) == true }
        val reds = cards.count { it.detail?.contains("red", ignoreCase = true) == true }

        val liveStatuses = listOf("1H", "2H", "HT", "ET", "BT", "P", "INT", "LIVE")
        val short = fixture?.status?.short ?: ""
        val status = when {
            short in liveStatuses -> MatchStatusUi.LIVE
            short in listOf("FT", "AET", "PEN") -> MatchStatusUi.COMPLETED
            else -> MatchStatusUi.UPCOMING
        }

        val kickoff = try {
            val ts = fixture?.timestamp ?: 0L
            if (ts > 0L) {
                java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                    .format(java.util.Date(ts * 1000L))
            } else null
        } catch (_: Exception) { null }

        return FixtureUiModel(
            id = (fixture?.id ?: 0).toString(),
            homeTeam = TeamUiModel(homeTeamId, teams?.home?.name ?: "", teams?.home?.logo),
            awayTeam = TeamUiModel(awayTeamId, teams?.away?.name ?: "", teams?.away?.logo),
            homeScore = goals?.home,
            awayScore = goals?.away,
            status = status,
            minute = fixture?.status?.elapsed,
            goalEvents = goalEvents,
            yellowCards = yellows,
            redCards = reds,
            attendance = null,
            kickoffTime = kickoff
        )
    }

    private suspend fun loadTopScorers(leagueId: Int, season: Int) {
        try {
            val rawScorers = apiService.getTopScorers(leagueId.toString())
            // v3 topscorers have no player_image: build badge-CDN URLs from player_key
            val response = rawScorers
                .map { scorer ->
                    val key = scorer.player_id
                    if (scorer.player_image == null && key != null && key > 0) {
                        scorer.copy(player_image = "https://apiv3.apifootball.com/badges/players/${key}.png")
                    } else scorer
                }
                .map { it.toPlayerProfileStatisticsResponse() }
            val maxVal = response.maxOfOrNull { it.statistics?.firstOrNull()?.goals?.total ?: 0 } ?: 1
            val models = response.mapIndexed { idx, entry ->
                val stats = entry.statistics?.firstOrNull()
                val valTotal = stats?.goals?.total ?: 0
                PlayerStatUiModel(
                    rank = idx + 1,
                    playerName = entry.player?.name ?: "Player",
                    clubName = stats?.team?.name ?: "",
                    avatarUrl = entry.player?.photo,
                    statValue = valTotal,
                    secondaryStatLabel = if (stats?.games?.appearances ?: 0 > 0)
                        String.format("%.1f per game", valTotal.toFloat() / (stats?.games?.appearances ?: 1)) else "",
                    progressFraction = if (maxVal > 0) valTotal.toFloat() / maxVal else 0f
                )
            }
            _state.update { it.copy(topScorers = ApiResult.Success(models)) }
        } catch (e: Exception) {
            _state.update { it.copy(topScorers = ApiResult.Error(e.message ?: "Failed")) }
        }
    }

    private suspend fun loadTopAssists(leagueId: Int, season: Int) {
        _state.update { it.copy(topAssists = ApiResult.Error("Not available in current API version")) }
    }

    private suspend fun loadTopYellowCards(leagueId: Int, season: Int) {
        _state.update { it.copy(topYellowCards = ApiResult.Error("Not available in current API version")) }
    }

    private suspend fun loadTopRedCards(leagueId: Int, season: Int) {
        _state.update { it.copy(topRedCards = ApiResult.Error("Not available in current API version")) }
    }

    private suspend fun loadTeams(leagueId: Int, season: Int) {
        try {
            val teams = apiService.getTeams(leagueId = leagueId.toString())
            val models = teams.map { TeamUiModel(it.team_key.toIntOr(0), it.team_name ?: "", it.team_badge) }
            _state.update { it.copy(teams = ApiResult.Success(models)) }
        } catch (e: Exception) {
            _state.update { it.copy(teams = ApiResult.Error(e.message ?: "Failed")) }
        }
    }

    private fun computeSeasonStats(
        standingsResult: ApiResult<List<StandingRowUiModel>>,
        fixturesResult: ApiResult<List<FixtureUiModel>>
    ) {
        if (standingsResult !is ApiResult.Success) return
        val standings = standingsResult.data
        if (standings.isEmpty()) return
        val fixtures = (fixturesResult as? ApiResult.Success)?.data.orEmpty()
        val finished = fixtures.filter { it.status == MatchStatusUi.COMPLETED }

        // Total goals: finished fixtures are ground truth; standings GF sum is the fallback.
        val fixtureGoals = finished.sumOf { (it.homeScore ?: 0) + (it.awayScore ?: 0) }
        // FC goal-timing removed with the FC layer; fixture scores are the total.
        val goalTimingTotal = 0
        val totalGoals = when {
            finished.isNotEmpty() -> fixtureGoals
            goalTimingTotal > 0 -> goalTimingTotal
            else -> standings.sumOf { it.goalsFor }
        }
        val totalPlayed = if (finished.isNotEmpty()) finished.size else standings.sumOf { it.played } / 2
        val avgGoals = if (totalPlayed > 0) totalGoals.toFloat() / totalPlayed else 0f

        // Most common scoreline — real, from finished fixtures (null when none available)
        val mostCommon = finished
            .filter { it.homeScore != null && it.awayScore != null }
            .map { "${it.homeScore}\u2013${it.awayScore}" }
            .groupingBy { it }
            .eachCount()
            .maxWithOrNull(compareBy({ it.value }, { it.key }))?.key

        // Cards — real counts summed from finished fixtures (null when no fixture data)
        val totalYellow = if (finished.isNotEmpty()) finished.sumOf { it.yellowCards } else null
        val totalRed = if (finished.isNotEmpty()) finished.sumOf { it.redCards } else null

        // Biggest win — largest goal margin among finished fixtures
        val biggest = finished
            .filter { it.homeScore != null && it.awayScore != null }
            .maxByOrNull { kotlin.math.abs((it.homeScore ?: 0) - (it.awayScore ?: 0)) }
        val biggestWin = biggest?.let { "${it.homeTeam.name} ${it.homeScore}\u2013${it.awayScore} ${it.awayTeam.name}" }

        // Goal timing bands: the FC /goal-timing/ source is gone; upstream has
        // no equivalent, so the Stats tab bands are zeros.
        val goalBands = listOf(
            GoalBand("1\u201315", 0), GoalBand("16\u201330", 0),
            GoalBand("31\u201345", 0), GoalBand("46\u201360", 0),
            GoalBand("61\u201375", 0), GoalBand("76\u201390+", 0)
        )

        // Real form: last 5 finished matches per team (standings have no form field in this API)
        val resultsByTeam = mutableMapOf<Int, MutableList<Pair<Char, Int>>>()
        finished.forEach { f ->
            val hs = f.homeScore ?: return@forEach
            val aws = f.awayScore ?: return@forEach
            val homeRes = if (hs > aws) 'W' else if (hs == aws) 'D' else 'L'
            val awayRes = if (aws > hs) 'W' else if (hs == aws) 'D' else 'L'
            resultsByTeam.getOrPut(f.homeTeam.id) { mutableListOf() }
                .add(homeRes to (if (homeRes == 'W') 3 else if (homeRes == 'D') 1 else 0))
            resultsByTeam.getOrPut(f.awayTeam.id) { mutableListOf() }
                .add(awayRes to (if (awayRes == 'W') 3 else if (awayRes == 'D') 1 else 0))
        }
        val formRows = standings.mapNotNull { s ->
            val last5 = resultsByTeam[s.team.id]?.takeLast(5) ?: return@mapNotNull null
            FormTeamRow(
                teamName = s.team.name,
                form = String(last5.map { it.first }.toCharArray()),
                pointsGained = last5.sumOf { it.second }
            )
        }.sortedByDescending { it.pointsGained }
        val inForm = formRows.take(5)
        val outOfForm = formRows.takeLast(5).reversed()

        val bestAttack = standings.maxByOrNull { it.goalsFor }
        val bestDefense = standings.minByOrNull { it.goalsAgainst }

        val homeWins = standings.sumOf { it.homeWon }
        val awayWins = standings.sumOf { it.awayWon }
        val draws = standings.sumOf { it.homeDraw }
        val totalResults = homeWins + awayWins + draws
        val homePct = if (totalResults > 0) homeWins.toFloat() / totalResults else 0f
        val awayPct = if (totalResults > 0) awayWins.toFloat() / totalResults else 0f
        val drawPct = if (totalResults > 0) draws.toFloat() / totalResults else 0f

        val model = SeasonStatsUiModel(
            totalGoals = totalGoals,
            avgGoalsPerGame = avgGoals,
            mostCommonScoreline = mostCommon,
            totalRedCards = totalRed,
            totalYellowCards = totalYellow,
            biggestWin = biggestWin,
            goalsByMinuteBand = goalBands,
            bestAttack = Pair(bestAttack?.team?.name ?: "", bestAttack?.goalsFor ?: 0),
            bestDefense = Pair(bestDefense?.team?.name ?: "", bestDefense?.goalsAgainst ?: 0),
            homeWinPct = homePct,
            awayWinPct = awayPct,
            drawPct = drawPct,
            formTable = FormTableData(inForm = inForm, outOfForm = outOfForm)
        )
        _state.update { it.copy(seasonStats = model) }
    }
}
