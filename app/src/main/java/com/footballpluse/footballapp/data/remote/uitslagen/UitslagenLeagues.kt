package com.footballpluse.footballapp.data.remote.uitslagen

/**
 * Maps the app's legacy league ids (used by HomeScreen, StatsViewModel,
 * HomeViewModel and screens) to footapi league keys.
 *
 * This is a deliberately static table. An earlier version tried to
 * "self-heal" unknown ids from feed rows (`leagueid` + `leagueKey`), but the
 * captured day feed proves footapi's numeric `leagueid` is a different
 * numbering system from the app's legacy ids (1079, 1203, 1272... vs 152,
 * 302, 207...) and is not even unique (1201 spans four leagues), so such a
 * bridge can never fire. Unknown ids resolve to null and the adapter
 * honestly serves an empty league block for them.
 *
 * Key shape: `CountryNameLeagueName`, no spaces (see docs/LIVE_FEED.md).
 * Every entry below was verified live on 2026-09-29 (HTTP 200 with a real
 * league block), either directly via `fixtures_v2/{key}_small.json` or via
 * the `leagueKey` embedded in `search_v3` team results.
 */
object UitslagenLeagues {

    /** Legacy id -> league key. All entries live-verified 2026-09-29. */
    val BY_ID: Map<Int, String> = mapOf(
        152 to "EnglandPremierLeague",        // fixtures_v2_small 260 KB
        302 to "SpainPrimeraDivision",        // search_v3 "Real Madrid" (was wrongly "SpainLaLiga")
        207 to "ItalySerieA",                 // fixtures_v2_small 240 KB
        175 to "GermanyBundesliga",           // fixtures_v2_small 211 KB
        168 to "FranceLigue1",                // fixtures_v2_small 195 KB
        88 to "NetherlandsEredivisie",        // fixtures_v2 + search_v3 "PSV"
        94 to "PortugalPrimeiraLiga",         // search_v3 "Benfica" (was wrongly "PortugalLigaPortugal")
        203 to "SaudiArabiaProLeague",        // search_v3 "Al Hilal" (was wrongly "SaudiArabiaSaudiProfessionalLeague")
        144 to "BelgiumProLeague",            // search_v3 "Club Brugge" (was wrongly "BelgiumJupilerLeague")
        187 to "SwitzerlandSuperLeague",      // fixtures_v2_small 103 KB

        // European & International Tournaments (verified live HTTP 200)
        28 to "EurocupsUEFAChampionsLeague",
        3 to "EurocupsUEFAEuropaLeague",
        848 to "EurocupsUEFAEuropaConferenceLeague",
        5 to "EurocupsUEFANationsLeagueLeagueA",
        1 to "InternationalWorldWorldCup",
        4 to "InternationalEuroEuroChampionship",
        9 to "InternationalAmericaCopaAmerica",
        17 to "InternationalAsiaAFCAsianCup"
    )

    /** Resolve the footapi key for a legacy league id, or null if unknown. */
    fun keyFor(leagueId: Int): String? = BY_ID[leagueId]

    /** Resolve the legacy league id for a footapi league key, or null if unknown. */
    fun idForKey(key: String): Int? = BY_ID.entries.firstOrNull { it.value == key }?.key

    /** Canonical league display names for known legacy IDs. */
    fun leagueNameFor(leagueId: Int?): String? = when (leagueId) {
        152 -> "Premier League"
        302 -> "La Liga"
        207 -> "Serie A"
        175 -> "Bundesliga"
        168 -> "Ligue 1"
        88 -> "Eredivisie"
        94 -> "Primeira Liga"
        203 -> "Saudi Pro League"
        144 -> "Pro League"
        187 -> "Super League"
        28 -> "UEFA Champions League"
        3 -> "UEFA Europa League"
        848 -> "UEFA Europa Conference League"
        5 -> "UEFA Nations League"
        1 -> "World Cup"
        4 -> "Euro Championship"
        9 -> "Copa America"
        17 -> "AFC Asian Cup"
        else -> null
    }

    /**
     * Resolves the canonical country name from A to Z based on league ID, league name,
     * and raw country/filegroup string.
     */
    fun countryFor(leagueId: Int?, leagueName: String? = null, rawCountry: String? = null): String {
        // 1. Direct ID mapping for authoritative top competitions
        val idCountry = when (leagueId) {
            152, 153, 154, 155 -> "England"
            302, 303, 300 -> "Spain"
            207, 208, 205 -> "Italy"
            175, 176, 173 -> "Germany"
            168, 169, 166 -> "France"
            88, 89, 90 -> "Netherlands"
            94, 95 -> "Portugal"
            203 -> "Saudi Arabia"
            144 -> "Belgium"
            187 -> "Switzerland"
            179 -> "Scotland"
            322 -> "Turkey"
            197 -> "Greece"
            181 -> "Austria"
            210 -> "Croatia"
            119 -> "Denmark"
            106 -> "Poland"
            113 -> "Sweden"
            103 -> "Norway"
            2556, 18933, 19064, 19091 -> "India"
            253 -> "USA"
            262 -> "Mexico"
            71 -> "Brazil"
            1076, 1077, 1078, 1079, 1080, 1081, 2129 -> "Argentina"
            188 -> "Australia"
            98, 1272 -> "Japan"
            28, 3, 848, 5, 6 -> "Europe"
            1, 15, 1535 -> "World"
            4 -> "Europe"
            9 -> "South America"
            17 -> "Asia"
            12 -> "Africa"
            else -> null
        }
        if (idCountry != null) return idCountry

        // 2. League name inference A-Z
        val lName = leagueName?.trim()?.lowercase(java.util.Locale.US).orEmpty()
        when {
            lName.contains("premier league") || lName.contains("championship") || lName.contains("fa cup") || lName.contains("efl cup") -> return "England"
            lName.contains("la liga") || lName.contains("primera division") || lName.contains("segunda division") || lName.contains("copa del rey") -> return "Spain"
            lName.contains("serie a") || lName.contains("serie b") || lName.contains("coppa italia") -> return "Italy"
            lName.contains("bundesliga") -> return if (lName.contains("austria")) "Austria" else "Germany"
            lName.contains("ligue 1") || lName.contains("ligue 2") || lName.contains("coupe de france") -> return "France"
            lName.contains("eredivisie") || lName.contains("eerste divisie") || lName.contains("knvb") -> return "Netherlands"
            lName.contains("primeira liga") || lName.contains("liga portugal") || lName.contains("taca de portugal") -> return "Portugal"
            lName.contains("saudi") -> return "Saudi Arabia"
            lName.contains("belgian") || (lName.contains("pro league") && (rawCountry?.contains("belgium", true) == true || leagueId == 144)) -> return "Belgium"
            lName.contains("swiss") || (lName.contains("super league") && rawCountry?.contains("switzerland", true) == true) -> return "Switzerland"
            lName.contains("scottish") || lName.contains("premiership") -> return "Scotland"
            lName.contains("super lig") -> return "Turkey"
            lName.contains("indian super league") || lName.contains("ifa shield") || lName.contains("aiff") -> return "India"
            lName.contains("j-league") || lName.contains("j1 league") || lName.contains("nabisco") -> return "Japan"
            lName.contains("major league soccer") || lName.contains("mls") -> return "USA"
            lName.contains("liga mx") -> return "Mexico"
            lName.contains("brasileir") -> return "Brazil"
            lName.contains("a-league") -> return "Australia"
            lName.contains("champions league") || lName.contains("europa league") || lName.contains("conference league") || lName.contains("nations league") -> return "Europe"
            lName.contains("world cup") || lName.contains("friendlies") -> return "World"
            lName.contains("copa america") || lName.contains("libertadores") || lName.contains("sudamericana") -> return "South America"
            lName.contains("asian cup") -> return "Asia"
            lName.contains("africa cup") || lName.contains("afcon") -> return "Africa"
        }

        // 3. Raw country normalization
        val raw = rawCountry?.trim().orEmpty()
        return when (raw.lowercase(java.util.Locale.US)) {
            "eurocups", "euro cups", "europe", "uefa" -> "Europe"
            "international cups", "international", "world" -> "World"
            "saudiarabia", "saudi arabia" -> "Saudi Arabia"
            "southamerica", "south america", "conmebol" -> "South America"
            "northamerica", "north america", "concacaf" -> "North America"
            "asia", "afc" -> "Asia"
            "africa", "caf" -> "Africa"
            "oceania", "ofc" -> "Oceania"
            "england" -> "England"
            "spain" -> "Spain"
            "italy" -> "Italy"
            "germany" -> "Germany"
            "france" -> "France"
            "netherlands", "holland" -> "Netherlands"
            "portugal" -> "Portugal"
            "belgium" -> "Belgium"
            "switzerland" -> "Switzerland"
            "argentina" -> "Argentina"
            "brazil" -> "Brazil"
            "japan" -> "Japan"
            "india" -> "India"
            "usa", "united states" -> "USA"
            "mexico" -> "Mexico"
            "scotland" -> "Scotland"
            "turkey" -> "Turkey"
            "greece" -> "Greece"
            "austria" -> "Austria"
            "croatia" -> "Croatia"
            "denmark" -> "Denmark"
            "poland" -> "Poland"
            "sweden" -> "Sweden"
            "norway" -> "Norway"
            "australia" -> "Australia"
            "" -> "World"
            else -> raw.replace(Regex("([a-z])([A-Z])"), "$1 $2").trim()
        }
    }

    /**
     * Resolves the official logo/flag URL from the upstream API for a given league/tournament.
     */
    fun logoUrlFor(leagueId: Int?, leagueName: String? = null, country: String? = null): String? {
        val canonicalCountry = countryFor(leagueId, leagueName, country)
        if (leagueId == null || leagueId <= 0) {
            return UitslagenApiService.flagUrl(canonicalCountry)
        }
        return when (leagueId) {
            28, 3, 848 -> "https://uitslagen.live/footapi/images/flags/eurocups.png"
            5 -> "https://uitslagen.live/footapi/images/flags/europe.png"
            1 -> "https://uitslagen.live/footapi/images/flags/world-cup.png"
            4 -> "https://uitslagen.live/footapi/images/flags/euro2024.png"
            9 -> "https://uitslagen.live/footapi/images/flags/south-america.png"
            17 -> "https://uitslagen.live/footapi/images/flags/asia.png"
            152 -> "https://uitslagen.live/footapi/images/flags/england.png"
            302 -> "https://uitslagen.live/footapi/images/flags/spain.png"
            207 -> "https://uitslagen.live/footapi/images/flags/italy.png"
            175 -> "https://uitslagen.live/footapi/images/flags/germany.png"
            168 -> "https://uitslagen.live/footapi/images/flags/france.png"
            88 -> "https://uitslagen.live/footapi/images/flags/netherlands.png"
            94 -> "https://uitslagen.live/footapi/images/flags/portugal.png"
            144 -> "https://uitslagen.live/footapi/images/flags/belgium.png"
            203 -> "https://uitslagen.live/footapi/images/flags/saudi-arabia.png"
            187 -> "https://uitslagen.live/footapi/images/flags/switzerland.png"
            else -> UitslagenApiService.flagUrl(canonicalCountry)
        }
    }
}
