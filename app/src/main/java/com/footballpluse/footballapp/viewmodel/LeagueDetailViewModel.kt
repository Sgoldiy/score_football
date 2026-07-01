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
            launch { loadTopScorers(leagueId, season) }
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
                    val w = f.teams?.home?.winner == true && f.teams?.home?.id == teamAId ||
                            f.teams?.away?.winner == true && f.teams?.away?.id == teamAId
                    w
                }
                val teamBWins = fixtures.count { f ->
                    val w = f.teams?.home?.winner == true && f.teams?.home?.id == teamBId ||
                            f.teams?.away?.winner == true && f.teams?.away?.id == teamBId
                    w
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

                val comparisons = listOf(
                    ComparisonStat("Wins", teamAWins.toFloat(), teamBWins.toFloat(), "$teamAWins", "$teamBWins"),
                    ComparisonStat("Goals", fixtures.sumOf {
                        val gf = if (it.teams?.home?.id == teamAId) it.goals?.home ?: 0 else it.goals?.away ?: 0
                        gf
                    }.toFloat(), fixtures.sumOf {
                        val gf = if (it.teams?.home?.id == teamBId) it.goals?.home ?: 0 else it.goals?.away ?: 0
                        gf
                    }.toFloat(), "?", "?")
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

    private suspend fun loadFixtures(leagueId: Int, season: Int) {
        try {
            _state.update { it.copy(fixtures = ApiResult.Loading) }
            val uiModels = apiService.getEvents(leagueId = leagueId.toString())
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
        val yellows = cards.count { it.detail == "Yellow Card" }
        val reds = cards.count { it.detail == "Red Card" }

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
            val response = apiService.getTopScorers(leagueId.toString())
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

        val totalGoals = standings.sumOf { it.goalsFor }
        val totalPlayed = standings.sumOf { it.played } / 2
        val avgGoals = if (totalPlayed > 0) totalGoals.toFloat() / totalPlayed else 0f

        val formTeams = standings.sortedByDescending {
            it.form?.let { f -> f.count { c -> c == 'W' } } ?: 0
        }
        val inForm = formTeams.take(5).map { s ->
            FormTeamRow(
                teamName = s.team.name,
                form = s.form ?: "UUUUU",
                pointsGained = s.form?.let { f ->
                    f.count { c -> c == 'W' } * 3 + f.count { c -> c == 'D' }
                } ?: 0
            )
        }
        val outOfForm = formTeams.takeLast(5).map { s ->
            FormTeamRow(
                teamName = s.team.name,
                form = s.form ?: "UUUUU",
                pointsGained = s.form?.let { f ->
                    f.count { c -> c == 'W' } * 3 + f.count { c -> c == 'D' }
                } ?: 0
            )
        }

        val bestAttack = standings.maxByOrNull { it.goalsFor }
        val bestDefense = standings.minByOrNull { it.goalsAgainst }

        val homeWins = standings.sumOf { it.homeWon }
        val awayWins = standings.sumOf { it.awayWon }
        val draws = standings.sumOf { it.homeDraw }
        val totalResults = homeWins + awayWins + draws
        val homePct = if (totalResults > 0) homeWins.toFloat() / totalResults else 0f
        val awayPct = if (totalResults > 0) awayWins.toFloat() / totalResults else 0f
        val drawPct = if (totalResults > 0) draws.toFloat() / totalResults else 0f

        val goalBands = listOf(
            GoalBand("1\u201315", 0), GoalBand("16\u201330", 0),
            GoalBand("31\u201345", 0), GoalBand("46\u201360", 0),
            GoalBand("61\u201375", 0), GoalBand("76\u201390+", 0)
        )

        val model = SeasonStatsUiModel(
            totalGoals = totalGoals,
            avgGoalsPerGame = avgGoals,
            mostCommonScoreline = "1\u20130",
            totalRedCards = 0,
            totalYellowCards = 0,
            biggestWin = "",
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
