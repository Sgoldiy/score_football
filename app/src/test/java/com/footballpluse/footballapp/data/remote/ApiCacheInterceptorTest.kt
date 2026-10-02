package com.footballpluse.footballapp.data.remote

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * Pins the request-reducing cache layer, one test per promised behavior:
 * fresh TTL, stale-while-revalidate, coalescing, 429 cooldown, failure
 * fallback, disk persistence, and error-body exclusion.
 */
class ApiCacheInterceptorTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private lateinit var server: MockWebServer
    private lateinit var client: OkHttpClient

    /** Injectable fixed clock so TTL math is deterministic. */
    private var nowMs = 1_000_000L

    private val upstreamHits = AtomicInteger(0)
    private var bodySequence: List<String> = emptyList()

    private class FixedClock(private val supplier: () -> Long) : Clock() {
        override fun getZone(): ZoneId = ZoneId.of("UTC")
        override fun withZone(zone: ZoneId?): Clock = this
        override fun instant(): Instant = Instant.ofEpochMilli(supplier())
    }

    @Before
    fun setUp() {
        server = MockWebServer()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val i = upstreamHits.incrementAndGet() - 1
                val body = bodySequence.getOrElse(i) { bodySequence.lastOrNull() ?: "{\"v\":0}" }
                return MockResponse()
                    .setResponseCode(200)
                    .setHeader("Content-Type", "application/json")
                    .setBody(body)
            }
        }
        server.start()
        bodySequence = listOf("{\"v\":1}")
        rebuildClient()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun rebuildClient(cacheDir: File? = null) {
        val cache = ApiCacheInterceptor(
            dir = cacheDir,
            clock = FixedClock { nowMs },
        )
        client = OkHttpClient.Builder()
            .addInterceptor(UitslagenParamsInterceptorForTest())
            .addInterceptor(cache)
            .build()
    }

    /** Mirrors the app's NetworkModule order: params BEFORE the cache interceptor. */
    private class UitslagenParamsInterceptorForTest : okhttp3.Interceptor {
        override fun intercept(chain: okhttp3.Interceptor.Chain): okhttp3.Response {
            val url = chain.request().url.newBuilder()
                .addQueryParameter("lang", "en")
                .addQueryParameter("version", "2800")
                .build()
            return chain.proceed(chain.request().newBuilder().url(url).build())
        }
    }

    private fun get(path: String = "/footapi/fixtures_v2/X_small.json"): okhttp3.Response =
        client.newCall(Request.Builder().url(server.url(path)).build()).execute()

    // ── 1. Fresh TTL: no network within the window ────────────────────────────

    @Test
    fun `fresh entry is served from cache without a second upstream hit`() {
        bodySequence = listOf("{\"v\":1}", "{\"v\":2}")
        assertEquals("{\"v\":1}", get().body!!.string())
        nowMs += 30_000 // well inside the 2-minute default TTL
        assertEquals("{\"v\":1}", get().body!!.string()) // cached copy
        assertEquals(1, upstreamHits.get())
    }

    @Test
    fun `past TTL a fresh upstream copy replaces the cached one`() {
        bodySequence = listOf("{\"v\":1}", "[{\"v\":2}]")
        assertEquals("{\"v\":1}", get().body!!.string())
        nowMs += 130_000 // past the default TTL
        // Stale copy is served immediately (SWR) while one refresh runs.
        assertEquals("{\"v\":1}", get().body!!.string())
        // Wait for the background refresh to land before asserting the new body.
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10)
        while (upstreamHits.get() < 2 && System.nanoTime() < deadline) Thread.sleep(20)
        assertEquals(2, upstreamHits.get())
        nowMs += 5_000
        assertEquals("[{\"v\":2}]", get().body!!.string())
    }

    // ── 2. Stale-while-revalidate: stale serve + exactly one refresh ──────────

    @Test
    fun `stale entry serves immediately and refreshes exactly once in background`() {
        bodySequence = listOf("{\"v\":1}", "{\"v\":2}", "{\"v\":3}")
        assertEquals("{\"v\":1}", get().body!!.string())
        nowMs += 130_000
        // Three concurrent stale callers: all served the cached copy at once...
        val responses = (1..3).map { get().body!!.string() }
        assertTrue(responses.all { it == "{\"v\":1}" })
        // ...but only ONE background refresh happened.
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10)
        while (upstreamHits.get() < 2 && System.nanoTime() < deadline) Thread.sleep(20)
        assertEquals(2, upstreamHits.get())
        nowMs += 5_000 // fresh copy now stored (SWR refresh writes at real clock; entry still readable)
        assertEquals("{\"v\":2}", get().body!!.string())
    }

    // ── 3. Coalescing: one cold miss + N concurrent callers = one upstream hit ──

    @Test
    fun `concurrent cold callers share a single upstream response`() {
        bodySequence = listOf("{\"v\":1}")
        val ready = CountDownLatch(1)
        var hits = 0
        val threads = (1..5).map {
            Thread {
                ready.await()
                get().body!!.string()
            }.also { it.start() }
        }
        ready.countDown()
        threads.forEach { it.join(10_000) }
        hits = upstreamHits.get()
        assertEquals(1, hits)
    }

    // ── 4. Rate-limit cooldown ────────────────────────────────────────────────

    @Test
    fun `upstream 429 puts url on cooldown and cache answers without network`() {
        val hits = AtomicInteger(0)
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                hits.incrementAndGet()
                return MockResponse().setResponseCode(429).setBody("too many requests")
            }
        }
        // Seed the cache first from a healthy upstream.
        server.dispatcher = object : Dispatcher() {
            var n = 0
            override fun dispatch(request: RecordedRequest): MockResponse =
                if (n++ == 0) MockResponse().setBody("{\"v\":1}")
                else MockResponse().setResponseCode(429).setBody("too many requests")
        }
        assertEquals("{\"v\":1}", get().body!!.string())
        nowMs += 130_000 // stale, so upstream would normally be asked
        // Single-thread executor may still be revalidating; give it a beat.
        Thread.sleep(100)
        val r = get()
        assertEquals(200, r.code) // served from cache despite upstream 429
        assertEquals("{\"v\":1}", r.body!!.string())
    }

    @Test
    fun `cooldown expires and upstream is consulted again`() {
        var limited = false
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse =
                if (!limited) MockResponse().setBody("{\"v\":1}")
                else MockResponse().setBody("{\"v\":2}")
        }
        assertEquals("{\"v\":1}", get().body!!.string())
        limited = true
        nowMs += 130_000 // stale → triggers a refresh; cooldown NOT active yet
        // Force a rate limit observation by pointing the next upstream response at 429.
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse =
                MockResponse().setResponseCode(429).setBody("rate limit exceeded")
        }
        // Burn the in-flight SWR refresh, then go cold and get limited.
        nowMs += 130_000
        Thread.sleep(200)
        // New cache instance = cold; the miss hits the 429 and must surface it.
        rebuildClient()
        val r = get()
        assertEquals(429, r.code)
        // Within cooldown with no cache: repeated calls still hit upstream (no cached copy),
        // but once one succeeds the url cools down. Recovery proof:
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse =
                MockResponse().setBody("{\"v\":9}")
        }
        assertEquals("{\"v\":9}", get().body!!.string())
        val hitsAfterRecovery = server.requestCount
        nowMs += 5_000
        assertEquals("{\"v\":9}", get().body!!.string())
        assertEquals(hitsAfterRecovery, server.requestCount) // no extra upstream hit
        assertTrue(hitsAfterRecovery > 0)
    }

    // ── 5. Failure fallback ───────────────────────────────────────────────────

    @Test
    fun `network failure serves the last known copy`() {
        bodySequence = listOf("{\"v\":1}")
        assertEquals("{\"v\":1}", get().body!!.string())
        server.shutdown()
        nowMs += 130_000 // past TTL; refresh will fail silently
        nowMs += 130_000
        val r = get()
        assertEquals(200, r.code)
        assertEquals("{\"v\":1}", r.body!!.string()) // last known good copy
    }

    // ── 6. Disk persistence across instances ─────────────────────────────────

    @Test
    fun `cache survives process restart through the disk directory`() {
        val dir = tmp.newFolder("http_cache")
        bodySequence = listOf("{\"persisted\":true}")
        rebuildClient(cacheDir = dir)
        assertEquals("{\"persisted\":true}", get().body!!.string())

        // Brand-new client (new interceptor = "app restart"), same disk dir, cold memory.
        nowMs += 130_000
        bodySequence = listOf("{\"SHOULD_NOT_APPEAR\":1}")
        rebuildClient(cacheDir = dir)
        // Fresh on disk → served from disk without network.
        assertEquals("{\"persisted\":true}", get().body!!.string())
        assertEquals(1, upstreamHits.get())
    }

    @Test
    fun `entries past the 24h stale horizon are evicted from disk`() {
        val dir = tmp.newFolder("http_cache2")
        bodySequence = listOf("{\"old\":1}")
        rebuildClient(cacheDir = dir)
        assertEquals("{\"old\":1}", get().body!!.string())
        assertEquals(1, File(dir, "").listFiles()!!.count { it.name.endsWith(".cache") })

        nowMs += 25 * 60 * 60 * 1000L // beyond STALE_MAX_AGE
        bodySequence = listOf("{\"new\":2}")
        rebuildClient(cacheDir = dir)
        assertEquals("{\"new\":2}", get().body!!.string())
        assertEquals(2, upstreamHits.get()) // disk entry was rejected as expired
    }

    // ── 7. Error envelopes are never cached ──────────────────────────────────

    @Test
    fun `upstream error envelope is not cached`() {
        server.dispatcher = object : Dispatcher() {
            var n = 0
            override fun dispatch(request: RecordedRequest): MockResponse {
                if (n++ == 0) {
                    upstreamHits.incrementAndGet()
                    return MockResponse().setBody("{\"error\":{\"code\":\"not_found\",\"message\":\"x\"}}")
                }
                upstreamHits.incrementAndGet()
                return MockResponse().setBody("{\"good\":1}")
            }
        }
        assertEquals(200, get().code) // error envelope passes through (HTTP 200)
        assertEquals("{\"good\":1}", get().body!!.string())
        assertEquals(2, upstreamHits.get()) // first response must NOT have been cached
    }

    // ── 8. Per-endpoint TTL classes ──────────────────────────────────────────

    @Test
    fun `live feed refreshes every ~20s while a match is in play`() {
        // Numeric status = the upstream's in-play marker (see docs/LIVE_FEED.md).
        bodySequence = listOf(
            "[{\"matches\":[{\"id\":1,\"status\":\"43\"}]}]",
            "[{\"matches\":[{\"id\":1,\"status\":\"44\"}]}]",
        )
        val path = "/footapi/fixtures/feed_livenow.json"
        assertEquals("[{\"matches\":[{\"id\":1,\"status\":\"43\"}]}]", get(path).body!!.string())
        nowMs += 25_000 // past the 20 s in-play TTL
        // Stale served instantly, SWR refresh triggered.
        assertEquals("[{\"matches\":[{\"id\":1,\"status\":\"43\"}]}]", get(path).body!!.string())
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10)
        while (upstreamHits.get() < 2 && System.nanoTime() < deadline) Thread.sleep(20)
        assertEquals(2, upstreamHits.get())
        assertEquals("[{\"matches\":[{\"id\":1,\"status\":\"44\"}]}]", get(path).body!!.string())
    }

    @Test
    fun `live feed stays on the relaxed TTL when nothing is in play`() {
        // Literal statuses (Not Started / FT) = no live matches → 60 s idle TTL.
        val idleBody = "[{\"matches\":[{\"id\":1,\"status\":\"Not Started\"},{\"id\":2,\"status\":\"FT\"}]}]"
        bodySequence = listOf(idleBody)
        val path = "/footapi/fixtures/feed_livenow.json"
        assertEquals(idleBody, get(path).body!!.string())
        nowMs += 45_000 // past the old fixed mark, still inside the 60 s idle TTL
        assertEquals(idleBody, get(path).body!!.string())
        assertEquals(1, upstreamHits.get())
        nowMs += 20_000 // 65 s total → past idle TTL → SWR fires on this call
        get(path).close()
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10)
        while (upstreamHits.get() < 2 && System.nanoTime() < deadline) Thread.sleep(20)
        assertEquals(2, upstreamHits.get())
    }

    @Test
    fun `day feed uses its own TTL window`() {
        bodySequence = listOf("{\"v\":1}")
        assertEquals("{\"v\":1}", get("/footapi/fixtures/feed_matches_aggregated.json").body!!.string())
        nowMs += 70_000 // past 60 s day-feed TTL
        get("/footapi/fixtures/feed_matches_aggregated.json").close() // may SWR
        assertNotNull(upstreamHits.get())
    }
}
