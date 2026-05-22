package com.example.footballapp.data.repository

import com.example.footballapp.data.local.db.FixtureDao
import com.example.footballapp.data.local.db.LeagueDao
import com.example.footballapp.data.local.db.StandingDao
import com.example.footballapp.data.mapper.*
import com.example.footballapp.data.remote.ApiService
import com.example.footballapp.data.util.ApiResult
import com.example.footballapp.domain.model.*
import com.example.footballapp.domain.repository.FootballRepository
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
        
        // Emit cached data first
        val cached = fixtureDao.getFixturesByDate(date).first()
        if (cached.isNotEmpty()) {
            emit(ApiResult.Success(cached.map { it.toMatch() }))
        }

        try {
            val response = apiService.getFixturesByDate(date)
            if (response.response.isNotEmpty()) {
                val entities = response.response.map { it.toEntity(date) }
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

    override fun getLiveMatches(): Flow<ApiResult<List<Match>>> = flow {
        while (true) {
            try {
                val response = apiService.getFixturesByDate(java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date()))
                val liveMatches = response.response.filter { it.fixture?.status?.short in listOf("1H", "2H", "HT", "ET", "BT", "P", "INT", "LIVE") }
                emit(ApiResult.Success(liveMatches.map { it.toMatch() }))
            } catch (e: Exception) {
                emit(ApiResult.Error(e.message ?: "Failed to refresh live matches"))
            }
            delay(15000) // Refresh every 15 seconds
        }
    }

    override suspend fun getMatchDetail(fixtureId: Int): ApiResult<MatchDetail> {
        return try {
            val response = apiService.getFixtureById(fixtureId).response.firstOrNull()
                ?: return ApiResult.Error("Match not found")
            
            val events = apiService.getFixtureEvents(fixtureId).response
            val lineups = apiService.getFixtureLineups(fixtureId).response
            val stats = apiService.getFixtureStatistics(fixtureId).response
            val playerStats = apiService.getFixturePlayerStatistics(fixtureId).response
            val predictions = apiService.getPredictions(fixtureId).response
            val odds = apiService.getOdds(fixtureId).response
            val injuries = apiService.getInjuries(fixtureId).response
            
            val h2h = if (response.teams?.home?.id != null && response.teams?.away?.id != null) {
                apiService.getHeadToHead("${response.teams.home.id}-${response.teams.away.id}", 5).response
            } else emptyList()

            ApiResult.Success(
                MatchDetail(
                    match = response.toMatch(),
                    events = events.map { it.toMatchEvent() },
                    lineups = lineups.firstOrNull()?.toMatchLineups(lineups.getOrNull(1)),
                    stats = stats.flatMap { teamStat ->
                        teamStat.statistics?.map { 
                            MatchStat(teamStat.team?.id ?: 0, it.type ?: "", it.value?.display ?: "0")
                        } ?: emptyList()
                    },
                    players = playerStats.map { ps ->
                        PlayerMatchStats(
                            teamId = ps.team?.id ?: 0,
                            players = ps.players?.map { entry ->
                                PlayerPerformance(
                                    id = entry.player?.id ?: 0,
                                    name = entry.player?.name ?: "",
                                    photo = entry.player?.photo,
                                    rating = entry.statistics?.firstOrNull()?.games?.rating,
                                    position = entry.statistics?.firstOrNull()?.games?.position ?: "",
                                    goals = entry.statistics?.firstOrNull()?.goals?.total ?: 0,
                                    assists = entry.statistics?.firstOrNull()?.goals?.assists ?: 0
                                )
                            } ?: emptyList()
                        )
                    },
                    prediction = predictions.firstOrNull()?.toMatchPrediction(),
                    odds = odds.flatMap { it.toMatchOdds() },
                    injuries = injuries.map { it.toMatchInjury() },
                    headToHead = h2h.map { it.toMatch() }
                )
            )
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Unknown error occurred")
        }
    }

    override suspend fun getTeamDetail(teamId: Int, leagueId: Int, season: Int): ApiResult<TeamDetail> {
        return try {
            val info = apiService.getTeamInfo(teamId).response.firstOrNull()
                ?: return ApiResult.Error("Team not found")
            val stats = apiService.getTeamStatistics(teamId, leagueId, season).response
            val squad = apiService.getTeamSquad(teamId).response.firstOrNull()?.players ?: emptyList()
            val coaches = apiService.getTeamCoaches(teamId).response
            val transfers = apiService.getTeamTransfers(teamId).response

            ApiResult.Success(
                TeamDetail(
                    info = TeamInfo(info.team?.id ?: 0, info.team?.name ?: "", info.team?.logo),
                    venue = info.venue?.toVenueInfo(),
                    stats = stats.toTeamStats(),
                    squad = squad.map { it.toSquadMember() },
                    coaches = coaches.map { CoachInfo(it.id ?: 0, it.name ?: "", it.photo) },
                    transfers = transfers.flatMap { it.toTransferRecord() }
                )
            )
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Failed to load team details")
        }
    }

    override fun getStandings(leagueId: Int, season: Int): Flow<ApiResult<List<StandingItem>>> = flow {
        emit(ApiResult.Loading)
        try {
            val response = apiService.getStandings(leagueId, season)
            val standings = response.response.firstOrNull()?.league?.standings?.flatten()
            if (standings != null) {
                emit(ApiResult.Success(standings.map { it.toStandingItem() }))
            } else {
                emit(ApiResult.Error("No standings available"))
            }
        } catch (e: Exception) {
            emit(ApiResult.Error(e.message ?: "Failed to load standings"))
        }
    }

    override suspend fun searchTeams(query: String): ApiResult<List<TeamInfo>> {
        return try {
            val response = apiService.searchTeams(query)
            ApiResult.Success(response.response.map { 
                TeamInfo(
                    id = it.team?.id ?: 0,
                    name = it.team?.name ?: "",
                    logo = it.team?.logo
                )
            })
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Search failed")
        }
    }

    suspend fun searchLeagues(query: String): ApiResult<List<LeagueInfo>> {
        return try {
            val response = apiService.searchLeagues(query)
            ApiResult.Success(response.response.map { 
                LeagueInfo(
                    id = it.league?.id ?: 0,
                    name = it.league?.name ?: "",
                    logo = it.league?.logo,
                    country = it.country?.name,
                    flag = it.country?.flag,
                    season = it.seasons?.find { s -> s.current == true }?.year
                )
            })
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Search failed")
        }
    }


    override suspend fun getLeagues(): ApiResult<List<LeagueInfo>> {
        return try {
            val response = apiService.getLeagues()
            ApiResult.Success(response.response.map { 
                LeagueInfo(
                    id = it.league?.id ?: 0,
                    name = it.league?.name ?: "",
                    logo = it.league?.logo,
                    country = it.country?.name,
                    flag = it.country?.flag,
                    season = it.seasons?.find { s -> s.current == true }?.year
                )
            })
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Failed to load leagues")
        }
    }

    override suspend fun getPlayerDetail(playerId: Int, season: Int): ApiResult<PlayerDetail> {
        return try {
            val statsResponse = apiService.getPlayerStats(playerId, season).response
            val trophies = apiService.getPlayerTrophies(playerId).response
            val sidelined = apiService.getPlayerSidelined(playerId).response
            
            val firstStat = statsResponse.firstOrNull()
                ?: return ApiResult.Error("Player not found")

            ApiResult.Success(
                PlayerDetail(
                    info = firstStat.toPlayerInfo(),
                    stats = statsResponse.flatMap { it.statistics?.map { s -> s.toPlayerStatDetail() } ?: emptyList() },
                    trophies = trophies.map { it.toPlayerTrophyInfo() },
                    sidelined = sidelined.map { it.toPlayerInjuryInfo() }
                )
            )
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Failed to load player details")
        }
    }

    override fun getFixturesByLeagueSeason(leagueId: Int, season: Int): Flow<ApiResult<List<Match>>> = flow {
        emit(ApiResult.Loading)
        try {
            val response = apiService.getFixturesByLeagueSeason(leagueId, season)
            emit(ApiResult.Success(response.response.map { it.toMatch() }))
        } catch (e: Exception) {
            emit(ApiResult.Error(e.message ?: "Failed to load league fixtures"))
        }
    }
}
