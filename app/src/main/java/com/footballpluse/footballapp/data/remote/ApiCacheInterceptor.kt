package com.footballpluse.footballapp.data.remote

import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import java.io.File
import java.security.MessageDigest
import java.time.Clock
import java.time.Instant
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.logging.Logger

/**
 * Request-reducing cache for the shared upstream (`uitslagen.live/footapi`), an
 * unofficial API with no published quota — every call we don't make is goodwill
 * preserved.
 *
 * Layers (checked in order):
 *  1. **Fresh** — a cached response younger than the entry's TTL is served
 *     immediately, no network. The app's 60 s live poll, screen re-entries and
 *     the fixtures date rail stop reaching the upstream entirely.
 *  2. **Upstream cooldown** — after an observed rate limit (HTTP 429 or an error
 *     envelope) that URL is served from cache for a cooldown period without
 *     touching the upstream.
 *  3. **Stale-while-revalidate** — a cached response past its TTL is served
 *     immediately while ONE background refresh runs; concurrent callers all get
 *     the cached copy.
 *  4. **Coalescing** — simultaneous callers of the same URL (cache miss) share
 *     one network response.
 *  5. **Failure fallback** — a network error serves the last known copy of the
 *     same URL instead of blank screens.
 *
 * Persistent across app restarts via small files in a caller-provided directory
 * (pass `File(context.filesDir, "http_cache")`); purely in-memory when no
 * directory is given (tests).
 *
 * Deliberately free of Android imports so the whole behavior is unit-testable
 * on the JVM. Must be added AFTER the interceptor that finalizes request URLs
 * (global params, User-Agent) so cache keys are stable and background
 * revalidations go upstream with the exact same shape as foreground ones.
 */
class ApiCacheInterceptor(
    private val dir: File? = null,
    private val clock: Clock = Clock.systemUTC(),
) : Interceptor {

    companion object {
        private val LOG = Logger.getLogger("ApiCacheInterceptor")
        private const val MAX_ENTRIES = 256
        private const val MAX_CACHED_BYTES = 2L * 1024 * 1024 // 2 MB per response
        private const val STALE_MAX_AGE_MS = 24 * 60 * 60 * 1000L // disk eviction horizon

        /** In-URL markers of the endpoints whose freshness demands differ. */
        private const val LIVE_FEED = "feed_livenow"
        private const val DAY_FEED = "feed_matches_aggregated"

        /**
         * TTL per endpoint class. The live feed adapts: 20 s while matches are
         * in play (so goals surface in ~25 s), relaxing to 60 s when nothing is
         * live — either way inside the 45–60 s politeness window, with all
         * pollers coalescing onto the single cached copy.
         */
        private const val TTL_LIVE_IDLE_MS = 60_000L
        private const val TTL_LIVE_ACTIVE_MS = 20_000L
        private const val TTL_DAY_FEED_MS = 60_000L
        private const val TTL_DEFAULT_MS = 120_000L

        /**
         * The upstream marks in-play matches with a bare numeric `status`
         * (`"status":"43"`); finished/scheduled rows use literal words
         * ("FT", "Not Started") — see docs/LIVE_FEED.md and UitslagenStatus.
         */
        private val LIVE_MATCH = Regex("\"status\"\\s*:\\s*\"\\d+\"")

        /** Back-off after an observed rate limit before that URL is allowed upstream again. */
        private const val RATE_LIMIT_COOLDOWN_MS = 60_000L

        private const val MEDIA_JSON = "application/json"
    }

    private data class CacheEntry(val code: Int, val body: String, val storedAt: Instant)

    /** URL → in-flight network fetch; late joiners wait for the leader's result. */
    private val inflight = ConcurrentHashMap<String, CompletableFuture<String>>()

    /** URL → epoch ms until which the upstream must not be asked for this key. */
    private val cooldownUntil = ConcurrentHashMap<String, Long>()

    private val memory = ConcurrentHashMap<String, CacheEntry>()

    /** Single daemon thread = background revalidations happen one at a time, politely. */
    private val refreshExecutor = Executors.newSingleThreadExecutor { r ->
        Thread(r, "api-cache-refresh").apply { isDaemon = true }
    }

    init {
        if (dir != null) runCatching { evictExpiredFromDisk() }
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val key = request.url.toString()
        val now = Instant.now(clock)

        val cached = get(key, now)
        val ttl = ttlFor(key, cached)

        // 1. Fresh in cache → serve it, no network.
        if (cached != null && now.toEpochMilli() - cached.storedAt.toEpochMilli() < ttl) {
            return respond(request, cached)
        }

        // 2. Upstream on cooldown (recently rate-limited) → cache or give up politely.
        val cooldown = cooldownUntil[key]
        if (cooldown != null && now.toEpochMilli() < cooldown) {
            if (cached != null) return respond(request, cached)
            // First-ever failure for this key: the caller must see the real error.
            return proceedAndCache(chain, request, key)
        }

        // 3. Stale copy exists → serve it now, refresh once in the background.
        if (cached != null) {
            revalidateInBackground(key, request)
            return respond(request, cached)
        }

        // 4. Cold cache → go upstream, collapsing identical concurrent requests.
        val existing = inflight[key]
        if (existing != null) return waitFor(existing, request)
        val leaderFuture = CompletableFuture<String>()
        if (inflight.putIfAbsent(key, leaderFuture) != null) {
            return waitFor(inflight[key]!!, request)
        }
        return try {
            val response = chain.proceed(request)
            val body = response.peekBody(MAX_CACHED_BYTES).string()
            if (isRateLimited(response.code, body)) {
                noteRateLimit(key, Instant.now(clock))
                leaderFuture.completeExceptionally(IllegalStateException("Upstream rate-limited"))
                response
            } else {
                store(key, response.code, body, Instant.now(clock))
                leaderFuture.complete(body)
                respond(request, CacheEntry(response.code, body, Instant.now(clock)))
            }
        } catch (e: Exception) {
            leaderFuture.completeExceptionally(e)
            // 5. Network failed: serve the last known good copy if we have one.
            val stale = get(key, Instant.now(clock))
            if (stale != null) respond(request, stale) else throw e
        } finally {
            inflight.remove(key, leaderFuture)
        }
    }

    // ── Layers ────────────────────────────────────────────────────────────────

    /** Refresh [key] once in the background; concurrent callers see the shared future. */
    private fun revalidateInBackground(key: String, template: Request) {
        val future = CompletableFuture<String>()
        if (inflight.putIfAbsent(key, future) != null) return // refresh already running
        refreshExecutor.execute {
            try {
                val response = OkHttpClient().newCall(template).execute()
                val body = response.peekBody(MAX_CACHED_BYTES).string()
                response.close()
                if (isRateLimited(response.code, body)) {
                    noteRateLimit(key, Instant.now(clock))
                    future.completeExceptionally(IllegalStateException("Upstream rate-limited"))
                } else {
                    store(key, response.code, body, Instant.now(clock))
                    future.complete(body)
                }
            } catch (e: Exception) {
                future.completeExceptionally(e) // background failure is silent by design
            } finally {
                inflight.remove(key, future)
            }
        }
    }

    /** Cold-cache path with no protection available: go upstream, store successes. */
    private fun proceedAndCache(
        chain: Interceptor.Chain,
        request: Request,
        key: String,
    ): Response {
        val response = chain.proceed(request)
        val body = response.peekBody(MAX_CACHED_BYTES).string()
        if (isRateLimited(response.code, body)) {
            noteRateLimit(key, Instant.now(clock))
            return response
        }
        if (isCacheable(body)) store(key, response.code, body, Instant.now(clock))
        return response
    }

    private fun waitFor(future: CompletableFuture<String>, request: Request): Response =
        try {
            val body = future.get(30, TimeUnit.SECONDS)
            respond(request, CacheEntry(200, body, Instant.now(clock)))
        } catch (e: ExecutionException) {
            throw (e.cause ?: IllegalStateException("Coalesced request failed"))
        } catch (_: TimeoutException) {
            throw IllegalStateException("Timed out waiting for coalesced response")
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
            throw IllegalStateException("Interrupted waiting for coalesced response")
        }

    // ── Storage ───────────────────────────────────────────────────────────────

    private fun store(key: String, code: Int, body: String, at: Instant) {
        if (!isCacheable(body)) return
        val entry = CacheEntry(code, body, at)
        memory[key] = entry
        dir?.let { runCatching { writeEntryToDisk(it, key, entry) } }
    }

    private fun get(key: String, now: Instant): CacheEntry? {
        memory[key]?.let { if (ageMs(it, now) < STALE_MAX_AGE_MS) return it }
        val d = dir ?: return null
        return runCatching { readEntryFromDisk(d, key) }.getOrNull()?.also { memory[key] = it }
    }

    private fun ageMs(entry: CacheEntry, now: Instant) =
        (now.toEpochMilli() - entry.storedAt.toEpochMilli()).coerceAtLeast(0)

    private fun isCacheable(body: String): Boolean =
        body.length < MAX_CACHED_BYTES &&
            (body.trimStart().startsWith("{") || body.trimStart().startsWith("[")) &&
            !looksLikeErrorBody(body)

    private fun isRateLimited(code: Int, body: String): Boolean {
        if (code == 429) return true
        val t = body.trim().lowercase()
        return t.contains("rate limit") || t.contains("too many requests") ||
            t.contains("quota") || t.contains("capacity")
    }

    /** Upstream error envelope: `{"error":{"code":"...","message":"..."}}` — never cache. */
    private fun looksLikeErrorBody(body: String): Boolean {
        val t = body.trim()
        if (!t.startsWith("{")) return false
        return t.contains("\"error\"") && (t.contains("\"code\"") || t.contains("\"message\""))
    }

    private fun noteRateLimit(key: String, now: Instant) {
        cooldownUntil[key] = now.toEpochMilli() + RATE_LIMIT_COOLDOWN_MS
        LOG.warning("Rate limited: ${urlForLog(key)} — serving cache for ${RATE_LIMIT_COOLDOWN_MS / 1000}s")
    }

    private fun ttlFor(url: String, entry: CacheEntry?): Long = when {
        url.contains(LIVE_FEED) ->
            if (entry != null && LIVE_MATCH.containsMatchIn(entry.body)) TTL_LIVE_ACTIVE_MS else TTL_LIVE_IDLE_MS
        url.contains(DAY_FEED) -> TTL_DAY_FEED_MS
        else -> TTL_DEFAULT_MS
    }

    // ── Response construction ─────────────────────────────────────────────────

    private fun respond(request: Request, entry: CacheEntry): Response =
        Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(entry.code)
            .message("OK (cached)")
            .body(entry.body.toResponseBody(MEDIA_JSON.toMediaTypeOrNull()))
            .build()

    // ── Disk persistence ──────────────────────────────────────────────────────

    private fun fileFor(dir: File, key: String): File {
        val digest = MessageDigest.getInstance("MD5")
            .digest(key.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
        return File(dir, "${digest}.cache")
    }

    private fun writeEntryToDisk(dir: File, key: String, entry: CacheEntry) {
        dir.mkdirs()
        val target = fileFor(dir, key)
        val tmp = File(dir, target.name + ".tmp")
        tmp.writeText(
            jsonEscape(key) + "\n" + entry.storedAt.toEpochMilli() + "\n" + entry.body,
            Charsets.UTF_8,
        )
        if (!tmp.renameTo(target)) tmp.delete()
        pruneDisk(dir)
    }

    private fun readEntryFromDisk(dir: File, key: String): CacheEntry? {
        val file = fileFor(dir, key)
        if (!file.exists()) return null
        val text = file.readText(Charsets.UTF_8)
        val first = text.indexOf('\n')
        val second = text.indexOf('\n', first + 1)
        if (first < 0 || second < 0) return null
        val entry = CacheEntry(
            code = 200,
            body = text.substring(second + 1),
            storedAt = Instant.ofEpochMilli(text.substring(first + 1, second).toLong()),
        )
        if (ageMs(entry, Instant.now(clock)) > STALE_MAX_AGE_MS) {
            file.delete()
            return null
        }
        return entry
    }

    /** Drop anything on disk past the stale horizon (called once per process). */
    private fun evictExpiredFromDisk() {
        val d = dir ?: return
        if (!d.isDirectory) return
        val nowMs = Instant.now(clock).toEpochMilli()
        d.listFiles { f -> f.isFile && f.name.endsWith(".cache") }?.forEach { f ->
            if (nowMs - f.lastModified() > STALE_MAX_AGE_MS) f.delete()
        }
    }

    /** Keep only the newest [MAX_ENTRIES] files; also drop stray temp files. */
    private fun pruneDisk(dir: File) {
        val files = dir.listFiles { f -> f.isFile } ?: return
        files.filter { it.name.endsWith(".tmp") }.forEach { it.delete() }
        val caches = files.filter { it.name.endsWith(".cache") }
        if (caches.size > MAX_ENTRIES) {
            caches.sortedByDescending { it.lastModified() }
                .drop(MAX_ENTRIES)
                .forEach { it.delete() }
        }
    }

    /** Keys can be long URLs — shorten them for logs. */
    private fun urlForLog(key: String) = key.substringAfter("footapi/", key).take(80)

    /** Minimal escaping for the one string we write into our flat file format. */
    private fun jsonEscape(s: String) = s.replace("\\", "\\\\").replace("\n", "\\n")
}
