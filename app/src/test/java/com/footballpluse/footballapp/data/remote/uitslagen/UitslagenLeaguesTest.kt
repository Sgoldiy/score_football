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
    fun `cup and tournament competitions are verified`() {
        assertEquals("EurocupsUEFAChampionsLeague", UitslagenLeagues.keyFor(28))
        assertEquals("EurocupsUEFAEuropaLeague", UitslagenLeagues.keyFor(3))
        assertEquals("EurocupsUEFAEuropaConferenceLeague", UitslagenLeagues.keyFor(848))
        assertEquals("EurocupsUEFANationsLeagueLeagueA", UitslagenLeagues.keyFor(5))
        assertEquals("InternationalWorldWorldCup", UitslagenLeagues.keyFor(1))
        assertEquals("InternationalEuroEuroChampionship", UitslagenLeagues.keyFor(4))
        assertEquals("InternationalAmericaCopaAmerica", UitslagenLeagues.keyFor(9))
        assertEquals("InternationalAsiaAFCAsianCup", UitslagenLeagues.keyFor(17))
    }

    @Test
    fun `table is complete and honest for unknown ids`() {
        assertEquals(18, UitslagenLeagues.BY_ID.size)
        assertTrue(UitslagenLeagues.BY_ID.values.none { it.isBlank() })
        assertNull(UitslagenLeagues.keyFor(9999))
        assertNull(UitslagenLeagues.keyFor(-1))
    }

    @Test
    fun `countryFor resolves leagues from A to Z correctly`() {
        // Direct IDs
        assertEquals("England", UitslagenLeagues.countryFor(152))
        assertEquals("Spain", UitslagenLeagues.countryFor(302))
        assertEquals("Italy", UitslagenLeagues.countryFor(207))
        assertEquals("Germany", UitslagenLeagues.countryFor(175))
        assertEquals("France", UitslagenLeagues.countryFor(168))
        assertEquals("Netherlands", UitslagenLeagues.countryFor(88))
        assertEquals("Portugal", UitslagenLeagues.countryFor(94))
        assertEquals("Saudi Arabia", UitslagenLeagues.countryFor(203))
        assertEquals("Belgium", UitslagenLeagues.countryFor(144))
        assertEquals("Switzerland", UitslagenLeagues.countryFor(187))
        assertEquals("India", UitslagenLeagues.countryFor(2556))
        assertEquals("Europe", UitslagenLeagues.countryFor(28))
        assertEquals("World", UitslagenLeagues.countryFor(1))

        // Name inference
        assertEquals("England", UitslagenLeagues.countryFor(null, "Premier League"))
        assertEquals("Spain", UitslagenLeagues.countryFor(null, "La Liga"))
        assertEquals("Italy", UitslagenLeagues.countryFor(null, "Serie A"))
        assertEquals("Germany", UitslagenLeagues.countryFor(null, "Bundesliga"))
        assertEquals("France", UitslagenLeagues.countryFor(null, "Ligue 1"))
        assertEquals("Netherlands", UitslagenLeagues.countryFor(null, "Eredivisie"))
        assertEquals("Portugal", UitslagenLeagues.countryFor(null, "Primeira Liga"))
        assertEquals("Saudi Arabia", UitslagenLeagues.countryFor(null, "Saudi Pro League"))
        assertEquals("Belgium", UitslagenLeagues.countryFor(null, "Belgian Pro League"))
        assertEquals("Switzerland", UitslagenLeagues.countryFor(null, "Swiss Super League"))
        assertEquals("India", UitslagenLeagues.countryFor(null, "Indian Super League"))
        assertEquals("Japan", UitslagenLeagues.countryFor(null, "J-League"))
        assertEquals("USA", UitslagenLeagues.countryFor(null, "MLS"))
        assertEquals("Europe", UitslagenLeagues.countryFor(null, "UEFA Champions League"))
        assertEquals("World", UitslagenLeagues.countryFor(null, "FIFA World Cup"))

        // Raw country normalization
        assertEquals("Europe", UitslagenLeagues.countryFor(null, null, "Eurocups"))
        assertEquals("World", UitslagenLeagues.countryFor(null, null, "International Cups"))
        assertEquals("Saudi Arabia", UitslagenLeagues.countryFor(null, null, "SaudiArabia"))
    }
}
