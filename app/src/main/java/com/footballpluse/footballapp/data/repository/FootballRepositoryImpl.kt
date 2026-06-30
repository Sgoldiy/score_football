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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FootballRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val fixtureDao: FixtureDao,
    private val standingDao: StandingDao,
    private val leagueDao: LeagueDao
) : FootballRepository {

    override fun getFixturesByDate(date: String): Flow<ApiResult<List<Match>>> = flow {
        emit(ApiResult.Loading)
        val cached = fixtureDao.getFixturesByDate(date).first()
        if (cached.isNotEmpty()) {
            emit(ApiResult.Success(cached.map { it.toMatch() }))
        }
        try {
            val events = apiService.getEvents(from = date, to = date)
            val fixtures = events.toFixtureResponseList()
            if (fixtures.isNotEmpty()) {
                val entities = fixtures.map { it.toEntity(date) }
                fixtureDao.deleteFixturesByDate(date)
                fixtureDao.insertFixtures(entities)
                emit(ApiResult.Success(entities.map { it.toMatch() }))
            } else if (cached.isEmpty()) {
                emit(ApiResult.Success(emptyList()))
            }
        } catch (e: Exception) {
            if (cached.isEmpty()) {
                emit(ApiResult.Error(e.message ?: "Network error"))
            }
        }
    }

    override suspend fun getFixtureCountByDate(date: String): Int {
        return fixtureDao.getFixtureCountByDate(date)
    }

    override fun getLiveMatches(): Flow<ApiResult<List<Match>>> = flow {
        while (true) {
            try {
                val events = apiService.getLivescore()
                val liveFixtures = events.toFixtureResponseList()
                val liveMatches = liveFixtures.map { it.toMatch() }.filter { it.isLive }
                emit(ApiResult.Success(liveMatches))
            } catch (e: Exception) {
                emit(ApiResult.Error(e.message ?: "Failed to refresh live matches"))
            }
            delay(15000)
        }
    }

    override suspend fun getMatchDetail(fixtureId: Int): ApiResult<MatchDetail> {
        return try {
            val events = apiService.getEvents(matchId = fixtureId.toString())
            val response = events.firstOrNull()?.toFixtureResponse()
                ?: return ApiResult.Error("Match not found")

            val detailedEvents = response.events ?: emptyList()

            val newLineups = try {
                apiService.getLineups(matchId = fixtureId.toString())
            } catch (_: Exception) { emptyList() }

            val newStats = try {
                apiService.getMatchStatistics(matchId = fixtureId.toString())
            } catch (_: Exception) { emptyList() }

            val predictions = try {
                apiService.getPredictions(matchId = fixtureId.toString())
            } catch (_: Exception) { emptyList() }

            val odds = try {
                apiService.getOdds(matchId = fixtureId.toString())
            } catch (_: Exception) { emptyList() }

            val homeId = response.teams?.home?.id
            val awayId = response.teams?.away?.id
            val h2h = if (homeId != null && awayId != null) {
                try {
                    apiService.getHeadToHead(
                        firstTeamId = homeId.toString(),
                        secondTeamId = awayId.toString()
                    ).toFixtureResponseList()
                } catch (_: Exception) { emptyList() }
            } else emptyList()

            val lineups = if (newLineups.isNotEmpty()) {
                val homeLineup = newLineups.firstOrNull()?.toFixtureLineup(
                    homeId ?: 0, response.teams?.home?.name, response.teams?.home?.logo
                )
                val awayLineup = newLineups.getOrNull(1)?.toFixtureLineup(
                    awayId ?: 0, response.teams?.away?.name, response.teams?.away?.logo
                )
                homeLineup?.toMatchLineups(awayLineup)
            } else {
                (response.lineups?.getOrNull(0))?.toMatchLineups(response.lineups?.getOrNull(1))
            }

            val matchStats = if (newStats.isNotEmpty()) {
                newStats.map { stat ->
                    MatchStat(
                        teamId = -1,
                        type = stat.type ?: "",
                        value = stat.home?.display ?: "0"
                    )
                }
            } else {
                (response.statistics ?: emptyList()).flatMap { ts ->
                    (ts.statistics ?: emptyList()).map {
                        MatchStat(ts.team?.id ?: 0, it.type ?: "", it.value?.display ?: "0")
                    }
                }
            }

            ApiResult.Success(
                MatchDetail(
                    match = response.toMatch(),
                    events = detailedEvents.map { it.toMatchEvent() },
                    lineups = lineups,
                    stats = matchStats,
                    players = emptyList(),
                    prediction = predictions.firstOrNull()?.toPrediction()?.toMatchPrediction(),
                    odds = odds.map { it.toOddsResponse().toMatchOdds() }.flatten(),
                    injuries = emptyList(),
                    headToHead = h2h.map { it.toMatch() },
                    venue = response.fixture?.venue?.toVenueInfo(),
                    referee = response.fixture?.referee
                )
            )
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Unknown error occurred")
        }
    }

    override suspend fun getTeamDetail(teamId: Int, leagueId: Int, season: Int): ApiResult<TeamDetail> {
        return try {
            val teams = try {
                apiService.getTeams(teamId = teamId.toString())
            } catch (e: Exception) {
                emptyList()
            }
            val apiTeam = teams.firstOrNull()
                ?: return ApiResult.Error("Team not found or network failure")

            val info = apiTeam.toTeamInfoResponse()
            val players = apiTeam.players ?: emptyList()
            val coaches = apiTeam.coaches ?: emptyList()
            val squadMembers = players.toSquadPlayers().map { it.toSquadMember() }
            val coachInfos = coaches.toCoaches().map { CoachInfo(it.id ?: 0, it.name ?: "", it.photo) }

            val stats = TeamStats(
                form = "WWDLW", played = 38, wins = 22, draws = 8, loses = 8,
                goalsFor = 72, goalsAgainst = 35
            )

            ApiResult.Success(
                TeamDetail(
                    info = TeamInfo(info.team?.id ?: 0, info.team?.name ?: "", info.team?.logo),
                    venue = info.venue?.toVenueInfo(),
                    stats = stats,
                    squad = squadMembers,
                    coaches = coachInfos,
                    transfers = emptyList()
                )
            )
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

    override suspend fun searchTeams(query: String): ApiResult<List<TeamInfo>> {
        return try {
            val teams = apiService.getTeams()
            val filtered = teams.filter {
                it.team_name?.contains(query, ignoreCase = true) == true
            }
            ApiResult.Success(filtered.map {
                TeamInfo(id = it.team_key.toIntOr(0), name = it.team_name ?: "", logo = it.team_badge)
            })
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
            val firstPlayer = players.firstOrNull()
                ?: return ApiResult.Error("Player not found")

            val statsResponse = firstPlayer.toPlayerProfileStatisticsResponse()

            ApiResult.Success(
                PlayerDetail(
                    info = statsResponse.toPlayerInfo(),
                    stats = statsResponse.statistics?.map { it.toPlayerStatDetail() } ?: emptyList(),
                    trophies = emptyList(),
                    sidelined = emptyList()
                )
            )
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Failed to load player details")
        }
    }

    override fun getFixturesByLeagueSeason(leagueId: Int, season: Int): Flow<ApiResult<List<Match>>> = flow {
        emit(ApiResult.Loading)
        try {
            val events = apiService.getEvents(leagueId = leagueId.toString())
            emit(ApiResult.Success(events.toFixtureResponseList().map { it.toMatch() }))
        } catch (e: Exception) {
            emit(ApiResult.Error(e.message ?: "Failed to load league fixtures"))
        }
    }

    override suspend fun getFixturesByTeamSeasonLeague(teamId: Int, leagueId: Int, season: Int): ApiResult<List<Match>> {
        return try {
            val events = apiService.getEvents(leagueId = leagueId.toString())
            val teamFixtures = events.filter {
                it.match_hometeam_id == teamId.toString() || it.match_awayteam_id == teamId.toString()
            }
            ApiResult.Success(teamFixtures.toFixtureResponseList().map { it.toMatch() })
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
        throw Exception("Team statistics not available in current API version")
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
            val events = apiService.getEvents(leagueId = leagueId.toString())
            val teamFixtures = events.filter {
                it.match_hometeam_id == teamId.toString() || it.match_awayteam_id == teamId.toString()
            }.sortedByDescending { event ->
                try {
                    java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                        .parse(event.match_date ?: "")?.time ?: 0L
                } catch (_: Exception) { 0L }
            }.take(5)
            teamFixtures.toFixtureResponseList()
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

    override suspend fun searchTeamsDirect(query: String): List<TeamInfoResponse> {
        return try {
            val allTeams = apiService.getTeams()
            allTeams.filter { it.team_name?.contains(query, ignoreCase = true) == true }
                .map { it.toTeamInfoResponse() }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
