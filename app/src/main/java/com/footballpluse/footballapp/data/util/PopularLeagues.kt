package com.footballpluse.footballapp.data.util

object PopularLeagues {
    /**
     * League IDs from the first two rows of HomeScreen:
     * Row 1: Top Domestic Leagues
     * Row 2: Top Competitions
     */
    val POPULAR_LEAGUE_IDS = listOf(
        // Row 1: Top Domestic Leagues
        152, // Premier League
        302, // LaLiga
        207, // Serie A
        175, // Bundesliga
        168, // Ligue 1
        88,  // Eredivisie
        94,  // Liga Portugal
        144, // Jupiler Pro League
        203, // Saudi Pro League
        187, // Super League
        // Row 2: Top Competitions
        28,  // Champions League
        3,   // Europa League
        848, // Conference League
        5,   // Nations League
        1,   // World Cup
        4,   // EURO
        9,   // Copa América
        17   // Asian Cup
    )

    fun getPopularityRank(leagueId: Int, leagueName: String?): Int {
        val idx = POPULAR_LEAGUE_IDS.indexOf(leagueId)
        if (idx >= 0) return idx
        val lower = leagueName?.lowercase().orEmpty()
        return when {
            lower.contains("champions league") -> 20
            lower.contains("premier league") -> 21
            lower.contains("laliga") || lower.contains("la liga") || lower.contains("primera division") -> 22
            lower.contains("serie a") -> 23
            lower.contains("bundesliga") -> 24
            lower.contains("ligue 1") -> 25
            lower.contains("europa league") -> 26
            lower.contains("conference league") -> 27
            lower.contains("nations league") -> 28
            lower.contains("eredivisie") -> 29
            lower.contains("primeira liga") || lower.contains("liga portugal") -> 30
            lower.contains("saudi") -> 31
            lower.contains("pro league") -> 32
            lower.contains("super league") -> 33
            lower.contains("world cup") -> 34
            lower.contains("euro") -> 35
            lower.contains("copa america") || lower.contains("copa américa") -> 36
            lower.contains("asian cup") -> 37
            else -> 1000
        }
    }

    fun isPopular(leagueId: Int, leagueName: String?): Boolean {
        return getPopularityRank(leagueId, leagueName) < 1000
    }
}
