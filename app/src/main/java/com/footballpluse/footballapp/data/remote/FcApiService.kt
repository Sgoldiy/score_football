package com.footballpluse.footballapp.data.remote

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * FootballCharts API endpoints (verified against the live backend on 2026-09-22).
 *
 * IMPORTANT characteristics of this API:
 *  - Teams are identified by NAME (e.g. "Arsenal") / slug, not integer ids.
 *  - Seasons are string labels: "2026-2027" (cross-year) or "2026" (calendar year).
 *  - Errors are HTTP 200 with {"error":{"code":..,"message":..}} or 404 with the
 *    same body; empty lists are returned as {"count":0,"matches":[]}.
 *  - No players/squads/lineups/injuries/odds/live events exist in this API; those
 *    features degrade to honest empty states in the repositories.
 */
interface FcApiService {

    @GET("leagues/")
    suspend fun getLeagues(): FcLeaguesResponse

    @GET("leagues/{league}/table/")
    suspend fun getLeagueTable(
        @Path("league") league: String,
        @Query("season") season: String? = null,
        @Query("view") view: String? = null
    ): FcTableResponse

    @GET("leagues/{league}/results/")
    suspend fun getResults(
        @Path("league") league: String,
        @Query("season") season: String? = null,
        @Query("team") team: String? = null
    ): FcMatchesResponse

    @GET("leagues/{league}/fixtures/")
    suspend fun getFixtures(
        @Path("league") league: String,
        @Query("season") season: String? = null
    ): FcMatchesResponse

    @GET("leagues/{league}/projection/")
    suspend fun getProjection(
        @Path("league") league: String,
        @Query("season") season: String? = null
    ): FcProjectionResponse

    @GET("leagues/{league}/goal-timing/")
    suspend fun getGoalTiming(
        @Path("league") league: String,
        @Query("season") season: String? = null
    ): FcGoalTimingResponse

    @GET("leagues/{league}/teams/")
    suspend fun getTeams(
        @Path("league") league: String,
        @Query("season") season: String? = null
    ): FcTeamsResponse

    @GET("leagues/{league}/teams/{team}/")
    suspend fun getTeamPage(
        @Path("league") league: String,
        @Path("team") team: String,
        @Query("season") season: String? = null
    ): FcTeamPageResponse

    @GET("matches/{slug}/")
    suspend fun getMatch(
        @Path("slug", encoded = true) slug: String
    ): FcMatchDetailResponse

    @GET("track-record/")
    suspend fun getTrackRecord(): FcTrackRecordResponse
}
