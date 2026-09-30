package com.footballpluse.footballapp.data.remote.uitslagen

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pins the static legacy-id -> footapi league-key map.
 *
 * Every key was verified live on 2026-09-29 (see docs/LIVE_FEED.md): four of
 * them were corrected after live probes showed the original guesses returned
 * empty blocks in-season. If an assertion here fails after an edit, a
 * verified key was changed or dropped — re-verify against the live upstream
 * before merging.
 */
class UitslagenLeaguesTest {

    @Test
    fun `corrected keys are the live-verified ones`() {
        assertEquals("SpainPrimeraDivision", UitslagenLeagues.keyFor(302))
        assertEquals("PortugalPrimeiraLiga", UitslagenLeagues.keyFor(94))
        assertEquals("BelgiumProLeague", UitslagenLeagues.keyFor(144))
        assertEquals("SaudiArabiaProLeague", UitslagenLeagues.keyFor(203))
    }

    @Test
    fun `originally verified keys are unchanged`() {
        assertEquals("EnglandPremierLeague", UitslagenLeagues.keyFor(152))
        assertEquals("ItalySerieA", UitslagenLeagues.keyFor(207))
        assertEquals("GermanyBundesliga", UitslagenLeagues.keyFor(175))
        assertEquals("FranceLigue1", UitslagenLeagues.keyFor(168))
        assertEquals("NetherlandsEredivisie", UitslagenLeagues.keyFor(88))
        assertEquals("SwitzerlandSuperLeague", UitslagenLeagues.keyFor(187))
    }

    @Test
    fun `table is complete and honest for unknown ids`() {
        assertEquals(10, UitslagenLeagues.BY_ID.size)
        assertTrue(UitslagenLeagues.BY_ID.values.none { it.isBlank() })
        assertNull(UitslagenLeagues.keyFor(9999))
        assertNull(UitslagenLeagues.keyFor(-1))
    }
}
