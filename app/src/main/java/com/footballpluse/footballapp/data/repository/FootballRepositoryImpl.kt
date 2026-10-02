package com.footballpluse.footballapp.data.repository

import com.footballpluse.footballapp.data.local.db.FixtureDao
import com.footballpluse.footballapp.data.local.db.LeagueDao
import com.footballpluse.footballapp.data.local.db.StandingDao
import com.footballpluse.footballapp.data.mapper.*
import com.footballpluse.footballapp.data.model.*
import com.footballpluse.footballapp.data.remote.ApiService
import com.footballpluse.footballapp.data.util.ApiResult
import com.footballpluse.footballapp.domain.model.*
import com.footballpluse.footballapp.domain.repository.FootballRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FootballRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val fixtureDao: FixtureDao,
    private val standingDao: StandingDao,
    private val leagueDao: LeagueDao,
    private val teamRepository: TeamRepository
) : FootballRepository {

    companion object {
        private val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)

        fun today(): String = sdf.format(Date())
        fun daysFromToday(offset: Int): String {
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, offset)
            return sdf.format(cal.time)
        }

        /** Live poll cadence: quick while matches are in play, polite when idle. */
        const val LIVE_POLL_ACTIVE_MS = 25_000L
        const val LIVE_POLL_IDLE_MS = 60_000L

        fun livePollDelayMs(hasLive: Boolean): Long =
            if (hasLive) LIVE_POLL_ACTIVE_MS else LIVE_POLL_IDLE_MS

        /** Leagues scanned to build the searchable team pool. */
        private val SEARCH_POOL_LEAGUES = listOf(152, 302, 207, 175, 168, 88, 94, 203, 144, 187, 188, 169)

        @Volatile
        private var teamPoolCache: List<ApiStanding>? = null
    }

    override fun getFixturesByDate(date: String): Flow<ApiResult<List<Match>>> = flow {
        // First check cache and emit immediately if present
        val cached = fixtureDao.getFixturesByDate(date).first()
        if (cached.isNotEmpty()) {
            emit(ApiResult.Success(cached.map { it.toMatch() }))
        } else {
            emit(ApiResult.Loading)
        }
        try {
            val events = apiService.getEvents(from = date, to = date)
            val fixtures = events.toFixtureResponseList()
            if (fixtures.isNotEmpty()) {
                val entities = fixtures.map { it.toEntity(date) }
                fixtureDao.deleteFixturesByDate(date)
                fixtureDao.insertFixtures(entities)
                emit(ApiResult.Success(entities.map { it.toMatch() }))
            } else {
                emit(ApiResult.Success(emptyList()))
            }
        } catch (e: Exception) {
            val isNoDataError = e.message?.contains("404") == true ||
                               e.message?.contains("No event found") == true ||
                               e is com.squareup.moshi.JsonDataException

            if (isNoDataError) {
                emit(ApiResult.Success(emptyList()))
            } else {
                val errorMessage = when {
                    e.message?.contains("429") == true -> "Rate limit exceeded. Please try again later."
                    e.message?.contains("500") == true -> "Server error. We're working on it!"
                    else -> e.message ?: "Network error"
                }
                if (cached.isEmpty()) {
                    emit(ApiResult.Error(errorMessage))
                }
            }
        }
    }

    override suspend fun getFixtureCountByDate(date: String): Int {
        return fixtureDao.getFixtureCountByDate(date)
    }

    override fun getLiveMatches(): Flow<ApiResult<List<Match>>> = flow {
        var inPlay = false
        while (true) {
            try {
                val events = apiService.getLivescore()
                val liveFixtures = events.toFixtureResponseList()
                val liveMatches = liveFixtures.map { it.toMatch() }
                    .filter { it.isLive || (it.elapsed ?: 0) > 0 && it.homeScore != null }
                inPlay = liveMatches.isNotEmpty()
                emit(ApiResult.Success(liveMatches))
            } catch (e: Exception) {
                val isNoDataError = e.message?.contains("404") == true ||
                                   e.message?.contains("No event found") == true ||
                                   e is com.squareup.moshi.JsonDataException

                if (isNoDataError) {
                    emit(ApiResult.Success(emptyList()))
                } else {
                    emit(ApiResult.Error(e.message ?: "Failed to refresh live matches"))
                }
            }
            delay(livePollDelayMs(inPlay))
        }
    }

    override suspend fun getMatchDetail(fixtureId: Int): ApiResult<MatchDetail> {
        return try {
            val eventsResponse = apiService.getEventById(matchId = fixtureId.toString())
            val apiEvent = eventsResponse.firstOrNull() ?: return ApiResult.Error("Match not found")
            val response = apiEvent.toFixtureResponse()

            val detailedEvents = response.events ?: emptyList()

            var homeForm: String? = null
            var awayForm: String? = null

            try {
                val leagueId = apiEvent.league_id
                if (leagueId != null) {
                    val standings = apiService.getStandings(leagueId = leagueId)
                    homeForm = standings.find { it.team_id == apiEvent.match_hometeam_id }?.overall_form
                    awayForm = standings.find { it.team_id == apiEvent.match_awayteam_id }?.overall_form
                }
            } catch (_: Exception) {}

            val lineupWrapper = try {
                apiService.getLineups(matchId = fixtureId.toString()).values.firstOrNull()?.lineup
            } catch (_: Exception) { null }

            val statsWrapper = try {
                apiService.getMatchStatistics(matchId = fixtureId.toString()).values.firstOrNull()
            } catch (_: Exception) { null }

            val predictions = try {
                apiService.getPredictions(matchId = fixtureId.toString())
            } catch (_: Exception) { emptyList() }

            val odds = try {
                apiService.getOdds(matchId = fixtureId.toString())
            } catch (_: Exception) { emptyList() }

            val homeId = response.teams?.home?.id
            val awayId = response.teams?.away?.id
            val h2h = if (homeId != null && awayId != null && homeId != 0 && awayId != 0) {
                try {
                    apiService.getHeadToHead(
                        firstTeamId = homeId.toString(),
                        secondTeamId = awayId.toString()
                    ).allEvents().toFixtureResponseList()
                } catch (_: Exception) { emptyList() }
            } else emptyList()

            val lineups = if (lineupWrapper != null) {
                val homeLineup = lineupWrapper.home?.toFixtureLineup(
                    homeId ?: 0, response.teams?.home?.name, response.teams?.home?.logo
                )
                val awayLineup = lineupWrapper.away?.toFixtureLineup(
                    awayId ?: 0, response.teams?.away?.name, response.teams?.away?.logo
                )
                homeLineup?.toMatchLineups(awayLineup)
            } else {
                (response.lineups?.getOrNull(0))?.toMatchLineups(response.lineups?.getOrNull(1))
            }

            val matchStats = if (statsWrapper != null) {
                (statsWrapper.statistics ?: emptyList()).flatMap { stat ->
                    listOf(
                        MatchStat(teamId = homeId ?: 0, type = stat.type ?: "", value = stat.home?.display ?: "0"),
                        MatchStat(teamId = awayId ?: 0, type = stat.type ?: "", value = stat.away?.display ?: "0")
                    )
                }
            } else {
                (response.statistics ?: emptyList()).flatMap { ts ->
                    (ts.statistics ?: emptyList()).map {
                        MatchStat(ts.team?.id ?: 0, it.type ?: "", it.value?.display ?: "0")
                    }
                }
            }

            // v3 API: player_statistics.team_name is literally "home"/"away" (verified live)
            val playerPerformances = if (statsWrapper != null) {
                val playerStats = statsWrapper.player_statistics ?: emptyList()
                val homePlayers = playerStats.filter { it.team_name.equals("home", ignoreCase = true) }
                    .map { it.toPlayerPerformance() }
                val awayPlayers = playerStats.filter { it.team_name.equals("away", ignoreCase = true) }
                    .map { it.toPlayerPerformance() }

                listOfNotNull(
                    if (homePlayers.isNotEmpty()) PlayerMatchStats(homeId ?: 0, homePlayers) else null,
                    if (awayPlayers.isNotEmpty()) PlayerMatchStats(awayId ?: 0, awayPlayers) else null
                )
            } else emptyList()

            ApiResult.Success(
                MatchDetail(
                    match = response.toMatch(),
                    events = detailedEvents.map { it.toMatchEvent() },
                    lineups = lineups,
                    stats = matchStats,
                    players = playerPerformances,
                    prediction = predictions.firstOrNull()?.toPrediction()?.toMatchPrediction(),
                    odds = odds.map { it.toOddsResponse().toMatchOdds() }.flatten(),
                    injuries = emptyList(),
                    headToHead = h2h.map { it.toMatch() },
                    venue = response.fixture?.venue?.toVenueInfo(),
                    referee = response.fixture?.referee,
                    homeForm = homeForm,
                    awayForm = awayForm
                )
            )
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Unknown error occurred")
        }
    }

    override suspend fun getTeamDetail(teamId: Int, leagueId: Int, season: Int): ApiResult<TeamDetail> {
        return try {
            val teams = apiService.getTeams(teamId = teamId.toString())
            val apiTeam = teams.firstOrNull() ?: return ApiResult.Error("Team not found")

            val standings = try {
                apiService.getStandings(leagueId = leagueId.toString())
            } catch (_: Exception) {
                emptyList()
            }
            val teamStanding = standings.find { it.team_id == teamId.toString() }

            ApiResult.Success(apiTeam.toTeamDetail(teamStanding))
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Failed to load team details")
        }
    }

    override fun getStandings(leagueId: Int, season: Int): Flow<ApiResult<List<StandingItem>>> = flow {
        emit(ApiResult.Loading)
        try {
            val standings = apiService.getStandings(leagueId = leagueId.toString())
            val standing = standings.toStanding()
            val records = standing.league?.standings?.flatten()
            if (records != null) {
                emit(ApiResult.Success(records.map { it.toStandingItem() }))
            } else {
                emit(ApiResult.Error("No standings available"))
            }
        } catch (e: Exception) {
            emit(ApiResult.Error(e.message ?: "Failed to load standings"))
        }
    }

    /**
     * Team search: this API has NO direct team-name search endpoint (verified live:
     * get_teams only accepts league_id/team_id), so we find leagues whose names match
     * the query and scan their squads. Delegates to searchTeamsDirect.
     */
    override suspend fun searchTeams(query: String): ApiResult<List<TeamInfo>> {
        return try {
            val results = searchTeamsDirect(query)
            ApiResult.Success(
                results.mapNotNull { resp ->
                    resp.team?.let { TeamInfo(it.id, it.name ?: "", it.logo, country = it.country) }
                }
            )
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Search failed")
        }
    }

    override suspend fun getLeagues(): ApiResult<List<LeagueInfo>> {
        return try {
            val apiLeagues = apiService.getLeagues()
            val seen = mutableSetOf<String>()
            ApiResult.Success(apiLeagues.mapNotNull { league ->
                val id = league.league_id ?: return@mapNotNull null
                if (id in seen) return@mapNotNull null
                seen.add(id)
                league.toLeagueResponse().let { lr ->
                    LeagueInfo(
                        id = lr.league?.id ?: 0, name = lr.league?.name ?: "",
                        logo = lr.league?.logo, country = lr.country?.name,
                        flag = lr.country?.flag, season = lr.league?.season
                    )
                }
            })
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Failed to load leagues")
        }
    }

    override suspend fun getPlayerDetail(playerId: Int, season: Int): ApiResult<PlayerDetail> {
        return try {
            val players = apiService.getPlayers(playerId = playerId.toString())
            val firstPlayer = players.firstOrNull() ?: return ApiResult.Error("Player not found")

            val statsResponse = firstPlayer.toPlayerProfileStatisticsResponse()
            ApiResult.Success(statsResponse.toPlayerDetail())
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Failed to load player details")
        }
    }

    override fun getFixturesByLeagueSeason(leagueId: Int, season: Int): Flow<ApiResult<List<Match>>> = flow {
        emit(ApiResult.Loading)
        try {
            // get_events requires a date window: use the season span
            val from = if (season > 0) "$season-07-01" else daysFromToday(-400)
            val to = if (season > 0) "${season + 1}-06-30" else daysFromToday(400)
            val events = try {
                apiService.getEvents(from = from, to = to, leagueId = leagueId.toString())
            } catch (e: Exception) {
                // Fallback: a 400-day rolling window around today
                apiService.getEvents(from = daysFromToday(-200), to = daysFromToday(200), leagueId = leagueId.toString())
            }
            emit(ApiResult.Success(events.toFixtureResponseList().map { it.toMatch() }))
        } catch (e: Exception) {
            emit(ApiResult.Error(e.message ?: "Failed to load league fixtures"))
        }
    }

    override suspend fun getFixturesByTeamSeasonLeague(teamId: Int, leagueId: Int, season: Int): ApiResult<List<Match>> {
        return try {
            // get_events requires a date window (from/to). Use a 400-day window to cover a season.
            val events = apiService.getEvents(
                from = daysFromToday(-200),
                to = daysFromToday(200),
                teamId = teamId.toString()
            )
            val teamFixtures = events.filter {
                it.match_hometeam_id == teamId.toString() || it.match_awayteam_id == teamId.toString()
            }
            val filtered = if (leagueId > 0) {
                teamFixtures.filter { it.league_id == leagueId.toString() }
            } else teamFixtures
            ApiResult.Success(filtered.toFixtureResponseList().map { it.toMatch() })
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Failed to load team fixtures")
        }
    }

    override suspend fun getTeamInfoDirect(teamId: Int): TeamInfoResponse {
        val teams = try {
            apiService.getTeams(teamId = teamId.toString())
        } catch (e: Exception) {
            throw Exception("Failed to load team info: ${e.message}")
        }
        return teams.firstOrNull()?.toTeamInfoResponse()
            ?: throw Exception("Team not found")
    }

    override suspend fun getTeamStatisticsDirect(teamId: Int, leagueId: Int, season: Int): TeamStatistics {
        val res = teamRepository.getTeamStatistics(teamId, leagueId, season)
        if (res is ApiResult.Success<TeamStatistics>) return res.data
        throw Exception("Team statistics not available")
    }

    override suspend fun getTeamSquadDirect(teamId: Int): List<SquadResponse> {
        return try {
            val teams = apiService.getTeams(teamId = teamId.toString())
            teams.firstOrNull()?.let { team ->
                val players = team.players ?: emptyList()
                listOf(
                    SquadResponse(
                        team = FixtureTeam(id = team.team_key.toIntOr(0), name = team.team_name, logo = team.team_badge,
                            winner = null, update = null, colors = null),
                        players = players.toSquadPlayers()
                    )
                )
            } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun getTeamCoachesDirect(teamId: Int): List<Coach> {
        return try {
            val teams = apiService.getTeams(teamId = teamId.toString())
            val coaches = teams.firstOrNull()?.coaches ?: emptyList()
            coaches.toCoaches()
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun getRecentFixturesDirect(teamId: Int, leagueId: Int, season: Int): List<FixtureResponse> {
        return try {
            // get_events requires a date window (from/to); team_id filter optional
            val events = apiService.getEvents(
                from = daysFromToday(-200),
                to = daysFromToday(200),
                teamId = teamId.toString()
            )
            events.filter {
                it.match_hometeam_id == teamId.toString() || it.match_awayteam_id == teamId.toString()
            }.sortedByDescending { event ->
                eventTimestamp(event.match_date, event.match_time)
            }.take(5)
                .toFixtureResponseList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun getTopScorersDirect(leagueId: Int, season: Int): List<PlayerProfileStatisticsResponse> {
        return try {
            apiService.getTopScorers(leagueId = leagueId.toString()).map { it.toPlayerProfileStatisticsResponse() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Player pool (name -> season stats incl. shots_total) from one get_teams call
     * per league. Expensive, so it is cached process-wide and shared by Search and
     * the Stats "Advanced" tab — never called more than once per league per session.
     */
    @Volatile
    private var playersPoolCache: Pair<Int, List<ApiPlayer>>? = null

    suspend fun getPlayersPoolSnapshot(leagueId: Int): List<ApiPlayer> {
        playersPoolCache?.let { (lid, players) -> if (lid == leagueId) return players }
        val teams = try {
            kotlinx.coroutines.withTimeoutOrNull(25_000) {
                apiService.getTeams(leagueId = leagueId.toString())
            }
        } catch (_: Exception) { null }
        val players = teams?.flatMap { it.players ?: emptyList() } ?: emptyList()
        if (players.isNotEmpty()) playersPoolCache = leagueId to players
        return players
    }

    private suspend fun getTeamPool(): List<ApiStanding> {
        teamPoolCache?.let { return it }
        val pool = kotlinx.coroutines.withTimeoutOrNull(20_000) {
            kotlinx.coroutines.coroutineScope {
                SEARCH_POOL_LEAGUES.map { leagueId ->
                    async {
                        try {
                            apiService.getStandings(leagueId = leagueId.toString())
                        } catch (_: Exception) {
                            emptyList<ApiStanding>()
                        }
                    }
                }.awaitAll().flatten().distinctBy { it.team_id }
            }
        } ?: emptyList()
        if (pool.isNotEmpty()) teamPoolCache = pool
        return pool
    }

    override suspend fun searchTeamsDirect(query: String): List<TeamInfoResponse> {
        return try {
            val q = query.trim()
            // get_teams has NO name search (verified live: only league_id/team_id params).
            // Build a searchable team pool from standings of popular leagues — standings are
            // lightweight and include team_id, team_name and team_badge.
            val pool = getTeamPool()
            val matches = pool.filter { it.team_name?.contains(q, ignoreCase = true) == true }
                .map { s ->
                    TeamInfoResponse(
                        team = Team(id = s.team_id.toIntOr(0), name = s.team_name, code = null,
                            country = s.country_name, founded = null, national = null, logo = s.team_badge),
                        venue = null
                    )
                }
            if (matches.isNotEmpty()) return matches

            // Fallback: find leagues whose own name matches the query and scan their squads
            val leagues = apiService.getLeagues()
            val matchedLeagueIds = leagues.filter { it.league_name?.contains(q, ignoreCase = true) == true }
                .mapNotNull { it.league_id }
            val results = mutableListOf<TeamInfoResponse>()
            for (leagueId in matchedLeagueIds.take(3)) {
                try {
                    val teams = apiService.getTeams(leagueId = leagueId)
                    results += teams.filter { it.team_name?.contains(q, ignoreCase = true) == true }
                        .map { it.toTeamInfoResponse() }
                } catch (_: Exception) { }
            }
            results.distinctBy { it.team?.id }
        } catch (e: Exception) {
            emptyList()
        }
    }

}
