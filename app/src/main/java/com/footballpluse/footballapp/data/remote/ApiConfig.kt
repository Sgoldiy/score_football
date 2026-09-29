package com.footballpluse.footballapp.data.remote

/**
 * Central configuration for the FootballCharts backend used by the app.
 * Keep every host/key reference here so switching plan or key is a one-line change.
 */
object ApiConfig {
    const val HOST = "footballcharts-backend.onrender.com"

    /** Bearer key for the FootballCharts API. Do not commit real production keys to source control. */
    const val API_KEY = "fc_cxuygHXyTuc1VQQFpQEptz6SmgvF0kqK"

    const val BASE_URL = "https://$HOST/api/v1/"

    const val ATTRIBUTION = "Data by football-charts.com"

    /**
     * Team logo from the FootballCharts CDN. Verified live 2026-09-28: logo_url
     * carries a COUNTRY segment —
     *   https://.../static/team-logos-organized/england/premier/manchester-city.png
     * Build it when the country is known; fall back to the legacy 2-segment shape
     * (still used by some responses).
     */
    fun teamLogoUrl(country: String?, leagueSlug: String, teamSlug: String): String {
        val base = "https://$HOST/static/team-logos-organized"
        val c = country?.let { teamSlug(it) }
        return if (c != null) "$base/$c/$leagueSlug/$teamSlug.png"
        else "$base/$leagueSlug/$teamSlug.png"
    }

    /**
     * Slug for a team name the way FC generates them ("Manchester Utd" ->
     * "manchester-utd"). Good enough for logo URLs and team-page links.
     */
    fun teamSlug(name: String?): String? {
        if (name.isNullOrBlank()) return null
        return name.lowercase()
            .replace("&", "and")
            .replace(Regex("[^a-z0-9\\- ]"), "")
            .trim()
            .replace(Regex("\\s+"), "-")
            .replace(Regex("-+"), "-")
            .removeSuffix("-")
    }
}
