package com.footballpluse.footballapp.data.remote

import com.footballpluse.footballapp.data.model.*
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * API-Football v3 style endpoints (verified against live API).
 *
 * IMPORTANT constraints discovered from the live API:
 *  - get_events REQUIRES a date window (from/to). Calls with only league_id or
 *    team_id return {"error":201,"message":"Required parameters missing"}.
 *  - get_teams REQUIRES either league_id or team_id. There is no unfiltered
 *    "get all teams" call and no team_name search parameter.
 *  - get_players REQUIRES either player_id or player_name (exact full name,
 *    e.g. "Erling Haaland"). Partial names return an empty array.
 *  - Error responses are HTTP 200 with {"error":404,"message":"No event found"}
 *    or {"error":201,...}. They parse as an empty/failed list, never an exception
 *    with a useful message, so callers must treat empty responses carefully.
 */
interface ApiService {

    // Countries
    @GET("?action=get_countries")
    suspend fun getCountries(): List<ApiCountry>

    // Leagues
    @GET("?action=get_leagues")
    suspend fun getLeagues(
        @Query("country_id") countryId: String? = null
    ): List<ApiLeague>

    // Teams: requires league_id OR team_id
    @GET("?action=get_teams")
    suspend fun getTeams(
        @Query("league_id") leagueId: String? = null,
        @Query("team_id") teamId: String? = null
    ): List<ApiTeam>

    // Players: requires player_id OR exact full player_name
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

    // Events: date window (from/to) is MANDATORY; other filters are optional additions
    @GET("?action=get_events")
    suspend fun getEvents(
        @Query("from") from: String,
        @Query("to") to: String,
        @Query("league_id") leagueId: String? = null,
        @Query("team_id") teamId: String? = null,
        @Query("match_id") matchId: String? = null
    ): List<ApiEvent>

    // Single match details (match_id alone is accepted for match lookups)
    @GET("?action=get_events")
    suspend fun getEventById(
        @Query("match_id") matchId: String
    ): List<ApiEvent>

    // Lineups
    @GET("?action=get_lineups")
    suspend fun getLineups(
        @Query("match_id") matchId: String
    ): Map<String, ApiLineupResponse>

    // Statistics (includes player_statistics with team_name = "home"/"away")
    @GET("?action=get_statistics")
    suspend fun getMatchStatistics(
        @Query("match_id") matchId: String
    ): Map<String, ApiMatchStatisticsResponse>

    // Odds (from/to mandatory when not filtering by match_id)
    @GET("?action=get_odds")
    suspend fun getOdds(
        @Query("from") from: String? = null,
        @Query("to") to: String? = null,
        @Query("match_id") matchId: String? = null
    ): List<ApiOdd>

    // Top Scorers
    @GET("?action=get_topscorers")
    suspend fun getTopScorers(
        @Query("league_id") leagueId: String
    ): List<ApiTopScorer>

    // Head to Head: team IDs or team names
    @GET("?action=get_H2H")
    suspend fun getHeadToHead(
        @Query("firstTeamId") firstTeamId: String,
        @Query("secondTeamId") secondTeamId: String
    ): ApiH2HResponse

    // Livescore
    @GET("?action=get_events&match_live=1")
    suspend fun getLivescore(
        @Query("match_id") matchId: String? = null,
        @Query("league_id") leagueId: String? = null
    ): List<ApiEvent>

    // Predictions: match_id alone is accepted
    @GET("?action=get_predictions")
    suspend fun getPredictions(
        @Query("match_id") matchId: String
    ): List<ApiPrediction>
}
