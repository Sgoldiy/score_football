package com.footballpluse.footballapp.data.remote.uitslagen

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

/**
 * Unit tests for [UitslagenStatus] against evidence captured live on
 * 2026-09-29 (see app/src/test/resources/uitslagen/ and docs/LIVE_FEED.md).
 */
class UitslagenStatusTest {

    private fun utcMs(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        cal.clear()
        cal.set(year, month - 1, day, hour, minute, 0)
        return cal.timeInMillis
    }

    // ---- scheduled ------------------------------------------------------------

    @Test
    fun `kickoff time as status decodes to scheduled`() {
        val p = UitslagenStatus.decode("18:45", kickoffUtcMs = null, nowUtcMs = utcMs(2026, 9, 29, 10, 0))
        assertFalse(p.isLive)
        assertFalse(p.isFinished)
        assertTrue(p.isScheduled)
        assertEquals("Not Started", p.label)
    }

    @Test
    fun `literal Not Started decodes to scheduled`() {
        val p = UitslagenStatus.decode("Not Started", null, 0L)
        assertTrue(p.isScheduled)
        assertEquals("Not Started", p.label)
    }

    // ---- literal finished / cancelled ------------------------------------------

    @Test
    fun `FT AET and AP all decode to finished`() {
        for (raw in listOf("FT", "AET", "AP")) {
            val p = UitslagenStatus.decode(raw, null, 0L)
            assertTrue(raw, p.isFinished)
            assertEquals("FT", p.label)
        }
    }

    @Test
    fun `Postp and Cancelled decode to their own labels`() {
        assertEquals("Postponed", UitslagenStatus.decode("Postp.", null, 0L).label)
        assertEquals("Cancelled", UitslagenStatus.decode("Cancelled", null, 0L).label)
        assertFalse(UitslagenStatus.decode("Postp.", null, 0L).isFinished)
    }

    // ---- numeric codes with kickoff evidence (from the 2026-09-29 captures) ----

    @Test
    fun `match at 9h00 with code 57 is in play at 10h21 with minute 70`() {
        // Capture evidence: code 57, goal at 49', injurytime 2/3, observed ~81' elapsed.
        val kickoff = utcMs(2026, 9, 29, 9, 0)
        val now = utcMs(2026, 9, 29, 10, 10) // 70 min elapsed
        val p = UitslagenStatus.decode("57", kickoff, now)
        assertTrue(p.isLive)
        assertEquals("In Play", p.label)
        assertEquals(55, p.minute) // after the break the display convention is elapsed - 15
    }

    @Test
    fun `second half at elapsed 90 reads minute 75`() {
        // France U20 v England U20 (code 43 at 51' elapsed) proves wall-clock
        // cannot separate 1H stoppage from the HT break; we pin a 2H case instead.
        val kickoff = utcMs(2026, 9, 29, 9, 30)
        val now = utcMs(2026, 9, 29, 11, 0)
        val p = UitslagenStatus.decode("43", kickoff, now)
        assertTrue(p.isLive)
        assertEquals(75, p.minute)
    }

    @Test
    fun `same match showed pre-goal code 3 earlier the same day`() {
        // Earlier capture: code 3 at 0 - 0 before the 49th-minute goal.
        val kickoff = utcMs(2026, 9, 29, 9, 0)
        val now = utcMs(2026, 9, 29, 9, 30)
        val p = UitslagenStatus.decode("3", kickoff, now)
        assertTrue(p.isLive)
        assertEquals(30, p.minute)
    }

    // ---- numeric codes without kickoff ------------------------------------------

    @Test
    fun `numeric code without kickoff falls back to live unknown`() {
        val p = UitslagenStatus.decode("57", null, 0L)
        assertTrue(p.isLive)
        assertEquals("In Play", p.label)
        assertNull(p.minute)
    }

    // ---- derived phases ---------------------------------------------------------

    @Test
    fun `elapsed 46 minutes reads as half time`() {
        val kickoff = utcMs(2026, 9, 29, 10, 0)
        val now = utcMs(2026, 9, 29, 10, 46)
        val p = UitslagenStatus.fromElapsed(kickoff, now)
        assertEquals("HT", p.label)
        assertTrue(p.isLive)
    }

    @Test
    fun `elapsed 112 minutes reads as FT`() {
        val kickoff = utcMs(2026, 9, 29, 10, 0)
        val now = utcMs(2026, 9, 29, 11, 52) // 112 min elapsed: past the 105' + grace window
        val p = UitslagenStatus.fromElapsed(kickoff, now)
        assertTrue(p.isFinished)
        assertEquals("FT", p.label)
    }

    @Test
    fun `elapsed 101 minutes still reads in play with minute 86`() {
        val kickoff = utcMs(2026, 9, 29, 10, 0)
        val now = utcMs(2026, 9, 29, 11, 41) // 101 min elapsed: inside second half + grace
        val p = UitslagenStatus.fromElapsed(kickoff, now)
        assertTrue(p.isLive)
        assertEquals(86, p.minute)
    }

    @Test
    fun `elapsed before kickoff reads as not started`() {
        val kickoff = utcMs(2026, 9, 29, 10, 0)
        val now = utcMs(2026, 9, 29, 9, 59)
        val p = UitslagenStatus.fromElapsed(kickoff, now)
        assertTrue(p.isScheduled)
        assertFalse(p.isLive)
    }

    // ---- parsing helpers ---------------------------------------------------------

    @Test
    fun `kickoff parsing treats feed times as UTC`() {
        val ms = UitslagenStatus.kickoffUtcMillis("29/09/2026", "09:00")
        assertEquals(utcMs(2026, 9, 29, 9, 0), ms)
    }

    @Test
    fun `kickoff parsing rejects malformed input`() {
        assertNull(UitslagenStatus.kickoffUtcMillis("2026-09-29", "09:00"))
        assertNull(UitslagenStatus.kickoffUtcMillis("29/09/2026", "9h"))
        assertNull(UitslagenStatus.kickoffUtcMillis(null, null))
    }

    @Test
    fun `score parsing handles real cells and placeholders`() {
        assertEquals(1 to 0, UitslagenStatus.parseScore("1 - 0"))
        assertEquals(0 to 3, UitslagenStatus.parseScore("0 - 3"))
        assertEquals(2 to 2, UitslagenStatus.parseScore("2-2"))
        assertNull(UitslagenStatus.parseScore(" - "))
        assertNull(UitslagenStatus.parseScore(""))
        assertNull(UitslagenStatus.parseScore(null))
    }

    @Test
    fun `base id strips fixture suffix`() {
        assertEquals("3949779", UitslagenStatus.baseId("3949779_f"))
        assertEquals("123", UitslagenStatus.baseId("123"))
        assertNull(UitslagenStatus.baseId(""))
        assertNull(UitslagenStatus.baseId(null))
    }

    @Test
    fun `mayBeLive brackets the realistic playing window`() {
        val kickoff = utcMs(2026, 9, 29, 10, 0)
        assertTrue(UitslagenStatus.mayBeLive(kickoff, utcMs(2026, 9, 29, 11, 0)))
        assertFalse(UitslagenStatus.mayBeLive(kickoff, utcMs(2026, 9, 29, 13, 0)))
        assertFalse(UitslagenStatus.mayBeLive(null, utcMs(2026, 9, 29, 11, 0)))
    }
}
