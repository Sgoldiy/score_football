package com.footballpluse.footballapp.data.remote

import com.footballpluse.footballapp.data.remote.FcLeagueCatalog
import java.util.concurrent.ConcurrentHashMap

/**
 * Session-level registries that bridge the FootballCharts API (which identifies
 * teams by NAME and matches by SLUG) with the app's stable Int ids.
 *
 *  - FcMatchRegistry: numeric match id  ->  the info needed to build/lookup a
 *    match slug for GET /matches/{slug}/.
 *  - FcTeamLeagueIndex: team name  ->  the FC leagues it appears in, so team-scoped
 *    queries (team page, team results) know which league to hit.
 *  - FcLeagueDataCache: short-TTL in-memory copy of results/fixtures per league so
 *    date-window scans (home screen, livescore) don't re-fetch everything.
 */
object FcMatchRegistry {

    data class Snapshot(
        val matchId: Int,
        val leagueSlug: String?,
        val homeTeam: String?,
        val awayTeam: String?,
        val date: String?,
        val slug: String?
    )

    private val byId = ConcurrentHashMap<Int, Snapshot>()

    // Fixture rows carry no numeric id — derive a stable one from the slug so
    // navigation ids stay consistent across launches.
    private val slugToId = ConcurrentHashMap<String, Int>()

    private fun hashOf(key: String): Int {
        var h = 1125899906842597L
        for (c in key) h = 31L * h + c.code
        return (h xor (h ushr 32)).toInt()
    }

    fun idFromSlug(slug: String): Int = slugToId.getOrPut(slug.lowercase().trim()) { hashOf(slug.lowercase().trim()) }

    fun idForSlug(slug: String): String = idFromSlug(slug).toString()

    @Synchronized
    fun put(
        matchId: Long?,
        leagueSlug: String?,
        homeTeam: String?,
        awayTeam: String?,
        date: String?,
        slug: String?
    ) {
        val existingBySlug = slug?.let { s -> byId[idFromSlug(s)] }
        // Fixture rows have no numeric id: derive a stable one from the slug.
        val id = (matchId?.toInt() ?: slug?.let { idFromSlug(it) } ?: 0)
        if (id == 0) return
        val existing = byId[id] ?: existingBySlug
        byId[id] = Snapshot(
            matchId = id,
            leagueSlug = leagueSlug ?: existing?.leagueSlug,
            homeTeam = homeTeam ?: existing?.homeTeam,
            awayTeam = awayTeam ?: existing?.awayTeam,
            date = date ?: existing?.date,
            slug = slug ?: existing?.slug
        )
    }

    fun get(matchId: Int): Snapshot? = byId[matchId]

    /**
     * Deterministic FC match slug. The first segment is the COUNTRY slug, not the
     * league slug — verified live 2026-09-28:
     *   algeria/ligue-1/2026-10-03-kabylie-vs-biskra        (algir1 league)
     *   england/premier-league/2026-09-20-manchester-city-vs-sunderland
     * The league-name segment falls back to the league slug only when the catalog
     * has no country/name for it.
     */
    fun buildSlug(leagueSlug: String, leagueRealName: String, date: String, home: String, away: String): String {
        val lg = FcLeagueCatalog.bySlug(leagueSlug)
        val countrySlug = lg?.country?.let { ApiConfig.teamSlug(it) } ?: leagueSlug
        val nameSegment = ApiConfig.teamSlug(lg?.name ?: leagueRealName) ?: leagueSlug
        return "$countrySlug/$nameSegment/${date}-${ApiConfig.teamSlug(home)}-vs-${ApiConfig.teamSlug(away)}"
    }

    /** Slug for an id that was derived from a slug-only fixture row. */
    fun slugForId(id: Int): String? = byId[id]?.slug

    /** Slug for a snapshot: the explicit one when known, else constructed. */
    fun slugFor(snapshot: Snapshot, leagueRealName: String?): String? {
        snapshot.slug?.let { return it }
        val lg = snapshot.leagueSlug ?: return null
        val date = snapshot.date ?: return null
        val home = snapshot.homeTeam ?: return null
        val away = snapshot.awayTeam ?: return null
        val realName = leagueRealName ?: FcLeagueCatalog.bySlug(lg)?.name ?: lg
        return buildSlug(lg, realName, date, home, away)
    }
}

object FcTeamLeagueIndex {

    private val teamToLeagues = ConcurrentHashMap<String, MutableSet<String>>()

    @Synchronized
    fun register(teamName: String?, leagueSlug: String?) {
        if (teamName.isNullOrBlank() || leagueSlug.isNullOrBlank()) return
        teamToLeagues.getOrPut(teamName.lowercase().trim()) { java.util.Collections.newSetFromMap(ConcurrentHashMap()) }
            .add(leagueSlug)
    }

    fun leaguesOf(teamName: String?): List<String> {
        if (teamName.isNullOrBlank()) return emptyList()
        return teamToLeagues[teamName.lowercase().trim()]?.toList() ?: emptyList()
    }
}

/**
 * Popular leagues scanned for date-based screens (Home "today", livescore).
 * Kept small on purpose: every league costs a results + fixtures call.
 */
object FcPopularLeagues {
    val SLUGS = listOf(
        "premier", "spain1", "germany1", "italy1", "france1",
        "cha", "scot-premier", "turkey1", "holland1", "portugal1",
        "belgium1", "brazil1"
    )
}

/** Short-TTL cache for per-league responses (results, fixtures, team lists). */
class FcLeagueDataCache<T : Any> {

    data class Entry<out V>(val response: V, val fetchedAt: Long)

    private val data = ConcurrentHashMap<String, Entry<T>>()

    suspend fun getOrFetch(
        key: String,
        ttlMs: Long,
        fetch: suspend () -> T
    ): T {
        val now = System.currentTimeMillis()
        data[key]?.let { if (now - it.fetchedAt < ttlMs) return it.response }
        val fresh = try {
            fetch()
        } catch (e: Exception) {
            // Serve stale copy when the backend is unreachable/cold-starting.
            data[key]?.response ?: throw e
        }
        data[key] = Entry(fresh, now)
        return fresh
    }
}
