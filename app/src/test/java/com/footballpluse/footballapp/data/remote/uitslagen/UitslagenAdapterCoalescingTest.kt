package com.footballpluse.footballapp.data.remote.uitslagen

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

class UitslagenAdapterCoalescingTest {

    private class TestUitslagenApiService : UitslagenApiService {
        val fixturesV2CallCount = AtomicInteger(0)

        override suspend fun feedLivenow(): List<UitslagenCountryFeed> = emptyList()
        override suspend fun feedAggregated(date: String, tzOffset: String): List<UitslagenCountryFeed> = emptyList()

        override suspend fun fixturesV2Small(key: String): UitslagenFixturesBlock {
            fixturesV2CallCount.incrementAndGet()
            delay(50) // simulate network latency
            return UitslagenFixturesBlock(
                table = UitslagenTable(
                    leaguename = "Premier League",
                    groups = listOf(
                        UitslagenTableGroup(
                            teams = listOf(
                                UitslagenTableRow(id_gs = "1", team = "Arsenal", points = "23", position = "1")
                            )
                        )
                    )
                ),
                fixtures = emptyList()
            )
        }

        override suspend fun matchDetail(matchId: String, h2h: String): UitslagenMatchDetail = UitslagenMatchDetail()
        override suspend fun teamPage(teamId: String): UitslagenTeamPage = UitslagenTeamPage()
        override suspend fun playerPage(playerId: String): UitslagenPlayerPage = UitslagenPlayerPage()
        override suspend fun search(query: String, country: String?): UitslagenSearchResponse = UitslagenSearchResponse()
    }

    @Test
    fun concurrent_calls_for_same_key_coalesce_into_single_api_call() = runBlocking {
        val testApi = TestUitslagenApiService()
        val adapter = UitslagenAdapter(testApi)

        // Launch 5 concurrent calls for the exact same league standings (leagueId 152 = "england")
        val results = coroutineScope {
            (1..5).map {
                async {
                    adapter.getStandings("152")
                }
            }.awaitAll()
        }

        // All 5 callers get the exact same valid standings
        assertEquals(5, results.size)
        results.forEach { standings ->
            assertEquals(1, standings.size)
            assertEquals("Arsenal", standings[0].team_name)
        }

        // Even though 5 concurrent calls occurred, the underlying API was only called ONCE!
        assertEquals(1, testApi.fixturesV2CallCount.get())
    }
}
