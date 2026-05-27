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
        OnboardingLeague(id = 39, name = "Premier League", country = "England", shortName = "EPL"),
        OnboardingLeague(id = 140, name = "La Liga", country = "Spain", shortName = "LL"),
        OnboardingLeague(id = 135, name = "Serie A", country = "Italy", shortName = "SA"),
        OnboardingLeague(id = 78, name = "Bundesliga", country = "Germany", shortName = "BL"),
        OnboardingLeague(id = 61, name = "Ligue 1", country = "France", shortName = "L1"),
    )

    // Hardcoded clubs (no API call)
    val clubsByLeagueId: Map<Int, List<OnboardingClub>> = mapOf(
        39 to listOf(
            OnboardingClub(clubId = 50, clubName = "Manchester City", leagueId = 39),
            OnboardingClub(clubId = 42, clubName = "Arsenal", leagueId = 39),
            OnboardingClub(clubId = 40, clubName = "Liverpool", leagueId = 39),
            OnboardingClub(clubId = 49, clubName = "Chelsea", leagueId = 39),
            OnboardingClub(clubId = 33, clubName = "Manchester United", leagueId = 39),
            OnboardingClub(clubId = 47, clubName = "Tottenham Hotspur", leagueId = 39),
        ),
        140 to listOf(
            OnboardingClub(clubId = 541, clubName = "Real Madrid", leagueId = 140),
            OnboardingClub(clubId = 529, clubName = "Barcelona", leagueId = 140),
            OnboardingClub(clubId = 530, clubName = "Atletico Madrid", leagueId = 140),
            OnboardingClub(clubId = 536, clubName = "Sevilla", leagueId = 140),
            OnboardingClub(clubId = 532, clubName = "Valencia", leagueId = 140),
            OnboardingClub(clubId = 531, clubName = "Athletic Bilbao", leagueId = 140),
        ),
        135 to listOf(
            OnboardingClub(clubId = 505, clubName = "Inter Milan", leagueId = 135),
            OnboardingClub(clubId = 496, clubName = "Juventus", leagueId = 135),
            OnboardingClub(clubId = 489, clubName = "AC Milan", leagueId = 135),
            OnboardingClub(clubId = 492, clubName = "Napoli", leagueId = 135),
            OnboardingClub(clubId = 497, clubName = "Roma", leagueId = 135),
            OnboardingClub(clubId = 487, clubName = "Lazio", leagueId = 135),
        ),
        78 to listOf(
            OnboardingClub(clubId = 157, clubName = "Bayern Munich", leagueId = 78),
            OnboardingClub(clubId = 165, clubName = "Borussia Dortmund", leagueId = 78),
            OnboardingClub(clubId = 173, clubName = "RB Leipzig", leagueId = 78),
            OnboardingClub(clubId = 168, clubName = "Bayer Leverkusen", leagueId = 78),
            OnboardingClub(clubId = 169, clubName = "Eintracht Frankfurt", leagueId = 78),
            OnboardingClub(clubId = 161, clubName = "Wolfsburg", leagueId = 78),
        ),
        61 to listOf(
            OnboardingClub(clubId = 85, clubName = "PSG", leagueId = 61),
            OnboardingClub(clubId = 81, clubName = "Marseille", leagueId = 61),
            OnboardingClub(clubId = 91, clubName = "Monaco", leagueId = 61),
            OnboardingClub(clubId = 80, clubName = "Lyon", leagueId = 61),
            OnboardingClub(clubId = 79, clubName = "Lille", leagueId = 61),
            OnboardingClub(clubId = 84, clubName = "Nice", leagueId = 61),
        ),
    )

    // Public API-Football asset URLs (used throughout many apps; avoids needing a leagues endpoint call).
    fun leagueLogoUrl(leagueId: Int): String = "https://media.api-sports.io/football/leagues/$leagueId.png"
    fun clubLogoUrl(teamId: Int): String = "https://media.api-sports.io/football/teams/$teamId.png"
}
