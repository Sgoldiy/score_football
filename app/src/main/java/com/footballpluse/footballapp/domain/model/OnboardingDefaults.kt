package com.footballpluse.footballapp.domain.model

data class OnboardingLeague(
    val id: Int,
    val name: String,
    val country: String,
    val shortName: String
)

data class OnboardingClub(
    val clubId: Int,
    val clubName: String,
    val leagueId: Int
)

object OnboardingDefaults {
    // Hardcoded leagues (no API call)
    val leagues: List<OnboardingLeague> = listOf(
        OnboardingLeague(id = 152, name = "Premier League", country = "England", shortName = "EPL"),
        OnboardingLeague(id = 302, name = "La Liga", country = "Spain", shortName = "LL"),
        OnboardingLeague(id = 207, name = "Serie A", country = "Italy", shortName = "SA"),
        OnboardingLeague(id = 175, name = "Bundesliga", country = "Germany", shortName = "BL"),
        OnboardingLeague(id = 168, name = "Ligue 1", country = "France", shortName = "L1"),
    )

    // Hardcoded clubs (no API call)
    val clubsByLeagueId: Map<Int, List<OnboardingClub>> = mapOf(
        152 to listOf(
            OnboardingClub(clubId = 80, clubName = "Manchester City", leagueId = 152),
            OnboardingClub(clubId = 141, clubName = "Arsenal FC", leagueId = 152),
            OnboardingClub(clubId = 84, clubName = "Liverpool", leagueId = 152),
            OnboardingClub(clubId = 88, clubName = "Chelsea", leagueId = 152),
            OnboardingClub(clubId = 102, clubName = "Manchester United", leagueId = 152),
            OnboardingClub(clubId = 164, clubName = "Tottenham Hotspur", leagueId = 152),
        ),
        302 to listOf(
            OnboardingClub(clubId = 76, clubName = "Real Madrid", leagueId = 302),
            OnboardingClub(clubId = 97, clubName = "Barcelona", leagueId = 302),
            OnboardingClub(clubId = 73, clubName = "Atlético de Madrid", leagueId = 302),
            OnboardingClub(clubId = 89, clubName = "Sevilla", leagueId = 302),
            OnboardingClub(clubId = 7272, clubName = "Valencia", leagueId = 302),
            OnboardingClub(clubId = 7258, clubName = "Athletic Club", leagueId = 302),
        ),
        207 to listOf(
            OnboardingClub(clubId = 79, clubName = "Internazionale", leagueId = 207),
            OnboardingClub(clubId = 96, clubName = "Juventus FC", leagueId = 207),
            OnboardingClub(clubId = 159, clubName = "Milan", leagueId = 207),
            OnboardingClub(clubId = 152, clubName = "Napoli", leagueId = 207),
            OnboardingClub(clubId = 139, clubName = "Roma", leagueId = 207),
            OnboardingClub(clubId = 93, clubName = "Lazio", leagueId = 207),
        ),
        175 to listOf(
            OnboardingClub(clubId = 72, clubName = "Bayern München", leagueId = 175),
            OnboardingClub(clubId = 92, clubName = "Borussia Dortmund", leagueId = 175),
            OnboardingClub(clubId = 101, clubName = "RB Leipzig", leagueId = 175),
            OnboardingClub(clubId = 143, clubName = "Bayer Leverkusen", leagueId = 175),
            OnboardingClub(clubId = 3945, clubName = "Eintracht Frankfurt", leagueId = 175),
            OnboardingClub(clubId = 196, clubName = "Wolfsburg", leagueId = 175),
        ),
        168 to listOf(
            OnboardingClub(clubId = 100, clubName = "PSG", leagueId = 168),
            OnboardingClub(clubId = 83, clubName = "Olympique Marseille", leagueId = 168),
            OnboardingClub(clubId = 3817, clubName = "Monaco", leagueId = 168),
            OnboardingClub(clubId = 3815, clubName = "Olympique Lyonnais", leagueId = 168),
            OnboardingClub(clubId = 160, clubName = "Lille", leagueId = 168),
            OnboardingClub(clubId = 145, clubName = "Nice", leagueId = 168),
        ),
    )

    fun leagueName(leagueId: String): String? {
        return leagues.find { it.id.toString() == leagueId }?.name
    }

    fun leagueLogoUrl(leagueId: Int, leagueName: String? = null): String {
        val slug = leagueName?.let { nameToSlug(it) }
        return slug?.let { "https://apiv3.apifootball.com/badges/logo_leagues/${leagueId}_$it.png" }
            ?: "https://apiv3.apifootball.com/badges/logo_leagues/$leagueId.png"
    }

    fun clubLogoUrl(teamId: Int, clubName: String? = null): String {
        val slug = clubName?.let { nameToSlug(it) }
        return slug?.let { "https://apiv3.apifootball.com/badges/${teamId}_$it.jpg" }
            ?: "https://apiv3.apifootball.com/badges/$teamId.jpg"
    }

    private fun nameToSlug(name: String): String {
        return name.lowercase()
            .replace("&", "and")
            .replace("'", "")
            .replace(".", "")
            .replace(Regex("[^a-z0-9\\-]"), "-")
            .replace(Regex("-{2,}"), "-")
            .trim('-')
    }
}
