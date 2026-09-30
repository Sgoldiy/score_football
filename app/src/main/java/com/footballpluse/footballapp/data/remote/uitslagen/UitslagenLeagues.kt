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
        187 to "SwitzerlandSuperLeague"       // fixtures_v2_small 103 KB
    )

    /** Resolve the footapi key for a legacy league id, or null if unknown. */
    fun keyFor(leagueId: Int): String? = BY_ID[leagueId]
}
