package com.footballpluse.footballapp.data.remote.uitslagen

/**
 * Maps the app's legacy league ids (used by HomeScreen, StatsViewModel,
 * HomeViewModel and screens) to footapi league keys.
 *
 * Keys follow `CountryNameLeagueName` (no spaces) — e.g. `IndiaIFAShield`,
 * `EnglandConferenceNational`, `ArgentinaPrimCMetro` (capture-proven shapes,
 * docs/LIVE_FEED.md).
 *
 * VERIFIED keys were each fetched live on 2026-09-29 with 200 OK and real
 * payloads. UNVERIFIED entries follow the same construction rule but returned
 * empty blocks at probe time (could be off-season or a wrong guess) — for
 * those the adapter falls back to runtime discovery: the day feed and the
 * live feed both carry `leagueKey` on every row, so the first match row seen
 * for a league registers its real key in [runtimeOverrides].
 */
object UitslagenLeagues {

    /** Legacy id -> league key. Verified entries marked in comments. */
    val BY_ID: Map<Int, String> = mapOf(
        152 to "EnglandPremierLeague",                 // VERIFIED 2026-09-29 (260 KB)
        302 to "SpainLaLiga",                          // unverified (empty at probe; discovery fallback)
        207 to "ItalySerieA",                          // VERIFIED 2026-09-29 (240 KB)
        175 to "GermanyBundesliga",                    // VERIFIED 2026-09-29 (211 KB)
        168 to "FranceLigue1",                         // VERIFIED 2026-09-29 (195 KB)
        88 to "NetherlandsEredivisie",                 // capture-proven shape (NetherlandsEredivisie in search payload)
        94 to "PortugalLigaPortugal",                  // unverified; discovery fallback
        203 to "SaudiArabiaSaudiProfessionalLeague",   // unverified; discovery fallback
        144 to "BelgiumJupilerLeague",                 // unverified; discovery fallback
        187 to "SwitzerlandSuperLeague"                // VERIFIED 2026-09-29 (103 KB)
    )

    /** Reverse lookup for display mapping (country from key prefix). */
    fun keyFor(leagueId: Int): String? = BY_ID[leagueId] ?: runtimeOverrides[leagueId]

    /**
     * Keys learned at runtime from feed rows (leagueId -> leagueKey). The
     * feeds carry `leagueid` + `leagueKey` on every match row, so any league
     * that ever appears in a feed self-registers here.
     */
    private val runtimeOverrides = java.util.concurrent.ConcurrentHashMap<Int, String>()

    /** Register a key discovered in a feed row (id -> key). */
    fun register(leagueId: Int?, key: String?) {
        if (leagueId != null && leagueId > 0 && !key.isNullOrBlank()) {
            runtimeOverrides.putIfAbsent(leagueId, key)
        }
    }

    /** Numeric league id parsed from a feed row's `leagueid` field, if any. */
    fun leagueIdOf(row: UitslagenMatchRow): Int? = row.leagueid?.toIntOrNull()
}
