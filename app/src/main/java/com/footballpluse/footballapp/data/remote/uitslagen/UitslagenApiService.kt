package com.footballpluse.footballapp.data.remote.uitslagen

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit interface for the open uitslagen.live/footapi JSON upstream
 * (no auth). `lang` and `version` are appended globally by the
 * DefaultParamsInterceptor in NetworkModule — see docs/LIVE_FEED.md.
 */
interface UitslagenApiService {

    @GET("footapi/fixtures/feed_livenow.json")
    suspend fun feedLivenow(): List<UitslagenCountryFeed>

    @GET("footapi/fixtures/feed_matches_aggregated.json")
    suspend fun feedAggregated(
        @Query("date") date: String,   // dd/MM/yyyy
        @Query("tzoffset") tzOffset: String = "0"
    ): List<UitslagenCountryFeed>

    @GET("footapi/fixtures_v2/{key}_small.json")
    suspend fun fixturesV2Small(@Path("key", encoded = true) leagueKey: String): UitslagenFixturesBlock

    @GET("footapi/matches/{id}.json")
    suspend fun matchDetail(
        @Path("id", encoded = true) matchId: String,
        @Query("h2h") h2h: String = "0"
    ): UitslagenMatchDetail

    @GET("footapi/search_v3")
    suspend fun search(
        @Query("q") query: String,
        @Query("country") country: String? = null
    ): UitslagenSearchResponse

    @GET("footapi/team_gs/{id}.json")
    suspend fun teamPage(@Path("id", encoded = true) teamId: String): UitslagenTeamPage

    @GET("footapi/players/{id}.json")
    suspend fun playerPage(@Path("id", encoded = true) playerId: String): UitslagenPlayerPage

    companion object {
        const val TEAM_LOGO_URL = "https://uitslagen.live/footapi/images/teams_gs/%s.png"

        /**
         * Logo URL for a numeric team id, or null when the id is not a usable
         * number — the upstream serves a broken image for blank/garbage ids,
         * so the UI falls back to its placeholder instead.
         */
        fun teamLogoUrl(teamId: String?): String? =
            teamId?.takeIf { it.isNotBlank() && it.all { c -> c.isDigit() } }
                ?.let { TEAM_LOGO_URL.format(it) }
    }
}
