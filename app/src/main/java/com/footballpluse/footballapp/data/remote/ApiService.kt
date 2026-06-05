package com.footballpluse.footballapp.data.remote

import com.footballpluse.footballapp.data.model.*
import retrofit2.http.GET
import retrofit2.http.Query

interface ApiService {

    // Fixtures
    @GET("fixtures")
    suspend fun getFixturesByDate(
        @Query("date") date: String
    ): ApiResponse<List<FixtureResponse>>

    @GET("fixtures")
    suspend fun getFixturesByTeamSeasonLeague(
        @Query("team") teamId: Int,
        @Query("season") season: Int,
        @Query("league") leagueId: Int
    ): ApiResponse<List<FixtureResponse>>

    @GET("fixtures")
    suspend fun getFixturesByLeagueSeason(
        @Query("league") leagueId: Int,
        @Query("season") season: Int
    ): ApiResponse<List<FixtureResponse>>

    @GET("fixtures")
    suspend fun getFixtureById(
        @Query("id") fixtureId: Int
    ): ApiResponse<List<FixtureResponse>>

    @GET("fixtures")
    suspend fun getNextFixtureForTeam(
        @Query("team") teamId: Int,
        @Query("next") next: Int = 1
    ): ApiResponse<List<FixtureResponse>>

    @GET("fixtures/headtohead")
    suspend fun getHeadToHead(
        @Query("h2h") teamsPair: String,
        @Query("last") last: Int = 5
    ): ApiResponse<List<FixtureResponse>>

    @GET("fixtures/lineups")
    suspend fun getFixtureLineups(
        @Query("fixture") fixtureId: Int
    ): ApiResponse<List<FixtureLineup>>

    @GET("fixtures/events")
    suspend fun getFixtureEvents(
        @Query("fixture") fixtureId: Int
    ): ApiResponse<List<FixtureEvent>>

    @GET("fixtures/statistics")
    suspend fun getFixtureStatistics(
        @Query("fixture") fixtureId: Int
    ): ApiResponse<List<FixtureTeamStatistics>>

    @GET("fixtures/players")
    suspend fun getFixturePlayerStatistics(
        @Query("fixture") fixtureId: Int
    ): ApiResponse<List<FixturePlayerStatisticsResponse>>

    // Standings
    @GET("standings")
    suspend fun getStandings(
        @Query("league") leagueId: Int,
        @Query("season") season: Int
    ): ApiResponse<List<Standing>>

    // Teams
    @GET("teams")
    suspend fun getTeamInfo(
        @Query("id") teamId: Int
    ): ApiResponse<List<TeamInfoResponse>>

    @GET("teams/statistics")
    suspend fun getTeamStatistics(
        @Query("team") teamId: Int,
        @Query("league") leagueId: Int,
        @Query("season") season: Int
    ): ApiResponse<TeamStatistics>

    @GET("venues")
    suspend fun getVenueById(
        @Query("id") venueId: Int
    ): ApiResponse<List<Venue>>

    @GET("players/squads")
    suspend fun getTeamSquad(
        @Query("team") teamId: Int
    ): ApiResponse<List<SquadResponse>>

    @GET("coachs")
    suspend fun getTeamCoaches(
        @Query("team") teamId: Int
    ): ApiResponse<List<Coach>>

    @GET("transfers")
    suspend fun getTeamTransfers(
        @Query("team") teamId: Int
    ): ApiResponse<List<Transfer>>

    // Players
    @GET("players")
    suspend fun getPlayerStats(
        @Query("id") playerId: Int,
        @Query("season") season: Int
    ): ApiResponse<List<PlayerProfileStatisticsResponse>>

    @GET("trophies")
    suspend fun getPlayerTrophies(
        @Query("player") playerId: Int
    ): ApiResponse<List<PlayerTrophy>>

    @GET("sidelined")
    suspend fun getPlayerSidelined(
        @Query("player") playerId: Int
    ): ApiResponse<List<PlayerSidelined>>

    // Leagues
    @GET("leagues")
    suspend fun getLeagues(): ApiResponse<List<LeagueResponse>>

    @GET("countries")
    suspend fun getCountries(): ApiResponse<List<Country>>

    @GET("leagues/seasons")
    suspend fun getSeasons(): ApiResponse<List<Int>>

    // Top Players
    @GET("players/topscorers")
    suspend fun getTopScorers(
        @Query("league") leagueId: Int,
        @Query("season") season: Int
    ): ApiResponse<List<PlayerProfileStatisticsResponse>>

    @GET("players/topassists")
    suspend fun getTopAssists(
        @Query("league") leagueId: Int,
        @Query("season") season: Int
    ): ApiResponse<List<PlayerProfileStatisticsResponse>>

    @GET("players/topyellowcards")
    suspend fun getTopYellowCards(
        @Query("league") leagueId: Int,
        @Query("season") season: Int
    ): ApiResponse<List<PlayerProfileStatisticsResponse>>

    @GET("players/topredcards")
    suspend fun getTopRedCards(
        @Query("league") leagueId: Int,
        @Query("season") season: Int
    ): ApiResponse<List<PlayerProfileStatisticsResponse>>

    // Search
    @GET("teams")
    suspend fun searchTeams(
        @Query("search") query: String
    ): ApiResponse<List<TeamInfoResponse>>

    @GET("leagues")
    suspend fun searchLeagues(
        @Query("search") query: String
    ): ApiResponse<List<LeagueResponse>>

    @GET("players")
    suspend fun searchPlayers(
        @Query("search") query: String,
        @Query("league") leagueId: Int? = null,
        @Query("season") season: Int? = null
    ): ApiResponse<List<PlayerProfileStatisticsResponse>>

    // Others
    @GET("predictions")
    suspend fun getPredictions(
        @Query("fixture") fixtureId: Int
    ): ApiResponse<List<Prediction>>

    @GET("odds")
    suspend fun getOdds(
        @Query("fixture") fixtureId: Int,
        @Query("bookmaker") bookmakerId: Int? = null
    ): ApiResponse<List<OddsResponse>>

    @GET("injuries")
    suspend fun getInjuries(
        @Query("fixture") fixtureId: Int
    ): ApiResponse<List<Injury>>
}
