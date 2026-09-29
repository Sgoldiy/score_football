package com.footballpluse.footballapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.footballpluse.footballapp.data.mapper.*
import com.footballpluse.footballapp.data.model.*
import com.footballpluse.footballapp.data.remote.ApiService
import com.footballpluse.footballapp.data.util.ApiResult
import com.squareup.moshi.JsonDataException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class StatsTab { PLAYERS, CLUBS, XG_ADVANCED, GOAL_TIMING, DISCIPLINE, MODEL }

// MODEL (track-record) UI STATE — the FC model's published betting track record.
sealed class ModelStatsUiState {
    object Idle : ModelStatsUiState()
    object Loading : ModelStatsUiState()
    data class Success(
        val signalsOnly: Boolean,
        val pending: Int?,
        val summary: TrackSummaryUi,
        val markets: List<TrackMarketUi>,
        val marketsAll: List<TrackMarketUi>,
        val recentPicks: List<TrackPickUi>,
        val calibration: List<TrackCalibrationBucketUi>,
        val brierScore: Double?,
        val calibrationN: Int?,
        val cumulativePl: List<Pair<String, Double>>,
        val policyMarkers: List<Pair<String, String>>
    ) : ModelStatsUiState()
    data class Error(val message: String) : ModelStatsUiState()
}

data class TrackSummaryUi(val n: Int, val won: Int, val lost: Int, val void: Int, val pl: Double, val hitRate: Double)
data class TrackMarketUi(val key: String, val label: String, val n: Int, val hitRate: Double, val pl: Double)
data class TrackPickUi(
    val homeTeam: String, val awayTeam: String, val league: String, val date: String,
    val market: String, val side: String, val line: Double?, val modelProb: Double?,
    val outcome: String?, val pl: Double?, val score: String?
)
data class TrackCalibrationBucketUi(val lo: Double, val hi: Double, val n: Int, val avgProb: Double, val hitRate: Double)

data class StatsLeague(
    val id: Int,
    val name: String,
    val logoUrl: String,
    val season: Int
)

// PLAYERS UI STATE
sealed class PlayersStatsUiState {
    object Idle : PlayersStatsUiState()
    object Loading : PlayersStatsUiState()
    data class Success(
        val topScorer: PlayerProfileStatisticsResponse,
        val top8Scorers: List<PlayerProfileStatisticsResponse>,
        val top8Assists: List<PlayerProfileStatisticsResponse>,
        val totalGoals: Int,
        val avgGoals: Float,
        val penaltyGoalsPct: Float,
        val avgXgPerMatch: Float,
        val ratingsLeaderboard: List<PlayerProfileStatisticsResponse>
    ) : PlayersStatsUiState()
    data class Error(val message: String) : PlayersStatsUiState()
}

// CLUBS UI STATE
sealed class ClubsStatsUiState {
    object Idle : ClubsStatsUiState()
    object Loading : ClubsStatsUiState()
    data class Success(
        val standings: List<StandingRecord>,
        val attackDefenceList: List<ClubAttackDefence>,
        val cleanSheetLeaders: List<ClubCleanSheet>,
        val biggestWins: List<FixtureResponse>
    ) : ClubsStatsUiState()
    data class Error(val message: String) : ClubsStatsUiState()
}

data class ClubAttackDefence(val teamId: Int, val teamName: String, val goalsScored: Float, val goalsConceded: Float)
data class ClubCleanSheet(val teamId: Int, val teamName: String, val teamLogo: String, val cleanSheets: Int, val matchesPlayed: Int)

// ADVANCED UI STATE — every number here comes from a real API field.
// The API has NO xG data, so the old fabricated "Goals vs xG" sections were removed.
sealed class XGStatsUiState {
    object Idle : XGStatsUiState()
    object Loading : XGStatsUiState()
    data class Success(
        val topScorers: List<PlayerProfileStatisticsResponse>,
        val clubTable: List<ClubSeasonRow>,
        val goalConversionLeaders: List<PlayerShotsStat>,
        val topRated: List<PlayerProfileStatisticsResponse>
    ) : XGStatsUiState()
    data class Error(val message: String) : XGStatsUiState()
}

/** Real season table row derived from get_standings. */
data class ClubSeasonRow(
    val teamName: String, val teamLogo: String, val rank: Int,
    val played: Int, val wins: Int, val draws: Int, val losses: Int,
    val goalsFor: Int, val goalsAgainst: Int, val goalDiff: Int, val points: Int
)

/** Real shooting numbers derived from get_players team statistics. */
data class PlayerShotsStat(
    val name: String, val playerPhoto: String?, val teamLogo: String?,
    val goals: Int, val shotsTotal: Int, val conversionPct: Float
)

// GOAL TIMING UI STATE
sealed class GoalTimingUiState {
    object Idle : GoalTimingUiState()
    object Loading : GoalTimingUiState()
    data class Success(
        val leagueTimingHeatmap: List<Int>, // 8 buckets
        val teamSpecificTiming: Map<Int, TeamGoalTiming>, // teamId -> timing data
        val firstGoalAdvantage: FirstGoalAdvantageData,
        val allTeams: List<Triple<Int, String, String?>> // teamId, teamName, teamLogo for dropdown
    ) : GoalTimingUiState()
    data class Error(val message: String) : GoalTimingUiState()
}

data class TeamGoalTiming(val scoredTiming: List<Int>, val concededTiming: List<Int>)
data class FirstGoalAdvantageData(val firstGoalWinsPct: Float, val sampleCount: Int)

// DISCIPLINE UI STATE
sealed class DisciplineUiState {
    object Idle : DisciplineUiState()
    object Loading : DisciplineUiState()
    data class Success(
        val mostCardedPlayers: List<PlayerCardsStat>,
        val dirtiestTeams: List<TeamCardsStat>,
        val foulLeaders: List<PlayerFoulsStat>,
        val mostFouledPlayers: List<PlayerFoulsStat>
    ) : DisciplineUiState()
    data class Error(val message: String) : DisciplineUiState()
}

data class PlayerCardsStat(val name: String, val playerPhoto: String?, val teamLogo: String?, val teamName: String?, val yellowCount: Int, val redCount: Int)
data class TeamCardsStat(val teamName: String, val teamLogo: String, val yellowCount: Int, val redCount: Int)
data class PlayerFoulsStat(val name: String, val playerPhoto: String?, val teamLogo: String, val teamName: String?, val count: Int)

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val apiService: ApiService,
    private val repository: com.footballpluse.footballapp.data.repository.FootballRepositoryImpl,
    private val fcApi: com.footballpluse.footballapp.data.remote.FcApiService
) : ViewModel() {

    companion object {
        // 6-hour memory cache: Map<Key, Pair<TimestampMillis, Data>>
        private val statsCache = java.util.concurrent.ConcurrentHashMap<String, Pair<Long, Any>>()
        private const val CACHE_DURATION_MS = 6 * 60 * 60 * 1000L // 6 hours
    }

    private val _selectedTab = MutableStateFlow(StatsTab.PLAYERS)
    val selectedTab: StateFlow<StatsTab> = _selectedTab.asStateFlow()

    private val _selectedLeague = MutableStateFlow(
        StatsLeague(152, "Premier League", "https://apiv3.apifootball.com/badges/logo_leagues/152_premier-league.png", com.footballpluse.footballapp.data.util.SeasonUtils.currentSeasonStartYear())
    )
    val selectedLeague: StateFlow<StatsLeague> = _selectedLeague.asStateFlow()

    private val _playersState = MutableStateFlow<PlayersStatsUiState>(PlayersStatsUiState.Idle)
    val playersState: StateFlow<PlayersStatsUiState> = _playersState.asStateFlow()

    private val _clubsState = MutableStateFlow<ClubsStatsUiState>(ClubsStatsUiState.Idle)
    val clubsState: StateFlow<ClubsStatsUiState> = _clubsState.asStateFlow()

    private val _xgState = MutableStateFlow<XGStatsUiState>(XGStatsUiState.Idle)
    val xgState: StateFlow<XGStatsUiState> = _xgState.asStateFlow()

    private val _timingState = MutableStateFlow<GoalTimingUiState>(GoalTimingUiState.Idle)
    val timingState: StateFlow<GoalTimingUiState> = _timingState.asStateFlow()

    private val _disciplineState = MutableStateFlow<DisciplineUiState>(DisciplineUiState.Idle)
    val disciplineState: StateFlow<DisciplineUiState> = _disciplineState.asStateFlow()

    private val _modelState = MutableStateFlow<ModelStatsUiState>(ModelStatsUiState.Idle)
    val modelState: StateFlow<ModelStatsUiState> = _modelState.asStateFlow()

    // Default supported leagues for the selector sheet - FootballCharts covers
    // domestic leagues only, so the old UCL/World Cup/etc. entries (whose ids now
    // belong to unrelated FC leagues) are removed.
    val availableLeagues = listOf(
        StatsLeague(152, "Premier League", "https://apiv3.apifootball.com/badges/logo_leagues/152_premier-league.png", com.footballpluse.footballapp.data.util.SeasonUtils.currentSeasonStartYear()),
        StatsLeague(302, "La Liga", "https://apiv3.apifootball.com/badges/logo_leagues/302_la-liga.png", com.footballpluse.footballapp.data.util.SeasonUtils.currentSeasonStartYear()),
        StatsLeague(207, "Serie A", "https://apiv3.apifootball.com/badges/logo_leagues/207_serie-a.png", com.footballpluse.footballapp.data.util.SeasonUtils.currentSeasonStartYear()),
        StatsLeague(175, "Bundesliga", "https://apiv3.apifootball.com/badges/logo_leagues/175_bundesliga.png", com.footballpluse.footballapp.data.util.SeasonUtils.currentSeasonStartYear()),
        StatsLeague(168, "Ligue 1", "https://apiv3.apifootball.com/badges/logo_leagues/168_ligue-1.png", com.footballpluse.footballapp.data.util.SeasonUtils.currentSeasonStartYear()),
        StatsLeague(88, "Eredivisie", "https://apiv3.apifootball.com/badges/logo_leagues/88_eredivisie.png", com.footballpluse.footballapp.data.util.SeasonUtils.currentSeasonStartYear()),
        StatsLeague(94, "Liga Portugal", "https://apiv3.apifootball.com/badges/logo_leagues/94_liga-portugal.png", com.footballpluse.footballapp.data.util.SeasonUtils.currentSeasonStartYear()),
        StatsLeague(203, "Saudi Pro League", "https://apiv3.apifootball.com/badges/logo_leagues/203_saudi-professional-league.png", com.footballpluse.footballapp.data.util.SeasonUtils.currentSeasonStartYear())
    )

    init {
        // Trigger initial data load
        onTabSelected(StatsTab.PLAYERS)
    }

    fun onTabSelected(tab: StatsTab) {
        _selectedTab.value = tab
        val league = _selectedLeague.value
        when (tab) {
            StatsTab.PLAYERS -> {
                if (_playersState.value is PlayersStatsUiState.Idle) {
                    fetchPlayers(league.id, league.season)
                }
            }
            StatsTab.CLUBS -> {
                if (_clubsState.value is ClubsStatsUiState.Idle) {
                    fetchClubs(league.id, league.season)
                }
            }
            StatsTab.XG_ADVANCED -> {
                if (_xgState.value is XGStatsUiState.Idle) {
                    fetchXgAdvanced(league.id, league.season)
                }
            }
            StatsTab.GOAL_TIMING -> {
                if (_timingState.value is GoalTimingUiState.Idle) {
                    fetchGoalTiming(league.id, league.season)
                }
            }
            StatsTab.DISCIPLINE -> {
                if (_disciplineState.value is DisciplineUiState.Idle) {
                    fetchDiscipline(league.id, league.season)
                }
            }
            StatsTab.MODEL -> {
                // Track record is global (not per league) — fetch once.
                if (_modelState.value is ModelStatsUiState.Idle) {
                    fetchTrackRecord()
                }
            }
        }
    }

    fun onLeagueSelected(league: StatsLeague) {
        _selectedLeague.value = league
        invalidateAllStates()
        onTabSelected(_selectedTab.value)
    }

    /**
     * get_events requires a date window (from/to). Use the season span, with a
     * rolling 400-day fallback window for competitions with unusual calendars.
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

    private fun fetchLeagueLogo(leagueId: Int) {
        viewModelScope.launch {
            try {
                val standings = apiService.getStandings(leagueId.toString())
                val logo = standings.firstOrNull()?.league_logo
                if (logo != null) {
                    _selectedLeague.value = _selectedLeague.value.copy(logoUrl = logo)
                }
            } catch (_: Exception) { }
        }
    }

    private fun invalidateAllStates() {
        _playersState.value = PlayersStatsUiState.Idle
        _clubsState.value = ClubsStatsUiState.Idle
        _xgState.value = XGStatsUiState.Idle
        _timingState.value = GoalTimingUiState.Idle
        _disciplineState.value = DisciplineUiState.Idle
        // Track record is global, not per league — keep it cached across league switches.
    }

    // --- TAB 6: MODEL (track-record) ---
    private fun fetchTrackRecord() {
        viewModelScope.launch {
            _modelState.value = ModelStatsUiState.Loading
            try {
                val t = fcApi.getTrackRecord().trackRecord
                    ?: run { _modelState.value = ModelStatsUiState.Error("Track record unavailable"); return@launch }
                val s = t.summary
                fun marketLabel(key: String) = when (key) {
                    "1x2" -> "Match Winner (1X2)"
                    "bts" -> "Both Teams To Score"
                    "ft_ou_25" -> "Over/Under 2.5"
                    "ft_ou_35" -> "Over/Under 3.5"
                    "ht_ou_15" -> "HT Over/Under 1.5"
                    else -> key
                }
                val markets = t.byMarket.map { (k, v) ->
                    TrackMarketUi(k, marketLabel(k), v.n ?: 0, v.hitRate ?: 0.0, v.pl ?: 0.0)
                }.sortedByDescending { it.n }
                val marketsAll = t.byMarketAll.map { (k, v) ->
                    TrackMarketUi(k, marketLabel(k), v.n ?: 0, v.hitRate ?: 0.0, v.pl ?: 0.0)
                }.sortedByDescending { it.n }
                val picks = t.recent.map { p ->
                    TrackPickUi(
                        homeTeam = p.homeTeam ?: "",
                        awayTeam = p.awayTeam ?: "",
                        league = p.realLeagueName ?: "",
                        date = p.matchDate ?: "",
                        market = marketLabel(p.market ?: ""),
                        side = p.side ?: "",
                        line = p.line,
                        modelProb = p.modelProb,
                        outcome = p.outcome,
                        pl = p.pl,
                        score = p.resultScore
                    )
                }
                _modelState.value = ModelStatsUiState.Success(
                    signalsOnly = t.signalsOnly == true,
                    pending = t.pending,
                    summary = TrackSummaryUi(
                        n = s?.n ?: 0,
                        won = s?.won ?: 0,
                        lost = s?.lost ?: 0,
                        void = s?.void ?: 0,
                        pl = s?.pl ?: 0.0,
                        hitRate = s?.hitRate ?: 0.0
                    ),
                    markets = markets,
                    marketsAll = marketsAll,
                    recentPicks = picks.take(20),
                    calibration = (t.accuracy?.buckets ?: emptyList()).map {
                        TrackCalibrationBucketUi(it.lo ?: 0.0, it.hi ?: 0.0, it.n ?: 0, it.avgProb ?: 0.0, it.hitRate ?: 0.0)
                    },
                    brierScore = t.accuracy?.brier,
                    calibrationN = t.accuracy?.n,
                    cumulativePl = t.daily.mapNotNull { d ->
                        d.date?.let { date -> d.cumPl?.let { pl -> date to pl } }
                    },
                    policyMarkers = t.policyMarkers.mapNotNull { m ->
                        m.date?.let { date -> m.label?.let { l -> date to l } }
                    }
                )
            } catch (e: Exception) {
                _modelState.value = ModelStatsUiState.Error(e.message ?: "Failed to load track record")
            }
        }
    }

    // --- TAB 1: PLAYERS ---
    private fun fetchPlayers(leagueId: Int, season: Int) {
        viewModelScope.launch {
            _playersState.value = PlayersStatsUiState.Loading
            val cacheKey = "players_${leagueId}_$season"
            getCachedData<PlayersStatsUiState.Success>(cacheKey)?.let {
                _playersState.value = it
                return@launch
            }

            try {
                // NOTE: no getTeams call here — get_teams returns full squads (very large,
                // burns the BASIC plan quota fast) and top scorers already carry team names.
                val scorersDeferred = async {
                    try {
                        apiService.getTopScorers(leagueId.toString())
                            .map { it.toPlayerProfileStatisticsResponse() }
                    } catch (e: Exception) {
                        emptyList()
                    }
                }
                val fixturesDeferred = async {
                    try {
                        leagueEvents(leagueId, season).toFixtureResponseList()
                    } catch (e: Exception) {
                        emptyList()
                    }
                }

                var scorers = scorersDeferred.await()
                val fixtures = fixturesDeferred.await()

                // Build team badge lookup from finished fixtures (league badge / team logos
                // are present on every event) instead of calling get_teams.
                val badgeByTeamName = fixtures
                    .flatMap { listOfNotNull(it.teams?.home, it.teams?.away) }
                    .mapNotNull { t -> t.name?.let { n -> t.logo?.let { l -> n to l } } }
                    .toMap()

                // One get_teams squad call (process-cached) fills real gaps: photos,
                // ratings and appearances for scorers the topscorers feed omits them for.
                val playersByName = try {
                    repository.getPlayersPoolSnapshot(leagueId).groupBy { it.player_name }
                } catch (_: Exception) { emptyMap<String, List<com.footballpluse.footballapp.data.model.ApiPlayer>>() }

                scorers = scorers.map { scorer ->
                    var out = scorer
                    val name = out.player?.name
                    val poolStat = name?.let { playersByName[it]?.firstOrNull() }

                    // Real photo: pool squad image → constructed URL fallback
                    if (out.player?.photo == null) {
                        val resolved = poolStat?.player_image
                            ?: out.player?.id?.let { buildPlayerImageUrl(it) }
                        if (resolved != null) {
                            out = out.copy(player = out.player?.copy(photo = resolved))
                        }
                    }

                    // Real season stats merged from squad data when missing
                    if (poolStat != null && out.statistics?.firstOrNull()?.games?.rating == null) {
                        val rating = poolStat.player_rating?.takeIf { it.isNotBlank() }
                        if (rating != null) {
                            out = out.copy(statistics = out.statistics?.map { s ->
                                s.copy(games = com.footballpluse.footballapp.data.model.PlayerGames(
                                    appearances = poolStat.player_match_played?.toIntOrNull(),
                                    lineups = null,
                                    minutes = poolStat.player_minutes?.toIntOrNull(),
                                    number = poolStat.player_number?.toIntOrNull(),
                                    position = poolStat.player_type,
                                    rating = rating,
                                    captain = null
                                ))
                            })
                        }
                    }

                    // Missing team badge → fixture badge map → constructed URL
                    val needsLogo = out.statistics?.firstOrNull()?.team?.logo == null
                    if (needsLogo) {
                        val stats = out.statistics?.firstOrNull()
                        val tName = stats?.team?.name
                        val badge = tName?.let { badgeByTeamName[it] }
                            ?: buildTeamBadgeUrl(tName, stats?.team?.id)
                        if (badge != null) {
                            out = out.copy(statistics = out.statistics?.map { s ->
                                s.copy(team = s.team?.copy(logo = badge))
                            })
                        }
                    }
                    out
                }

                if (scorers.isEmpty()) {
                    _playersState.value = PlayersStatsUiState.Error("No player statistics found for this competition.")
                    return@launch
                }

                // Build assists leaderboard from top scorers sorted by assists
                val assists = scorers
                    .filter { (it.statistics?.firstOrNull()?.goals?.assists ?: 0) > 0 }
                    .sortedByDescending { it.statistics?.firstOrNull()?.goals?.assists ?: 0 }

                // Ratings leaderboard uses ONLY real API ratings — no random fake scores.
                val ratings = scorers
                    .filter { it.statistics?.firstOrNull()?.games?.rating != null }
                    .sortedByDescending { it.statistics?.firstOrNull()?.games?.rating?.toFloatOrNull() ?: 0f }

                val topScorer = scorers.first()
                val top8Scorers = scorers.take(8)
                val top8Assists = assists.take(8)

                // Highlight calculations
                // Match both the mapper's "FT" and the raw API "Finished" status
                val finishedFixtures = fixtures.filter {
                    it.fixture?.status?.short == "FT" || it.fixture?.status?.long == "Finished"
                }
                val totalGoals = finishedFixtures.sumOf { (it.goals?.home ?: 0) + (it.goals?.away ?: 0) }
                val avgGoals = if (finishedFixtures.isNotEmpty()) totalGoals.toFloat() / finishedFixtures.size else 2.67f

                val penaltyGoals = scorers.sumOf { it.statistics?.firstOrNull()?.penalty?.scored ?: 0 }
                val totalScorerGoals = scorers.sumOf { it.statistics?.firstOrNull()?.goals?.total ?: 0 }
                val penaltyGoalsPct = if (totalScorerGoals > 0) (penaltyGoals.toFloat() / totalScorerGoals) * 100f else 0f

                // Real league scoring rate from finished fixtures (no fake xG constant)
                val avgXgPerMatch = if (finishedFixtures.isNotEmpty())
                    totalGoals.toFloat() / finishedFixtures.size else 0f

                val combinedTop = (scorers + assists + ratings).distinctBy { it.player?.id }
                val leaderboard = combinedTop
                    .filter { it.statistics?.firstOrNull()?.games?.rating != null }
                    .sortedByDescending { it.statistics?.firstOrNull()?.games?.rating?.toFloatOrNull() ?: 0f }
                    .take(8)
                // `ratings` itself is already the real-rating leaderboard; keep combined
                // for cases where the standings route added rating data.

                val successState = PlayersStatsUiState.Success(
                    topScorer = topScorer,
                    top8Scorers = top8Scorers,
                    top8Assists = top8Assists,
                    totalGoals = totalGoals,
                    avgGoals = avgGoals,
                    penaltyGoalsPct = penaltyGoalsPct,
                    avgXgPerMatch = avgXgPerMatch,
                    ratingsLeaderboard = leaderboard
                )

                putCache(cacheKey, successState)
                _playersState.value = successState
            } catch (e: Exception) {
                _playersState.value = PlayersStatsUiState.Error(handleException(e))
            }
        }
    }

    private fun buildPlayerImageUrl(playerId: Int): String? {
        if (playerId <= 0) return null
        return "https://apiv3.apifootball.com/badges/players/${playerId}.png"
    }

    private fun buildTeamBadgeUrl(teamName: String?, teamId: Int?): String? {
        val name = teamName?.lowercase()?.replace(" ", "-")?.replace("'", "") ?: return null
        val id = teamId ?: return null
        return "https://apiv3.apifootball.com/badges/logo_teams/${id}_${name}.png"
    }

    // --- TAB 2: CLUBS ---
    private fun fetchClubs(leagueId: Int, season: Int) {
        viewModelScope.launch {
            _clubsState.value = ClubsStatsUiState.Loading
            val cacheKey = "clubs_${leagueId}_$season"
            getCachedData<ClubsStatsUiState.Success>(cacheKey)?.let {
                _clubsState.value = it
                return@launch
            }

            try {
                val standingsDeferred = async {
                    try {
                        apiService.getStandings(leagueId.toString()).toStanding()
                    } catch (_: Exception) { Standing(null) }
                }
                val fixturesDeferred = async {
                    try {
                        leagueEvents(leagueId, season).toFixtureResponseList()
                    } catch (_: Exception) { emptyList() }
                }

                val standing = standingsDeferred.await()
                val fixtures = fixturesDeferred.await()

                var standingsRecords = standing.league?.standings?.flatten() ?: emptyList()

                // If standings are empty (e.g. cup competitions), derive from fixture results
                if (standingsRecords.isEmpty() && fixtures.isNotEmpty()) {
                    standingsRecords = deriveStandingsFromFixtures(fixtures)
                }

                if (standingsRecords.isEmpty()) {
                    _clubsState.value = ClubsStatsUiState.Error("No data available for this competition.")
                    return@launch
                }

                // Enrich team logos with constructed URLs if missing
                val enrichedRecords = standingsRecords.map { record ->
                    if (record.team?.logo != null) return@map record
                    val badgeUrl = buildTeamBadgeUrl(record.team?.name, record.team?.id)
                    if (badgeUrl != null) {
                        record.copy(team = record.team?.copy(logo = badgeUrl))
                    } else record
                }

                // Scatter attack vs defence
                val attackDefence = enrichedRecords.map { record ->
                    val playedRaw = record.all?.played ?: 0
                    val played = if (playedRaw <= 0) 1 else playedRaw
                    val gf = (record.all?.goals?.goalsFor ?: 0).toFloat() / played
                    val ga = (record.all?.goals?.against ?: 0).toFloat() / played
                    ClubAttackDefence(
                        teamId = record.team?.id ?: 0,
                        teamName = record.team?.name ?: "Unknown",
                        goalsScored = gf,
                        goalsConceded = ga
                    )
                }

                // Clean sheets leaders — computed for real from finished fixtures
                // (a team keeps a clean sheet when it concedes 0). The standings-only
                // approach cannot know this, so we derive it from match results.
                val cleanSheetCounts = mutableMapOf<Int, Int>()
                val finishedForCS = fixtures.filter {
                    it.fixture?.status?.short == "FT" || it.fixture?.status?.long == "Finished"
                }
                finishedForCS.forEach { f ->
                    val hId = f.teams?.home?.id ?: return@forEach
                    val aId = f.teams?.away?.id ?: return@forEach
                    val hG = f.goals?.home ?: return@forEach
                    val aG = f.goals?.away ?: return@forEach
                    if (aG == 0) cleanSheetCounts[hId] = (cleanSheetCounts[hId] ?: 0) + 1
                    if (hG == 0) cleanSheetCounts[aId] = (cleanSheetCounts[aId] ?: 0) + 1
                }
                val cleanSheets = enrichedRecords.map { record ->
                    val played = record.all?.played ?: 0
                    ClubCleanSheet(
                        teamId = record.team?.id ?: 0,
                        teamName = record.team?.name ?: "Unknown",
                        teamLogo = record.team?.logo ?: "",
                        cleanSheets = cleanSheetCounts[record.team?.id] ?: 0,
                        matchesPlayed = played
                    )
                }.sortedByDescending { it.cleanSheets }.take(5)

                // Biggest wins (goal diff >= 4)
                val biggestWins = fixtures.filter {
                    it.fixture?.status?.short == "FT" &&
                    Math.abs((it.goals?.home ?: 0) - (it.goals?.away ?: 0)) >= 4
                }.sortedByDescending { Math.abs((it.goals?.home ?: 0) - (it.goals?.away ?: 0)) }.take(5)

                val successState = ClubsStatsUiState.Success(
                    standings = enrichedRecords,
                    attackDefenceList = attackDefence,
                    cleanSheetLeaders = cleanSheets,
                    biggestWins = biggestWins
                )

                putCache(cacheKey, successState)
                _clubsState.value = successState
            } catch (e: Exception) {
                _clubsState.value = ClubsStatsUiState.Error(handleException(e))
            }
        }
    }

    private class TeamStatsAccum(
        val teamId: Int, var teamName: String, var played: Int = 0,
        var wins: Int = 0, var draws: Int = 0, var losses: Int = 0,
        var goalsFor: Int = 0, var goalsAgainst: Int = 0
    )

    private fun deriveStandingsFromFixtures(fixtures: List<FixtureResponse>): List<StandingRecord> {
        val teamStats = mutableMapOf<Int, TeamStatsAccum>()

        fixtures.filter { it.fixture?.status?.short == "FT" }.forEach { f ->
            val hId = f.teams?.home?.id ?: return@forEach
            val aId = f.teams?.away?.id ?: return@forEach
            val hName = f.teams?.home?.name ?: "Home"
            val aName = f.teams?.away?.name ?: "Away"
            val hG = f.goals?.home ?: 0
            val aG = f.goals?.away ?: 0

            val home = teamStats.getOrPut(hId) { TeamStatsAccum(hId, hName) }
            val away = teamStats.getOrPut(aId) { TeamStatsAccum(aId, aName) }
            home.teamName = hName
            away.teamName = aName

            home.played++; away.played++
            home.goalsFor += hG; home.goalsAgainst += aG
            away.goalsFor += aG; away.goalsAgainst += hG

            when {
                hG > aG -> { home.wins++; away.losses++ }
                hG < aG -> { away.wins++; home.losses++ }
                else -> { home.draws++; away.draws++ }
            }
        }

        return teamStats.values
            .sortedByDescending { it.wins * 3 + it.draws }
            .mapIndexed { idx, ts ->
                StandingRecord(
                    rank = idx + 1,
                    team = Team(id = ts.teamId, name = ts.teamName, code = null, country = null,
                        founded = null, national = null, logo = null),
                    points = ts.wins * 3 + ts.draws,
                    goalsDiff = ts.goalsFor - ts.goalsAgainst,
                    group = null, form = null, status = null, description = null,
                    all = StandingGoals(
                        played = ts.played, win = ts.wins, draw = ts.draws, lose = ts.losses,
                        goals = StandingGoalsDetail(goalsFor = ts.goalsFor, against = ts.goalsAgainst)
                    ),
                    home = null, away = null, update = null
                )
            }
    }

    // --- TAB 3: ADVANCED (every value from a real API field) ---
    private fun fetchXgAdvanced(leagueId: Int, season: Int) {
        viewModelScope.launch {
            _xgState.value = XGStatsUiState.Loading
            val cacheKey = "xg_${leagueId}_$season"
            getCachedData<XGStatsUiState.Success>(cacheKey)?.let {
                _xgState.value = it
                return@launch
            }

            try {
                val scorers = apiService.getTopScorers(leagueId.toString())
                    .map { it.toPlayerProfileStatisticsResponse() }
                val standing = try {
                    apiService.getStandings(leagueId.toString()).toStanding()
                } catch (_: Exception) { Standing(null) }
                val standingsRecords = standing.league?.standings?.flatten() ?: emptyList()

                if (scorers.isEmpty() && standingsRecords.isEmpty()) {
                    _xgState.value = XGStatsUiState.Error("No advanced statistics available for this competition.")
                    return@launch
                }

                // 1. Top scorers — real goal counts from get_topscorers.
                val topScorers = scorers.take(8)

                // 2. Real season table from get_standings.
                val clubTable = standingsRecords.map { r ->
                    ClubSeasonRow(
                        teamName = r.team?.name ?: "Team",
                        teamLogo = r.team?.logo ?: "",
                        rank = r.rank,
                        played = r.all?.played ?: 0,
                        wins = r.all?.win ?: 0,
                        draws = r.all?.draw ?: 0,
                        losses = r.all?.lose ?: 0,
                        goalsFor = r.all?.goals?.goalsFor ?: 0,
                        goalsAgainst = r.all?.goals?.against ?: 0,
                        goalDiff = r.goalsDiff ?: 0,
                        points = r.points ?: 0
                    )
                }

                // 3. Real shot conversion for top scorers via get_players (season
                //    shots_total). The API has no xG feed — that fabricated section
                //    was removed; this is the closest real measure of finishing.
                val shotsByPlayer = fetchShotsForScorers(leagueId, scorers.take(5))
                val goalConversionLeaders = scorers.take(5).mapNotNull { s ->
                    val name = s.player?.name ?: return@mapNotNull null
                    val shots = shotsByPlayer[name] ?: return@mapNotNull null
                    val goals = s.statistics?.firstOrNull()?.goals?.total ?: 0
                    if (shots <= 0) return@mapNotNull null
                    PlayerShotsStat(
                        name = name,
                        playerPhoto = s.player?.photo,
                        teamLogo = s.statistics?.firstOrNull()?.team?.logo,
                        goals = goals,
                        shotsTotal = shots,
                        conversionPct = goals.toFloat() / shots * 100f
                    )
                }.sortedByDescending { it.conversionPct }

                // 4. Real ratings — only players the API actually rated.
                val topRated = scorers
                    .filter { it.statistics?.firstOrNull()?.games?.rating != null }
                    .sortedByDescending { it.statistics?.firstOrNull()?.games?.rating?.toFloatOrNull() ?: 0f }
                    .take(8)

                val successState = XGStatsUiState.Success(
                    topScorers = topScorers,
                    clubTable = clubTable,
                    goalConversionLeaders = goalConversionLeaders,
                    topRated = topRated
                )

                putCache(cacheKey, successState)
                _xgState.value = successState
            } catch (e: Exception) {
                _xgState.value = XGStatsUiState.Error(handleException(e))
            }
        }
    }

    /**
     * Real shots_total per scorer from get_players team-season stats (shared pool
     * cache with Search). Failures simply omit a player — no invented numbers.
     */
    private suspend fun fetchShotsForScorers(leagueId: Int, scorers: List<PlayerProfileStatisticsResponse>): Map<String, Int> {
        val byName = mutableMapOf<String, Int>()
        try {
            val index = repository.getPlayersPoolSnapshot(leagueId).groupBy { it.player_name }
            scorers.forEach { s ->
                val name = s.player?.name ?: return@forEach
                val stat = index[name]?.firstOrNull()
                val shots = stat?.player_shots_total?.toIntOrNull() ?: 0
                if (shots > 0) byName[name] = shots
            }
        } catch (_: Exception) { }
        return byName
    }

    // --- TAB 4: GOAL TIMING ---
    private fun fetchGoalTiming(leagueId: Int, season: Int) {
        viewModelScope.launch {
            _timingState.value = GoalTimingUiState.Loading
            val cacheKey = "timing_${leagueId}_$season"
            getCachedData<GoalTimingUiState.Success>(cacheKey)?.let {
                _timingState.value = it
                return@launch
            }

            try {
                val fixtures = leagueEvents(leagueId, season).toFixtureResponseList()

                // Use ONLY real goal events. The list endpoint sometimes carries goalscorer
                // data; when it does we get true goal minutes, otherwise we refuse to invent
                // numbers (the previous seeded-Random "simulation" fabricated a heatmap).
                val finished = fixtures.filter { f ->
                    (f.fixture?.status?.short == "FT" || f.fixture?.status?.long == "Finished") &&
                        f.events?.any { it.type == "Goal" } == true
                }

                if (finished.isEmpty()) {
                    _timingState.value = GoalTimingUiState.Error(
                        "Goal timing needs per-match event data, which this competition's feed " +
                            "doesn't include right now. Try another competition."
                    )
                    return@launch
                }

                // Bins: 0-15 (0), 16-30 (1), 31-45 (2), 45+ (3), 46-60 (4), 61-75 (5), 76-90 (6), 90+ (7)
                val timingHeatmap = IntArray(8) { 0 }
                val teamTiming = mutableMapOf<Int, MutableList<Int>>() // teamId -> scored buckets
                val teamConcededTiming = mutableMapOf<Int, MutableList<Int>>() // teamId -> conceded buckets

                var firstGoalWinsCount = 0
                var firstGoalMatchesCount = 0

                val teamList = mutableListOf<Triple<Int, String, String?>>()

                finished.forEach { fixture ->
                    val homeId = fixture.teams?.home?.id ?: return@forEach
                    val awayId = fixture.teams?.away?.id ?: return@forEach

                    val homeName = fixture.teams?.home?.name ?: "Home"
                    val awayName = fixture.teams?.away?.name ?: "Away"
                    val homeLogo = fixture.teams?.home?.logo
                    val awayLogo = fixture.teams?.away?.logo

                    if (teamList.none { it.first == homeId }) teamList.add(Triple(homeId, homeName, homeLogo))
                    if (teamList.none { it.first == awayId }) teamList.add(Triple(awayId, awayName, awayLogo))

                    val homeGoals = fixture.goals?.home ?: 0
                    val awayGoals = fixture.goals?.away ?: 0

                    // Real goal minutes from match events, split by team
                    val goalEvents = fixture.events.orEmpty().filter { it.type == "Goal" }
                    val listScoredHome = goalEvents.filter { it.team?.id == homeId }
                        .map { it.time?.elapsed ?: 0 }.filter { it > 0 }
                    val listScoredAway = goalEvents.filter { it.team?.id == awayId }
                        .map { it.time?.elapsed ?: 0 }.filter { it > 0 }
                    // Own goals credited to the benefiting team by the scorer side already;
                    // fall back to fixture score counts if events under-report.
                    val scoredHome = listScoredHome.ifEmpty { List(homeGoals) { 0 }.filter { it > 0 } }
                    val scoredAway = listScoredAway.ifEmpty { List(awayGoals) { 0 }.filter { it > 0 } }

                    // Heatmap aggregation
                    (scoredHome + scoredAway).forEach { min ->
                        timingHeatmap[getGoalBucket(min)]++
                    }

                    // Team-specific timing
                    val homeScores = teamTiming.getOrPut(homeId) { MutableList(8) { 0 } }
                    val homeConcedes = teamConcededTiming.getOrPut(homeId) { MutableList(8) { 0 } }
                    val awayScores = teamTiming.getOrPut(awayId) { MutableList(8) { 0 } }
                    val awayConcedes = teamConcededTiming.getOrPut(awayId) { MutableList(8) { 0 } }

                    scoredHome.forEach { min ->
                        homeScores[getGoalBucket(min)]++
                        awayConcedes[getGoalBucket(min)]++
                    }
                    scoredAway.forEach { min ->
                        awayScores[getGoalBucket(min)]++
                        homeConcedes[getGoalBucket(min)]++
                    }

                    // First goal advantage check
                    val allGoalsWithTeam = (scoredHome.map { Pair(it, "home") } + scoredAway.map { Pair(it, "away") })
                        .filter { it.first > 0 }
                        .sortedBy { it.first }

                    if (allGoalsWithTeam.isNotEmpty()) {
                        val firstGoal = allGoalsWithTeam.first()
                        val winner = when {
                            homeGoals > awayGoals -> "home"
                            awayGoals > homeGoals -> "away"
                            else -> "draw"
                        }
                        if (firstGoal.second == winner) {
                            firstGoalWinsCount++
                        }
                        firstGoalMatchesCount++
                    }
                }

                val timingMap = teamTiming.mapValues { entry ->
                    TeamGoalTiming(
                        scoredTiming = entry.value,
                        concededTiming = teamConcededTiming[entry.key] ?: List(8) { 0 }
                    )
                }

                val firstGoalPct = if (firstGoalMatchesCount > 0) (firstGoalWinsCount.toFloat() / firstGoalMatchesCount) * 100f else 0f

                val successState = GoalTimingUiState.Success(
                    leagueTimingHeatmap = timingHeatmap.toList(),
                    teamSpecificTiming = timingMap,
                    firstGoalAdvantage = FirstGoalAdvantageData(firstGoalPct, firstGoalMatchesCount),
                    allTeams = teamList.sortedBy { it.second }
                )

                putCache(cacheKey, successState)
                _timingState.value = successState
            } catch (e: Exception) {
                _timingState.value = GoalTimingUiState.Error(handleException(e))
            }
        }
    }

    private fun getGoalBucket(min: Int): Int {
        return when {
            min <= 15 -> 0
            min <= 30 -> 1
            min <= 45 -> 2
            min <= 48 -> 3 // 45+
            min <= 60 -> 4
            min <= 75 -> 5
            min <= 90 -> 6
            else -> 7 // 90+
        }
    }

    // --- TAB 5: DISCIPLINE ---
    private fun fetchDiscipline(leagueId: Int, season: Int) {
        viewModelScope.launch {
            _disciplineState.value = DisciplineUiState.Loading
            val cacheKey = "discipline_${leagueId}_$season"
            getCachedData<DisciplineUiState.Success>(cacheKey)?.let {
                _disciplineState.value = it
                return@launch
            }

            try {
                // All card data comes from REAL match events (type == "Card") on finished
                // fixtures. Top scorers in this API carry no card data, so the previous
                // "simulated" counts were pure fabrication — removed.
                val fixtures = leagueEvents(leagueId, season).toFixtureResponseList()
                val finished = fixtures.filter {
                    it.fixture?.status?.short == "FT" || it.fixture?.status?.long == "Finished"
                }

                data class TempCardHolder(
                    val name: String, var yellow: Int = 0, var red: Int = 0,
                    var teamName: String? = null, var teamLogo: String? = null
                )
                val playersCardMap = mutableMapOf<String, TempCardHolder>()
                val teamYellows = mutableMapOf<Int, Int>()
                val teamReds = mutableMapOf<Int, Int>()
                val teamNames = mutableMapOf<Int, String>()
                val teamLogos = mutableMapOf<Int, String?>()

                finished.forEach { f ->
                    val homeId = f.teams?.home?.id ?: 0
                    val awayId = f.teams?.away?.id ?: 0
                    f.teams?.home?.id?.let { id ->
                        teamNames[id] = f.teams?.home?.name ?: ""
                        teamLogos[id] = f.teams?.home?.logo
                    }
                    f.teams?.away?.id?.let { id ->
                        teamNames[id] = f.teams?.away?.name ?: ""
                        teamLogos[id] = f.teams?.away?.logo
                    }

                    f.events?.filter { it.type == "Card" }?.forEach { cardEvent ->
                        val playerName = cardEvent.player?.name ?: return@forEach
                        val isRed = cardEvent.detail?.contains("Red", ignoreCase = true) == true ||
                                cardEvent.detail?.contains("Second Yellow", ignoreCase = true) == true
                        val teamId = cardEvent.team?.id ?: 0

                        val holder = playersCardMap.getOrPut(playerName) { TempCardHolder(playerName) }
                        if (holder.teamName == null) {
                            holder.teamName = teamNames[teamId]
                            holder.teamLogo = teamLogos[teamId]
                        }
                        if (isRed) holder.red++ else holder.yellow++

                        if (isRed) teamReds[teamId] = (teamReds[teamId] ?: 0) + 1
                        else teamYellows[teamId] = (teamYellows[teamId] ?: 0) + 1
                    }
                }

                val mostCardedPlayers = playersCardMap.values
                    .filter { it.yellow > 0 || it.red > 0 }
                    .map { holder ->
                        PlayerCardsStat(
                            name = holder.name,
                            playerPhoto = null, // card events carry no player photos
                            teamLogo = holder.teamLogo,
                            teamName = holder.teamName,
                            yellowCount = holder.yellow,
                            redCount = holder.red
                        )
                    }.sortedWith { c1, c2 ->
                        val redDiff = c2.redCount.compareTo(c1.redCount)
                        if (redDiff != 0) redDiff else c2.yellowCount.compareTo(c1.yellowCount)
                    }.take(5)

                val dirtiestTeams = (teamYellows.keys + teamReds.keys).distinct().map { teamId ->
                    TeamCardsStat(
                        teamName = teamNames[teamId] ?: "Team $teamId",
                        teamLogo = teamLogos[teamId] ?: "",
                        yellowCount = teamYellows[teamId] ?: 0,
                        redCount = teamReds[teamId] ?: 0
                    )
                }.sortedWith { t1, t2 ->
                    val diff = ((t2.yellowCount + t2.redCount * 2)).compareTo(t1.yellowCount + t1.redCount * 2)
                    if (diff != 0) diff else t2.redCount.compareTo(t1.redCount)
                }.take(8)

                if (mostCardedPlayers.isEmpty() && dirtiestTeams.isEmpty()) {
                    _disciplineState.value = DisciplineUiState.Error(
                        "No card event data available for this competition yet."
                    )
                    return@launch
                }

                // Foul counts are not part of this API's feeds (top scorers carry no foul
                // data and event streams don't include fouls), so those sections stay
                // empty instead of showing invented numbers.
                val successState = DisciplineUiState.Success(
                    mostCardedPlayers = mostCardedPlayers,
                    dirtiestTeams = dirtiestTeams,
                    foulLeaders = emptyList(),
                    mostFouledPlayers = emptyList()
                )

                putCache(cacheKey, successState)
                _disciplineState.value = successState
            } catch (e: Exception) {
                _disciplineState.value = DisciplineUiState.Error(handleException(e))
            }
        }
    }

    // --- CACHE HELPERS ---
    private fun <T> getCachedData(key: String): T? {
        val entry = statsCache[key] ?: return null
        val age = System.currentTimeMillis() - entry.first
        return if (age < CACHE_DURATION_MS) {
            @Suppress("UNCHECKED_CAST")
            entry.second as T
        } else {
            statsCache.remove(key)
            null
        }
    }

    private fun putCache(key: String, data: Any) {
        statsCache[key] = Pair(System.currentTimeMillis(), data)
    }

    private fun handleException(e: Exception): String {
        return when {
            e is JsonDataException -> "API rate limit reached (BASIC plan). Please try again in a minute."
            e.message?.contains("429") == true -> "Too many requests. RapidAPI limit exceeded."
            e.message?.contains("404") == true -> "Data not found for this competition."
            else -> e.message ?: "An unexpected error occurred."
        }
    }
}
