package com.footballpluse.footballapp.data.repository

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Pins the live-poll cadence decision that pairs with the cache's adaptive
 * live TTL: quick refreshes only while matches are actually in play, the
 * 45–60 s politeness window when idle.
 */
class LivePollCadenceTest {

    @Test
    fun `polls every 25s while a match is in play`() {
        assertEquals(25_000L, FootballRepositoryImpl.livePollDelayMs(hasLive = true))
    }

    @Test
    fun `relaxes to 60s when nothing is live`() {
        assertEquals(60_000L, FootballRepositoryImpl.livePollDelayMs(hasLive = false))
    }
}
