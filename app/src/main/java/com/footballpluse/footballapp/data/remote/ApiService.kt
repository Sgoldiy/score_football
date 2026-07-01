package com.footballpluse.footballapp.data.remote

import com.footballpluse.footballapp.data.model.*
import retrofit2.http.GET
import retrofit2.http.Query

interface ApiService {

    // Countries
    @GET("?action=get_countries")
    suspend fun getCountries(): List<ApiCountry>

    // Leagues
    @GET("?action=get_leagues")
    suspend fun getLeagues(
        @Query("country_id") countryId: String? = null
    ): List<ApiLeague>

    // Teams
    @GET("?action=get_teams")
    suspend fun getTeams(
        @Query("league_id") leagueId: String? = null,
        @Query("team_id") teamId: String? = null
    ): List<ApiTeam>

    // Players
    @GET("?action=get_players")
    suspend fun getPlayers(
        @Query("player_id") playerId: String? = null,
        @Query("player_name") playerName: String? = null
    ): List<ApiPlayer>

    // Standings
    @GET("?action=get_standings")
    suspend fun getStandings(
        @Query("league_id") leagueId: String
    ): List<ApiStanding>

    // Events (Fixtures)
    @GET("?action=get_events")
    suspend fun getEvents(
        @Query("from") from: String? = null,
        @Query("to") to: String? = null,
        @Query("league_id") leagueId: String? = null,
        @Query("match_id") matchId: String? = null
    ): List<ApiEvent>

    // Lineups
    @GET("?action=get_lineups")
    suspend fun getLineups(
        @Query("match_id") matchId: String
    ): Map<String, ApiLineupResponse>

    // Statistics
    @GET("?action=get_statistics")
    suspend fun getMatchStatistics(
        @Query("match_id") matchId: String
    ): Map<String, ApiMatchStatisticsResponse>

    // Odds
    @GET("?action=get_odds")
    suspend fun getOdds(
        @Query("match_id") matchId: String
    ): List<ApiOdd>

    // Top Scorers
    @GET("?action=get_topscorers")
    suspend fun getTopScorers(
        @Query("league_id") leagueId: String
    ): List<ApiTopScorer>

    // Head to Head
    @GET("?action=get_H2H")
    suspend fun getHeadToHead(
        @Query("firstTeamId") firstTeamId: String,
        @Query("secondTeamId") secondTeamId: String
    ): ApiH2HResponse

    // Livescore
    @GET("?action=get_livescore")
    suspend fun getLivescore(
        @Query("match_id") matchId: String? = null
    ): List<ApiEvent>

    // Predictions
    @GET("?action=get_predictions")
    suspend fun getPredictions(
        @Query("match_id") matchId: String
    ): List<ApiPrediction>
}
