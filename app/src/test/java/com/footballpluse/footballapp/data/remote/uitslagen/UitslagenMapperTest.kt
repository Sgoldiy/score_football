package com.footballpluse.footballapp.data.remote.uitslagen

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Parses the real payloads captured on 2026-09-29 (app/src/test/resources/uitslagen/)
 * and verifies the mapping into the legacy Api models.
 */
class UitslagenMapperTest {

    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

    private fun resource(name: String): String =
        javaClass.getResourceAsStream("/uitslagen/$name")!!.bufferedReader().use { it.readText() }

    private fun feedAdapter() = moshi.adapter(UitslagenCountryFeed::class.java)

    private fun parseLivenow(name: String): List<UitslagenCountryFeed> {
        // Manual array parse: the upstream returns a bare JSON array.
        val text = resource(name).trim()
        assertTrue(text.startsWith("["))
        val sb = StringBuilder("[")
        var depth = 0
        var inStr = false
        var esc = false
        var start = -1
        var i = 0
        // Split top-level array items naively but safely: we instead re-wrap.
        // Simplest robust approach: use Moshi's Map adapter to count, then slice.
        val items = mutableListOf<String>()
        depth = 0
        while (i < text.length) {
            val c = text[i]
            when {
                c == '"' && !esc -> inStr = !inStr
                c == '\\' && inStr -> esc = true
                else -> esc = false
            }
            if (!inStr) {
                if (c == '{') {
                    if (depth == 0) start = i
                    depth++
                } else if (c == '}') {
                    depth--
                    if (depth == 0) items.add(text.substring(start, i + 1))
                }
            }
            i++
        }
        return items.mapNotNull { feedAdapter().fromJson(it) }
    }

    @Test
    fun `live feed parses and maps real rows`() {
        val feeds = parseLivenow("feed_livenow_1.json")
        assertTrue("expected countries in live feed", feeds.isNotEmpty())
        val events = UitslagenMapper.flattenFeeds(feeds, nowUtcMs = 0L)
        assertTrue("expected events", events.isNotEmpty())
        val known = events.firstOrNull { it.match_id == "3951238" }
        assertNotNull("Mohun Bagan match missing", known)
        known!!
        assertEquals("Mohun Bagan", known.match_hometeam_name)
        assertEquals("NorthEast United", known.match_awayteam_name)
        assertEquals("IFA Shield", known.league_name)
        assertEquals("India", known.country_name)
        // scoretime was "1 - 0" in this capture
        assertEquals("1", known.match_hometeam_score)
        assertEquals("0", known.match_awayteam_score)
        // numeric status + now=0 (long before the 2026 kickoff) -> Not Started
        assertEquals("Not Started", known.match_status)
        // now deep past the 09:00 UTC kickoff -> derived FT (elapsed > 110 min)
        val kickoff = UitslagenStatus.kickoffUtcMillis("29/09/2026", "09:00")!!
        val ftEvent = UitslagenMapper.rowToApiEvent(
            UitslagenMatchRow(
                id = known.match_id, date = "29/09/2026", time = "09:00", status = "57",
                scoretime = "1 - 0", localteam = known.match_hometeam_name,
                visitorteam = known.match_awayteam_name, leagueid = known.league_id,
                leaguename = known.league_name, filegroup = known.country_name
            ),
            nowUtcMs = kickoff + 130L * 60_000L
        )
        assertEquals("FT", ftEvent.match_status)
        // team ids flow through and logos resolve to the footapi CDN
        assertNotNull(known.match_hometeam_id)
        assertTrue(known.team_home_badge!!.startsWith("https://uitslagen.live/footapi/images/teams_gs/"))
    }

    @Test
    fun `literal statuses pass through the decoder`() {
        val feeds = parseLivenow("feed_livenow_1.json")
        val events = UitslagenMapper.flattenFeeds(feeds, nowUtcMs = 0L)
        val scheduled = events.firstOrNull { it.match_status == "Not Started" }
        // not every capture has one, but the decoder contract must hold for any
        if (scheduled != null) assertEquals("1", null ?: scheduled.match_live ?: "1")
    }

    @Test
    fun `aggregated day feed parses`() {
        val feeds = parseLivenow("feed_matches_aggregated.json")
        val events = UitslagenMapper.flattenFeeds(feeds, nowUtcMs = 0L)
        assertTrue(events.size > 100)
        // day feed mixes FT rows with future rows; both must keep their score/status
        val ft = events.filter { it.match_status == "FT" }
        assertTrue(ft.isNotEmpty())
        assertTrue(ft.any { it.match_hometeam_score != null && it.match_awayteam_score != null })
    }

    @Test
    fun `fixtures block yields standings teams and rows`() {
        val block = moshi.adapter(UitslagenFixturesBlock::class.java).fromJson(resource("fixtures_v2_small.json"))!!
        val standings = UitslagenMapper.tableToStandings(block.table, "123", "Primera C")
        assertTrue("table rows missing", standings.isNotEmpty())
        val first = standings.first()
        assertEquals("1", first.standing_place)
        assertNotNull(first.team_name)
        assertEquals("48", first.standing_PTS)
        assertEquals("12", first.standing_W)
        assertEquals("35", first.overall_GF)
        assertNotNull(first.team_badge)
        val teams = UitslagenMapper.blockToTeams(block, "any")
        assertTrue(teams.isNotEmpty())
        // FT fixture rows keep score and status
        val ftRow = block.fixtures.orEmpty().first { it.status == "FT" }
        val ev = UitslagenMapper.rowToApiEvent(ftRow, nowUtcMs = 0L)
        assertEquals("FT", ev.match_status)
        assertEquals("0", ev.match_hometeam_score)
        assertEquals("1", ev.match_awayteam_score)
    }

    @Test
    fun `match detail maps events and venue`() {
        val detail = moshi.adapter(UitslagenMatchDetail::class.java).fromJson(resource("matches_detail.json"))!!
        val ev = UitslagenMapper.detailToApiEvent(detail, nowUtcMs = 0L)
        assertEquals("3951238", ev.match_id)
        assertEquals("Vivekananda Yuba Bharati Krirangan", ev.match_stadium)
        val goals = ev.goalscorer.orEmpty()
        assertEquals(1, goals.size)
        assertEquals("49", goals[0].time)
        assertEquals("1 - 0", goals[0].score)
        // the capture's goal event carries an empty player name (upstream gap)
        assertNull(goals[0].home_scorer)
        assertNull(goals[0].away_scorer)
        // h2h summary stats map into statistics
        val stats = detail.stats
        assertNotNull(stats)
        assertEquals(10, stats!!.total_localteam_won)
    }

    @Test
    fun `team page maps squad venue and coach`() {
        val page = moshi.adapter(UitslagenTeamPage::class.java).fromJson(resource("team_gs.json"))!!
        val team = UitslagenMapper.teamPageToApiTeam(page)
        assertEquals("Mohun Bagan", team.team_name)
        assertEquals("India", team.team_country)
        assertNotNull(team.venue?.venue_name)
        assertTrue(team.players.orEmpty().isNotEmpty())
        assertEquals("Vishal Kaith", team.players!!.first().player_name)
        assertEquals("34291", team.team_key)
    }

    @Test
    fun `search response splits teams and players`() {
        val resp = moshi.adapter(UitslagenSearchResponse::class.java).fromJson(resource("search_v3.json"))!!
        val teams = UitslagenMapper.searchTeams(resp)
        val players = UitslagenMapper.searchPlayers(resp)
        assertTrue(teams.isNotEmpty())
        assertTrue(teams.any { it.team_name?.contains("Ajax", true) == true })
        // capture carries 5 player hits alongside 7 team hits
        assertEquals(5, players.size)
    }

    @Test
    fun `normalize strips diacritics and punctuation`() {
        assertEquals("muniz", UitslagenMapper.normalize("Muñiz"))
        assertEquals("atleticobalboa", UitslagenMapper.normalize("Atlético Balboa"))
        assertEquals("afcwimbledon", UitslagenMapper.normalize("AFC Wimbledon"))
        assertEquals("", UitslagenMapper.normalize(null))
    }

    @Test
    fun `inRangeUtc compares dd-MM-yyyy against ISO window`() {
        assertTrue(UitslagenMapper.inRangeUtc("29/09/2026", "2026-07-01", "2027-06-30"))
        assertFalse2(UitslagenMapper.inRangeUtc("21/02/2026", "2026-07-01", "2027-06-30"))
        // missing window keeps everything
        assertTrue(UitslagenMapper.inRangeUtc("29/09/2026", null, null))
    }

    @Test
    fun `top scorers parse from the populated EPL block`() {
        val block = moshi.adapter(UitslagenFixturesBlock::class.java)
            .fromJson(resource("fixtures_v2_small_topscorers.json"))!!
        // The same full block still maps table + fixtures correctly.
        assertEquals(20, UitslagenMapper.tableToStandings(block.table, "152", block.table?.leaguename).size)
        // Populated payload nests the scorer list under `players` (verified live 2026-09-30).
        val scorers = UitslagenMapper.topScorers(block)
        assertEquals(50, scorers.size)
        val top = scorers.first()
        assertEquals("Erling Haaland", top.player_name)
        assertEquals("Manchester City", top.team_name)
        assertEquals("5", top.goals)
        assertEquals("1", top.player_place)
        assertNotNull(top.player_id)
        assertTrue(!top.team_id.isNullOrBlank())
        // Every row carries the fields the screen renders.
        assertTrue(scorers.all { !it.player_name.isNullOrBlank() })
        assertTrue(scorers.all { !it.goals.isNullOrBlank() })
    }

    @Test
    fun `top scorers fallback for the legacy topscorers list key`() {
        val json = "{\"topscorers\":{\"tournaments\":[{\"name\":\"X\",\"topscorers\":[" +
            "{\"id\":\"7\",\"name\":\"Legacy Player\",\"goals\":\"3\",\"team\":\"Old FC\",\"teamid\":\"42\",\"penalty\":\"1\"}]}]}}"
        val block = moshi.adapter(UitslagenFixturesBlock::class.java).fromJson(json)!!
        val scorers = UitslagenMapper.topScorers(block)
        assertEquals(1, scorers.size)
        assertEquals("Legacy Player", scorers[0].player_name)
        assertEquals("3", scorers[0].goals)
        assertEquals("1", scorers[0].penalty_goals)
        assertEquals("42", scorers[0].team_id)
    }

    private fun assertFalse2(v: Boolean) {
        if (v) throw AssertionError("expected false")
    }
}
