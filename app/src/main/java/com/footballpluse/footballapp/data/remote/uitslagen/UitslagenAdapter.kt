package com.footballpluse.footballapp.data.remote.uitslagen

import com.footballpluse.footballapp.data.model.ApiCountry
import com.footballpluse.footballapp.data.model.ApiEvent
import com.footballpluse.footballapp.data.model.ApiLeague
import com.footballpluse.footballapp.data.model.ApiLineupResponse
import com.footballpluse.footballapp.data.model.ApiMatchStatistic
import com.footballpluse.footballapp.data.model.ApiMatchStatisticsResponse
import com.footballpluse.footballapp.data.model.ApiOdd
import com.footballpluse.footballapp.data.model.ApiPlayer
import com.footballpluse.footballapp.data.model.ApiPrediction
import com.footballpluse.footballapp.data.model.ApiStanding
import com.footballpluse.footballapp.data.model.ApiTeam
import com.footballpluse.footballapp.data.model.ApiTopScorer
import com.footballpluse.footballapp.data.model.ApiH2HResponse
import com.footballpluse.footballapp.data.model.StatisticValue
import com.footballpluse.footballapp.data.remote.ApiService
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Serves the whole legacy [ApiService] surface from the open
 * uitslagen.live/footapi upstream. Real values only: live scores, day feeds,
 * league blocks (fixtures + table + top scorers), match detail with events,
 * lineups and venue, team/player pages and global search.
 *
 * Predictions and odds do not exist upstream and return empty lists — the
 * corresponding UI was removed with the FootballCharts layer.
 */
@Singleton
class UitslagenAdapter @Inject constructor(
    private val api: UitslagenApiService
) : ApiService {

    companion object {
        private val LIVE_TTL_MS = 60_000L
        private val DAY_TTL_MS = 5 * 60_000L
        private val BLOCK_TTL_MS = 30 * 60_000L
        private val DETAIL_TTL_MS = 2 * 60_000L
    }

    private val gate = Semaphore(4)
    private val cache = ConcurrentHashMap<String, Pair<Long, Any>>()

    private suspend fun <T> gated(block: suspend () -> T): T = gate.withPermit { block() }

    @Suppress("UNCHECKED_CAST")
    private suspend fun <T> cached(key: String, ttlMs: Long, fetch: suspend () -> T): T {
        val hit = cache[key]
        if (hit != null && System.currentTimeMillis() - hit.first < ttlMs) return hit.second as T
        val fresh = gated { fetch() }
        cache[key] = System.currentTimeMillis() to (fresh as Any)
        return fresh
    }

    private fun clearCache() = cache.clear()

    // ─── Countries & leagues (discovered from the live day feed) ──────────────

    override suspend fun getCountries(): List<ApiCountry> = try {
        dayFeed().mapNotNull { it.country }.distinct().sorted().map {
            ApiCountry(country_id = it.lowercase().replace(" ", "-"), country_name = it, country_logo = null)
        }
    } catch (_: Exception) { emptyList() }

    override suspend fun getLeagues(countryId: String?): List<ApiLeague> = try {
        val wanted = countryId?.let { c ->
            dayFeed().firstOrNull {
                it.country.equals(c, true) || it.country?.lowercase()?.replace(" ", "-") == c
            }?.country
        }
        dayFeed().flatMap { c ->
            c.leagues.orEmpty().mapNotNull { lg ->
                val id = lg.matches?.firstOrNull()?.leagueid ?: return@mapNotNull null
                val country = c.country
                if (country == null || (wanted != null && country != wanted)) return@mapNotNull null
                ApiLeague(
                    country_id = country.lowercase().replace(" ", "-"),
                    country_name = country,
                    league_id = id,
                    league_name = lg.league,
                    league_season = lg.matches.firstOrNull()?.season,
                    league_logo = null,
                    country_logo = null
                )
            }
        }.distinctBy { it.league_id }
    } catch (_: Exception) { emptyList() }

    // ─── Teams & players ──────────────────────────────────────────────────────

    override suspend fun getTeams(leagueId: String?, teamId: String?): List<ApiTeam> = try {
        when {
            !teamId.isNullOrBlank() -> {
                val page = cached("team|$teamId", BLOCK_TTL_MS) { api.teamPage(teamId) }
                listOf(UitslagenMapper.teamPageToApiTeam(page))
            }
            !leagueId.isNullOrBlank() -> {
                val key = UitslagenLeagues.keyFor(leagueId.toIntOrNull() ?: 0) ?: return emptyList()
                blockToTeams(key, leagueId)
            }
            else -> emptyList()
        }
    } catch (_: Exception) { emptyList() }

    private suspend fun blockToTeams(key: String, leagueId: String): List<ApiTeam> {
        val block = cached("block|$key", BLOCK_TTL_MS) { api.fixturesV2Small(key) }
        return UitslagenMapper.blockToTeams(block, leagueId)
    }

    override suspend fun getPlayers(playerId: String?, playerName: String?): List<ApiPlayer> = try {
        when {
            !playerId.isNullOrBlank() -> listOf(
                UitslagenMapper.playerPageToApiPlayer(cached("player|$playerId", BLOCK_TTL_MS) { api.playerPage(playerId) })
            )
            !playerName.isNullOrBlank() ->
                UitslagenMapper.searchPlayers(api.search(playerName))
            else -> emptyList()
        }
    } catch (_: Exception) { emptyList() }

    // ─── Standings (real table from the league block) ─────────────────────────

    override suspend fun getStandings(leagueId: String): List<ApiStanding> = try {
        val key = UitslagenLeagues.keyFor(leagueId.toIntOrNull() ?: 0) ?: return emptyList()
        val block = cached("block|$key", BLOCK_TTL_MS) { api.fixturesV2Small(key) }
        UitslagenMapper.tableToStandings(block?.table, leagueId, block?.table?.leaguename)
    } catch (_: Exception) { emptyList() }

    // ─── Events ───────────────────────────────────────────────────────────────

    override suspend fun getEvents(
        from: String,
        to: String,
        leagueId: String?,
        teamId: String?,
        matchId: String?
    ): List<ApiEvent> = try {
        when {
            !matchId.isNullOrBlank() -> getEventById(matchId)
            !leagueId.isNullOrBlank() -> leagueEvents(leagueId, from, to, teamId)
            !teamId.isNullOrBlank() -> teamEvents(teamId, from, to)
            else -> popularEvents(from, to)
        }
    } catch (_: Exception) { emptyList() }

    override suspend fun getEventById(matchId: String): List<ApiEvent> = try {
        listOfNotNull(
            UitslagenMapper.detailToApiEvent(cached("detail|$matchId", DETAIL_TTL_MS) { api.matchDetail(matchId) })
        )
    } catch (_: Exception) { emptyList() }

    private suspend fun leagueEvents(leagueId: String, from: String, to: String, teamId: String?): List<ApiEvent> {
        val key = UitslagenLeagues.keyFor(leagueId.toIntOrNull() ?: 0) ?: return emptyList()
        val block = cached("block|$key", BLOCK_TTL_MS) { api.fixturesV2Small(key) }
        return block.fixtures.orEmpty()
            .map { UitslagenMapper.rowToApiEvent(it, leagueIdOverride = leagueId, leagueNameOverride = block.table?.leaguename) }
            .filter { UitslagenMapper.inRangeUtc(it.match_date, from, to) }
            .filter { ev ->
                teamId == null || ev.match_hometeam_id == teamId || ev.match_awayteam_id == teamId
            }
    }

    private suspend fun teamEvents(teamId: String, from: String, to: String): List<ApiEvent> {
        val page = cached("team|$teamId", BLOCK_TTL_MS) { api.teamPage(teamId) }
        return page.fixtures.orEmpty()
            .map { UitslagenMapper.rowToApiEvent(it) }
            .filter { UitslagenMapper.inRangeUtc(it.match_date, from, to) }
    }

    private suspend fun popularEvents(from: String, to: String): List<ApiEvent> {
        val day = dayFeedEvents()
        val live = try { liveFeedEvents() } catch (_: Exception) { emptyList() }
        return (live + day)
            .filter { UitslagenMapper.inRangeUtc(it.match_date, from, to) }
            .distinctBy { it.match_id }
    }

    // ─── Livescore (real live rows from feed_livenow) ─────────────────────────

    override suspend fun getLivescore(matchId: String?, leagueId: String?): List<ApiEvent> = try {
        val rows = liveFeedEvents()
        val live = rows.filter { it.match_live == "1" }
        if (matchId.isNullOrBlank() && leagueId.isNullOrBlank()) {
            live
        } else {
            live.filter { ev ->
                (matchId == null || ev.match_id == matchId) &&
                    (leagueId == null || ev.league_id == leagueId)
            }
        }
    } catch (_: Exception) { emptyList() }

    // ─── Head to head (team pages' fixture lists) ─────────────────────────────

    override suspend fun getHeadToHead(firstTeamId: String, secondTeamId: String): ApiH2HResponse = try {
        coroutineScope {
            val aDeferred = async { cached("team|$firstTeamId", BLOCK_TTL_MS) { api.teamPage(firstTeamId) } }
            val bDeferred = async { cached("team|$secondTeamId", BLOCK_TTL_MS) { api.teamPage(secondTeamId) } }
            val a = aDeferred.await()
            val b = bDeferred.await()
            val bName = b.teamname
            val bId = b.id_gs
            val aRows = a.fixtures.orEmpty().map { UitslagenMapper.rowToApiEvent(it) }
            val bRows = b.fixtures.orEmpty().map { UitslagenMapper.rowToApiEvent(it) }
            val mutual = aRows.filter { ev ->
                // Authoritative: numeric team ids come from the same payload
                // family, so equal ids are a certain mutual meeting.
                (ev.match_hometeam_id != null && ev.match_hometeam_id == bId) ||
                    (ev.match_awayteam_id != null && ev.match_awayteam_id == bId) ||
                    // Fallback for rows without ids: normalized display name.
                    UitslagenMapper.normalize(ev.match_hometeam_name) == UitslagenMapper.normalize(bName) ||
                    UitslagenMapper.normalize(ev.match_awayteam_name) == UitslagenMapper.normalize(bName)
            }
            ApiH2HResponse(
                firstTeam_lastResults = aRows.distinctBy { it.match_id }.take(10),
                secondTeam_lastResults = bRows.distinctBy { it.match_id }.take(10),
                firstTeam_VS_secondTeam = mutual.distinctBy { it.match_id }.take(10)
            )
        }
    } catch (_: Exception) { ApiH2HResponse(null, null, null) }

    // ─── Lineups & statistics (from match detail) ─────────────────────────────

    override suspend fun getLineups(matchId: String): Map<String, ApiLineupResponse> = try {
        val detail = cached("detail|$matchId", DETAIL_TTL_MS) { api.matchDetail(matchId) }
        val wrapper = UitslagenMapper.lineupWrapper(detail.lineups) ?: return emptyMap()
        mapOf("lineup" to ApiLineupResponse(wrapper))
    } catch (_: Exception) { emptyMap() }

    override suspend fun getMatchStatistics(matchId: String): Map<String, ApiMatchStatisticsResponse> = try {
        val detail = cached("detail|$matchId", DETAIL_TTL_MS) { api.matchDetail(matchId) }
        val s = detail.stats ?: return emptyMap()
        val stats = listOfNotNull(
            s.total_localteam_won?.let { w ->
                ApiMatchStatistic(
                    type = "H2H Wins",
                    home = StatisticValue(display = w.toString(), numeric = w.toFloat()),
                    away = s.total_visitorteam_won?.let { v -> StatisticValue(v.toString(), v.toFloat()) }
                )
            },
            s.total_draws?.let { d ->
                ApiMatchStatistic(
                    type = "H2H Draws",
                    home = StatisticValue(d.toString(), d.toFloat()),
                    away = StatisticValue(d.toString(), d.toFloat())
                )
            },
            s.total_localteam_scored?.let { g ->
                ApiMatchStatistic(
                    type = "H2H Goals",
                    home = StatisticValue(g.toString(), g.toFloat()),
                    away = s.total_visitorteam_scored?.let { v -> StatisticValue(v.toString(), v.toFloat()) }
                )
            }
        )
        if (stats.isEmpty()) emptyMap()
        else mapOf("statistics" to ApiMatchStatisticsResponse(statistics = stats, player_statistics = null, statistics_1half = null))
    } catch (_: Exception) { emptyMap() }

    // ─── Not available upstream (UI removed) ──────────────────────────────────

    override suspend fun getTopScorers(leagueId: String): List<ApiTopScorer> = try {
        val key = UitslagenLeagues.keyFor(leagueId.toIntOrNull() ?: 0) ?: return emptyList()
        val block = cached("block|$key", BLOCK_TTL_MS) { api.fixturesV2Small(key) }
        UitslagenMapper.topScorers(block)
    } catch (_: Exception) { emptyList() }

    override suspend fun getOdds(from: String?, to: String?, matchId: String?): List<ApiOdd> = emptyList()

    override suspend fun getPredictions(matchId: String): List<ApiPrediction> = emptyList()

    // ─── Feed helpers ─────────────────────────────────────────────────────────

    private suspend fun liveFeed(): List<UitslagenCountryFeed> =
        cached("live", LIVE_TTL_MS) { api.feedLivenow() }

    private suspend fun liveFeedEvents(): List<ApiEvent> =
        UitslagenMapper.flattenFeeds(liveFeed())

    private suspend fun dayFeed(): List<UitslagenCountryFeed> =
        cached("day|${todayUtc()}", DAY_TTL_MS) { api.feedAggregated(todayUtc()) }

    private suspend fun dayFeedEvents(): List<ApiEvent> =
        UitslagenMapper.flattenFeeds(dayFeed())

    private fun todayUtc(): String =
        SimpleDateFormat("dd/MM/yyyy", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
            .format(Date())
}
