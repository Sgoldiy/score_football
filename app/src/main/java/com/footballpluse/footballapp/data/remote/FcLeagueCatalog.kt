package com.footballpluse.footballapp.data.remote

import com.footballpluse.footballapp.data.util.SeasonUtils

/**
 * Catalog of every league exposed by the FootballCharts API (generated from
 * GET /leagues/). The app keeps its own stable Int IDs so the rest of the codebase
 * (Room entities, favorites, navigation) keeps working; each ID maps to the FC
 * league slug + available seasons used for real network calls.
 */
data class FcLeague(
    val appId: Int,
    val slug: String,
    val name: String,
    val country: String,
    val seasons: List<String>
) {
    /** Latest (current) season label, e.g. "2026-2027" or "2026". */
    val currentSeason: String get() = seasons.firstOrNull() ?: ""
}

object FcLeagueCatalog {

    /*
     * ID space: leagues the app already persisted under the previous provider
     * keep their legacy ids (152, 302, 207, 175, 168, 88, 94, 144, 203, 187) so
     * DataStore favourites, Room rows and UI constants keep resolving. Every other
     * league keeps a generated id; one generated id (sweden2) collided with a
     * legacy id and was moved to 300+ to keep the space collision-free.
     */

    val ALL: List<FcLeague> = listOf(
        FcLeague(1, "algir1", "Ligue 1", "Algeria", listOf("2026-2027", "2025-2026")),
        FcLeague(2, "australia", "A-League", "Australia", listOf("2025-2026", "2024-2025")),
        FcLeague(3, "austria2", "2. Liga", "Austria", listOf("2026-2027", "2025-2026")),
        FcLeague(4, "austria1", "Bundesliga", "Austria", listOf("2026-2027", "2025-2026")),
        FcLeague(5, "azer", "Premier League", "Azerbaijan", listOf("2026-2027", "2025-2026")),
        FcLeague(144, "belgium1", "Jupiler Pro League", "Belgium", listOf("2026-2027", "2025-2026")),
        FcLeague(7, "wbelgium1", "Super League Women", "Belgium", listOf("2026-2027", "2025-2026")),
        FcLeague(8, "brazil1", "Serie A Betano", "Brazil", listOf("2026", "2025")),
        FcLeague(9, "brazil2", "Serie B", "Brazil", listOf("2026", "2025")),
        FcLeague(10, "bulgaria1", "efbet League", "Bulgaria", listOf("2026-2027", "2025-2026")),
        FcLeague(11, "china", "Super League", "China", listOf("2026", "2025")),
        FcLeague(12, "czech3a", "3. CFL - Group A", "Czech Republic", listOf("2026-2027", "2025-2026")),
        FcLeague(13, "czech3b", "3. CFL - Group B", "Czech Republic", listOf("2026-2027", "2025-2026")),
        FcLeague(14, "czech1", "Chance Liga", "Czech Republic", listOf("2026-2027", "2025-2026")),
        FcLeague(15, "czech2", "ChNL", "Czech Republic", listOf("2026-2027", "2025-2026")),
        FcLeague(16, "denmark2", "1st Division", "Denmark", listOf("2026-2027", "2025-2026")),
        FcLeague(17, "denmark1", "Superliga", "Denmark", listOf("2026-2027", "2025-2026")),
        FcLeague(18, "egypt", "Premier League", "Egypt", listOf("2026-2027", "2025-2026")),
        FcLeague(19, "cha", "Championship", "England", listOf("2026-2027", "2025-2026")),
        FcLeague(20, "eng1", "League One", "England", listOf("2026-2027", "2025-2026")),
        FcLeague(21, "eng2", "League Two", "England", listOf("2026-2027", "2025-2026")),
        FcLeague(22, "national", "National League", "England", listOf("2026-2027", "2025-2026")),
        FcLeague(23, "north", "National League North", "England", listOf("2026-2027", "2025-2026")),
        FcLeague(24, "south", "National League South", "England", listOf("2026-2027", "2025-2026")),
        FcLeague(152, "premier", "Premier League", "England", listOf("2026-2027", "2025-2026")),
        FcLeague(26, "develop21", "Professional Development League", "England", listOf("2026-2027")),
        FcLeague(27, "estonia", "Meistriliiga", "Estonia", listOf("2026", "2025")),
        FcLeague(28, "finland1", "Veikkausliiga", "Finland", listOf("2026", "2025")),
        FcLeague(29, "finland2", "Ykkosliiga", "Finland", listOf("2026", "2025")),
        FcLeague(168, "france1", "Ligue 1", "France", listOf("2026-2027", "2025-2026")),
        FcLeague(31, "france2", "Ligue 2", "France", listOf("2026-2027", "2025-2026")),
        FcLeague(32, "france3", "Ligue 3", "France", listOf("2026-2027", "2025-2026")),
        FcLeague(33, "wfrance1", "Premiere Ligue Women", "France", listOf("2026-2027", "2025-2026")),
        FcLeague(34, "germany2", "2. Bundesliga", "Germany", listOf("2026-2027", "2025-2026")),
        FcLeague(35, "germany3", "3. Liga", "Germany", listOf("2026-2027", "2025-2026")),
        FcLeague(175, "germany1", "Bundesliga", "Germany", listOf("2026-2027", "2025-2026")),
        FcLeague(37, "wgermany1", "Bundesliga Women", "Germany", listOf("2026-2027", "2025-2026")),
        FcLeague(38, "germany-north", "Regionalliga North", "Germany", listOf("2026-2027", "2025-2026")),
        FcLeague(39, "greece1", "Super League", "Greece", listOf("2026-2027", "2025-2026")),
        FcLeague(40, "greece2", "Super League 2", "Greece", listOf("2026-2027", "2025-2026")),
        FcLeague(41, "hungary1", "NB I.", "Hungary", listOf("2026-2027", "2025-2026")),
        FcLeague(42, "hungary2", "NB II.", "Hungary", listOf("2026-2027", "2025-2026")),
        FcLeague(43, "india1", "ISL", "India", listOf("2025-2026", "2024-2025")),
        FcLeague(44, "ireland2", "Division 1", "Ireland", listOf("2026", "2025")),
        FcLeague(45, "ireland1", "Premier Division", "Ireland", listOf("2026", "2025")),
        FcLeague(46, "italy19", "Primavera 1", "Italy", listOf("2026-2027", "2025-2026")),
        FcLeague(207, "italy1", "Serie A", "Italy", listOf("2026-2027", "2025-2026")),
        FcLeague(48, "italy2", "Serie B", "Italy", listOf("2026-2027", "2025-2026")),
        FcLeague(49, "italy3a", "Serie C - Group A", "Italy", listOf("2026-2027", "2025-2026")),
        FcLeague(50, "italy3b", "Serie C - Group B", "Italy", listOf("2026-2027", "2025-2026")),
        FcLeague(51, "japan1", "J1 League", "Japan", listOf("2026-2027", "2026")),
        FcLeague(52, "japan2", "J2 League", "Japan", listOf("2025", "2024")),
        FcLeague(53, "kuwait", "Premier League", "Kuwait", listOf("2026-2027", "2025-2026")),
        FcLeague(54, "latvia", "Virsliga", "Latvia", listOf("2026", "2025")),
        FcLeague(55, "lithuania", "TOPLYGA", "Lithuania", listOf("2026", "2025")),
        FcLeague(56, "morocco", "Botola Pro", "Morocco", listOf("2025-2026", "2024-2025")),
        FcLeague(57, "holland2", "Eerste Divisie", "Netherlands", listOf("2026-2027", "2025-2026")),
        FcLeague(88, "holland1", "Eredivisie", "Netherlands", listOf("2026-2027", "2025-2026")),
        FcLeague(59, "holland3", "Tweede Divisie", "Netherlands", listOf("2026-2027", "2025-2026")),
        FcLeague(60, "norway", "Eliteserien", "Norway", listOf("2026", "2025")),
        FcLeague(61, "poland2", "Division 1", "Poland", listOf("2026-2027", "2025-2026")),
        FcLeague(62, "poland1", "Ekstraklasa", "Poland", listOf("2026-2027", "2025-2026")),
        FcLeague(94, "portugal1", "Liga Portugal", "Portugal", listOf("2026-2027", "2025-2026")),
        FcLeague(64, "portugal2", "Liga Portugal 2", "Portugal", listOf("2026-2027", "2025-2026")),
        FcLeague(65, "romania1", "Superliga", "Romania", listOf("2026-2027", "2025-2026")),
        FcLeague(66, "russia", "Premier League", "Russia", listOf("2026-2027", "2025-2026")),
        FcLeague(67, "saudi2", "Division 1", "Saudi Arabia", listOf("2026-2027", "2025-2026")),
        FcLeague(203, "saudi1", "Saudi Professional League", "Saudi Arabia", listOf("2026-2027", "2025-2026")),
        FcLeague(69, "scot-champ", "Championship", "Scotland", listOf("2026-2027", "2025-2026")),
        FcLeague(70, "scot1", "League One", "Scotland", listOf("2026-2027", "2025-2026")),
        FcLeague(71, "scot2", "League Two", "Scotland", listOf("2026-2027", "2025-2026")),
        FcLeague(72, "scot-premier", "Premiership", "Scotland", listOf("2026-2027", "2025-2026")),
        FcLeague(73, "serbia1", "Mozzart Bet Super Liga", "Serbia", listOf("2026-2027", "2025-2026")),
        FcLeague(74, "slovenia1", "Prva liga", "Slovenia", listOf("2026-2027", "2025-2026")),
        FcLeague(75, "korea1", "K League 1", "South Korea", listOf("2026", "2025")),
        FcLeague(76, "korea2", "K League 2", "South Korea", listOf("2026", "2025")),
        FcLeague(302, "spain1", "LaLiga", "Spain", listOf("2026-2027", "2025-2026")),
        FcLeague(78, "spain2", "LaLiga2", "Spain", listOf("2026-2027", "2025-2026")),
        FcLeague(79, "primera1", "Primera RFEF - Group 1", "Spain", listOf("2026-2027", "2025-2026")),
        FcLeague(80, "primera2", "Primera RFEF - Group 2", "Spain", listOf("2026-2027", "2025-2026")),
        FcLeague(81, "segunda1", "Segunda RFEF - Group 1", "Spain", listOf("2026-2027", "2025-2026")),
        FcLeague(82, "segunda2", "Segunda RFEF - Group 2", "Spain", listOf("2026-2027", "2025-2026")),
        FcLeague(83, "segunda3", "Segunda RFEF - Group 3", "Spain", listOf("2026-2027", "2025-2026")),
        FcLeague(84, "segunda4", "Segunda RFEF - Group 4", "Spain", listOf("2026-2027", "2025-2026")),
        FcLeague(85, "segunda5", "Segunda RFEF - Group 5", "Spain", listOf("2026-2027", "2025-2026")),
        FcLeague(86, "sweden1", "Allsvenskan", "Sweden", listOf("2026", "2025")),
        FcLeague(87, "wsweden", "Allsvenskan Women", "Sweden", listOf("2026", "2025")),
        FcLeague(300, "sweden2", "Superettan", "Sweden", listOf("2026", "2025")),
        FcLeague(89, "swiss2", "Challenge League", "Switzerland", listOf("2026-2027", "2025-2026")),
        FcLeague(187, "swiss1", "Super League", "Switzerland", listOf("2026-2027", "2025-2026")),
        FcLeague(91, "thai1", "Thai League 1", "Thailand", listOf("2026-2027", "2025-2026")),
        FcLeague(92, "turkey2", "1. Lig", "Turkey", listOf("2026-2027", "2025-2026")),
        FcLeague(93, "turkey1", "Super Lig", "Turkey", listOf("2026-2027", "2025-2026")),
    )

    val BY_ID: Map<Int, FcLeague> = ALL.associateBy { it.appId }

    /** Legacy ids resolve first so persisted favourites can never be shadowed by catalog appIds. */
    fun byId(id: Int): FcLeague? =
        ALL.firstOrNull { it.slug == LEGACY_SLUG_BY_ID[id] } ?: BY_ID[id]

    private val LEGACY_SLUG_BY_ID: Map<Int, String> = mapOf(
        152 to "premier", 302 to "spain1", 207 to "italy1", 175 to "germany1",
        168 to "france1", 88 to "holland1", 94 to "portugal1",
        144 to "belgium1", 203 to "saudi1", 187 to "swiss1"
    )

    fun bySlug(slug: String): FcLeague? = ALL.firstOrNull { it.slug == slug }

    /** Best-effort lookup by league name (and optionally country). */
    fun findByName(name: String, country: String? = null): FcLeague? {
        val n = name.trim()
        return ALL.firstOrNull { it.name.equals(n, true) && (country == null || it.country.equals(country, true)) }
            ?: ALL.firstOrNull { it.name.equals(n, true) }
            ?: ALL.firstOrNull { it.name.contains(n, true) && (country == null || it.country.equals(country, true)) }
    }

    /**
     * Convert an app season start-year (e.g. 2026) into the FC season label for
     * this league. Calendar-year leagues use "2026" while cross-year leagues use
     * "2026-2027". Falls back to the league's current season when unavailable.
     */
    fun seasonLabel(league: FcLeague, seasonStartYear: Int): String {
        val cross = "%d-%d".format(seasonStartYear, seasonStartYear + 1)
        if (league.seasons.contains(cross)) return cross
        val cal = seasonStartYear.toString()
        if (league.seasons.contains(cal)) return cal
        return league.currentSeason
    }

    /** Season start-year Int from an FC label ("2026-2027" -> 2026, "2026" -> 2026). */
    fun seasonYear(label: String): Int = label.take(4).toIntOrNull() ?: SeasonUtils.currentSeasonStartYear()
}
