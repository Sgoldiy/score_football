package com.footballpluse.footballapp.data.remote.uitslagen

import com.footballpluse.footballapp.data.remote.ApiService
import com.footballpluse.footballapp.data.remote.FlexibleJsonAdapters
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Ignore
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

/**
 * LIVE BEHAVIORAL VERIFICATION (ignored by default — hits the real network).
 * Drives the real [ApiService] implementation ([UitslagenAdapter]) against
 * the real upstream exactly as the app's screens do, asserting that each
 * flow returns complete data (no missing fields).
 *
 * Run on demand when verifying against the upstream:
 *   ./gradlew.bat testDebugUnitTest --tests "*.LiveAdapterBehaviorTest"
 *
 * All 9 flows verified green against the live upstream on 2026-09-30
 * (see docs/LIVE_FEED.md for the evidence). Skipped in normal builds.
 */
@Ignore("hits the live uitslagen upstream — run on demand")
class LiveAdapterBehaviorTest {

    private val adapter: ApiService by lazy {
        val moshi = Moshi.Builder()
            .add(FlexibleJsonAdapters())
            .add(KotlinJsonAdapterFactory())
            .build()
        val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                chain.proceed(
                    chain.request().newBuilder()
                        .header("User-Agent", "FootballPulse/1.0")
                        .build()
                )
            }
            .build()
        UitslagenAdapter(
            Retrofit.Builder()
                .baseUrl("https://uitslagen.live/")
                .client(client)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
                .create(UitslagenApiService::class.java)
        )
    }

    private fun isoDay(offsetDays: Int = 0): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(Date(System.currentTimeMillis() + offsetDays * 86_400_000L))

    private fun badgeOk(url: String?) =
        url != null && url.startsWith("https://uitslagen.live/footapi/images/teams_gs/") && url.endsWith(".png")

    // ─── FLOW 1: Home feed (popular events = live + day feed) ────────────────

    @Test
    fun flow1_homeFeed_rowsComplete() = runBlocking {
        val events = adapter.getEvents(from = isoDay(-1), to = isoDay(1))
        println("FLOW1 homeFeed=${events.size}")
        assertTrue("home feed nearly empty: ${events.size}", events.size >= 20)
        val noId = events.count { it.match_id.isNullOrBlank() }
        val noTeams = events.count { it.match_hometeam_name.isNullOrBlank() || it.match_awayteam_name.isNullOrBlank() }
        val noDate = events.count { it.match_date.isNullOrBlank() }
        val noStatus = events.count { it.match_status.isNullOrBlank() }
        val noCountry = events.count { it.country_name.isNullOrBlank() }
        val noLeague = events.count { it.league_name.isNullOrBlank() }
        val badBadges = events.count { !badgeOk(it.team_home_badge) || !badgeOk(it.team_away_badge) }
        println("FLOW1 missing id=$noId teams=$noTeams date=$noDate status=$noStatus country=$noCountry league=$noLeague badge=$badBadges")
        assertEquals("rows without ids", 0, noId)
        assertEquals("rows without teams", 0, noTeams)
        assertEquals("rows without date", 0, noDate)
        assertEquals("rows without status", 0, noStatus)
        assertEquals("rows without country", 0, noCountry)
        assertEquals("rows without league", 0, noLeague)
        assertEquals("rows with broken badge urls", 0, badBadges)
    }

    // ─── FLOW 2: Live scores ──────────────────────────────────────────────────

    @Test
    fun flow2_livescore_saneRows() = runBlocking {
        val live = adapter.getLivescore(matchId = null, leagueId = null)
        println("FLOW2 liveNow=${live.size}")
        live.forEach { ev ->
            assertTrue("live row without match_live flag", ev.match_live == "1")
            assertNotNull("live row without score", ev.match_hometeam_score)
            assertNotNull("live row without score", ev.match_awayteam_score)
            assertTrue("live row without time/status", !ev.match_time.isNullOrBlank() || !ev.match_status.isNullOrBlank())
        }
        // live feed and home feed must agree on live rows (union path)
        val home = adapter.getEvents(from = isoDay(-1), to = isoDay(1))
        val liveIds = live.map { it.match_id }.toSet()
        assertTrue(
            "live rows missing from home feed",
            liveIds.isEmpty() || home.any { it.match_id in liveIds }
        )
    }

    // ─── FLOW 3: LaLiga standings (corrected key) ─────────────────────────────

    @Test
    fun flow3_laliga_standingsComplete() = runBlocking {
        val table = adapter.getStandings("302")
        println("FLOW3 standings=${table.size}")
        assertTrue("LaLiga table nearly empty: ${table.size}", table.size >= 18)
        assertEquals("first row not position 1", "1", table.first().standing_place)
        val blank = table.filter {
            it.team_name.isNullOrBlank() || it.standing_PTS.isNullOrBlank() ||
                it.team_id.isNullOrBlank() || !badgeOk(it.team_badge)
        }
        println("FLOW3 rowsMissingFields=${blank.size} first=${table.first().team_name} pts=${table.first().standing_PTS}")
        assertEquals("table rows with missing fields: $blank", 0, blank.size)
    }

    // ─── FLOW 4: LaLiga events window ─────────────────────────────────────────

    @Test
    fun flow4_laliga_eventsComplete() = runBlocking {
        val events = adapter.getEvents(from = isoDay(-200), to = isoDay(200), leagueId = "302")
        println("FLOW4 laLigaEvents=${events.size}")
        assertTrue("LaLiga league events nearly empty: ${events.size}", events.size >= 5)
        val blank = events.count {
            it.match_id.isNullOrBlank() || it.match_hometeam_name.isNullOrBlank() ||
                it.match_awayteam_name.isNullOrBlank() || it.match_date.isNullOrBlank()
        }
        assertEquals("events with missing core fields", 0, blank)
        Unit
    }

    // ─── FLOW 5: Top scorers (record reality; must not crash) ─────────────────

    @Test
    fun flow5_topScorers_reported() = runBlocking {
        val scorers = adapter.getTopScorers("302")
        println("FLOW5 topScorers=${scorers.size} first=${scorers.firstOrNull()?.let { it.player_name + " " + it.goals }}")
        scorers.forEach {
            assertTrue("scorer without name", !it.player_name.isNullOrBlank())
        }
    }

    // ─── FLOW 6: Match detail + lineups + stats for a finished LaLiga match ───

    @Test
    fun flow6_matchDetail_lineups_stats() = runBlocking {
        val finished = adapter.getEvents(from = isoDay(-30), to = isoDay(-1), leagueId = "302")
            .firstOrNull { it.match_status?.contains("FT", true) == true }
            ?: adapter.getEvents(from = isoDay(-200), to = isoDay(-1), leagueId = "302")
                .firstOrNull { it.match_status?.contains("FT", true) == true }
        assertNotNull("no finished LaLiga match found", finished)
        val id = finished!!.match_id!!
        val detail = adapter.getEventById(id)
        println("FLOW6 detail=${detail.size} status=${detail.firstOrNull()?.match_status} " +
            "score=${detail.firstOrNull()?.match_hometeam_score}-${detail.firstOrNull()?.match_awayteam_score} " +
            "stadium=${detail.firstOrNull()?.match_stadium} ref=${detail.firstOrNull()?.match_referee}")
        assertEquals("detail did not resolve match $id", 1, detail.size)
        val d = detail.first()
        assertEquals("detail status not FT", "FT", d.match_status)
        assertTrue("no home score", !d.match_hometeam_score.isNullOrBlank())
        assertTrue("no away score", !d.match_awayteam_score.isNullOrBlank())

        val lineups = adapter.getLineups(id)
        println("FLOW6 lineups=${lineups.size} " +
            "homePlayers=${lineups["lineup"]?.lineup?.home?.starting_lineups?.size} " +
            "awayPlayers=${lineups["lineup"]?.lineup?.away?.starting_lineups?.size}")
        if (lineups.isNotEmpty()) {
            val w = lineups["lineup"]!!.lineup
            assertTrue("home XI missing", (w.home?.starting_lineups?.size ?: 0) >= 11)
            assertTrue("away XI missing", (w.away?.starting_lineups?.size ?: 0) >= 11)
            val p = w.home?.starting_lineups?.firstOrNull()
            assertTrue("lineup player fields missing", !p?.player.isNullOrBlank() && !p?.player_number.isNullOrBlank())
        } else {
            println("FLOW6-WARN no lineups for this match")
        }

        val stats = adapter.getMatchStatistics(id)
        println("FLOW6 stats=${stats.size} types=${stats["statistics"]?.statistics?.map { it.type }}")
    }

    // ─── FLOW 7: Search (players + team page) ─────────────────────────────────

    @Test
    fun flow7_search_teamPage() = runBlocking {
        val players = adapter.getPlayers(playerName = "Lamine")
        println("FLOW7 searchPlayers=${players.size} first=${players.firstOrNull()?.player_name}")
        assertTrue("player search returned nothing", players.isNotEmpty())
        assertTrue("search result missing name", players.all { !it.player_name.isNullOrBlank() })

        val table = adapter.getStandings("302")
        val teamId = table.first().team_id!!
        val team = adapter.getTeams(teamId = teamId)
        println("FLOW7 teamPage=${team.size} name=${team.firstOrNull()?.team_name} " +
            "venue=${team.firstOrNull()?.venue?.venue_name} badge=${team.firstOrNull()?.team_badge}")
        assertEquals("team page did not resolve id $teamId", 1, team.size)
        assertTrue("team name missing", !team.first().team_name.isNullOrBlank())
        assertTrue("team badge missing", badgeOk(team.first().team_badge))
    }

    // ─── FLOW 8: H2H for a real past pairing ──────────────────────────────────

    @Test
    fun flow8_h2h_mutualMeetingsFound() = runBlocking {
        val pairing = adapter.getEvents(from = isoDay(-200), to = isoDay(-1), leagueId = "302")
            .firstOrNull { it.match_status?.contains("FT", true) == true }
        assertNotNull("no finished pairing for H2H", pairing)
        val h2h = adapter.getHeadToHead(pairing!!.match_hometeam_id!!, pairing.match_awayteam_id!!)
        println("FLOW8 pairing=${pairing.match_hometeam_name} vs ${pairing.match_awayteam_name} " +
            "aLast=${h2h.firstTeam_lastResults?.size} bLast=${h2h.secondTeam_lastResults?.size} " +
            "mutual=${h2h.firstTeam_VS_secondTeam?.size}")
        assertTrue("first team last results empty", !h2h.firstTeam_lastResults.isNullOrEmpty())
        assertTrue("second team last results empty", !h2h.secondTeam_lastResults.isNullOrEmpty())
        assertTrue(
            "mutual meetings not detected by name matching",
            !h2h.firstTeam_VS_secondTeam.isNullOrEmpty()
        )
    }

    // ─── FLOW 9: Countries & leagues discovery ────────────────────────────────

    @Test
    fun flow9_countries_leagues() = runBlocking {
        val countries = adapter.getCountries()
        println("FLOW9 countries=${countries.size} sample=${countries.take(5).map { it.country_name }}")
        assertTrue("countries empty", countries.isNotEmpty())
        assertTrue("country name missing", countries.all { !it.country_name.isNullOrBlank() })
        val leagues = adapter.getLeagues(countries.first().country_name)
        println("FLOW9 leaguesForFirst=${leagues.size} sample=${leagues.take(3).map { it.league_name }}")
        assertTrue("no leagues for a discovered country", leagues.isNotEmpty())
        assertTrue("league name missing", leagues.all { !it.league_name.isNullOrBlank() })
        Unit
    }
}
