package com.example.footballapp.data.remote

import com.example.footballapp.data.model.*
import retrofit2.http.GET
import retrofit2.http.Query

interface ApiService {

    // Fixtures
    @GET("fixtures")
    suspend fun getFixturesByDate(
        @Query("date") date: String
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

    @GET("fixtures/headtohead")
    suspend fun getHeadToHead(
        @Query("h2h") teamsPair: String,
        @Query("last") last: Int = 5
    ): ApiResponse<List<FixtureResponse>>

    @GET("fixtures/lineups")
    suspend fun getFixtureLineups(
        @Query("fixture") fixtureId: Int
    ): ApiResponse<List<Any>> // Simplified, actual type could be more specific

    @GET("fixtures/events")
    suspend fun getFixtureEvents(
        @Query("fixture") fixtureId: Int
    ): ApiResponse<List<Any>>

    @GET("fixtures/statistics")
    suspend fun getFixtureStatistics(
        @Query("fixture") fixtureId: Int
    ): ApiResponse<List<Any>>

    @GET("fixtures/players")
    suspend fun getFixturePlayerStatistics(
        @Query("fixture") fixtureId: Int
    ): ApiResponse<List<Any>>

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
    ): ApiResponse<List<Any>>

    @GET("teams/statistics")
    suspend fun getTeamStatistics(
        @Query("team") teamId: Int,
        @Query("league") leagueId: Int,
        @Query("season") season: Int
    ): ApiResponse<TeamStatistics>

    @GET("venues")
    suspend fun getVenues(
        @Query("team") teamId: Int
    ): ApiResponse<List<Venue>>

    @GET("players/squad")
    suspend fun getTeamSquad(
        @Query("team") teamId: Int
    ): ApiResponse<List<Any>>

    @GET("coachs")
    suspend fun getTeamCoaches(
        @Query("team") teamId: Int
    ): ApiResponse<List<Any>>

    @GET("transfers")
    suspend fun getTeamTransfers(
        @Query("team") teamId: Int
    ): ApiResponse<List<Transfer>>

    // Players
    @GET("players")
    suspend fun getPlayerStats(
        @Query("id") playerId: Int,
        @Query("season") season: Int
    ): ApiResponse<List<PlayerStatistics>>

    @GET("trophies")
    suspend fun getPlayerTrophies(
        @Query("player") playerId: Int
    ): ApiResponse<List<Any>>

    @GET("sidelined")
    suspend fun getPlayerSidelined(
        @Query("player") playerId: Int
    ): ApiResponse<List<Any>>

    // Leagues
    @GET("leagues")
    suspend fun getLeagues(): ApiResponse<List<League>>

    @GET("countries")
    suspend fun getCountries(): ApiResponse<List<Country>>

    @GET("leagues/seasons")
    suspend fun getSeasons(): ApiResponse<List<Int>>

    // Top Players
    @GET("players/topscorers")
    suspend fun getTopScorers(
        @Query("league") leagueId: Int,
        @Query("season") season: Int
    ): ApiResponse<List<PlayerStatistics>>

    @GET("players/topassists")
    suspend fun getTopAssists(
        @Query("league") leagueId: Int,
        @Query("season") season: Int
    ): ApiResponse<List<PlayerStatistics>>

    @GET("players/topyellowcards")
    suspend fun getTopYellowCards(
        @Query("league") leagueId: Int,
        @Query("season") season: Int
    ): ApiResponse<List<PlayerStatistics>>

    @GET("players/topredcards")
    suspend fun getTopRedCards(
        @Query("league") leagueId: Int,
        @Query("season") season: Int
    ): ApiResponse<List<PlayerStatistics>>

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
