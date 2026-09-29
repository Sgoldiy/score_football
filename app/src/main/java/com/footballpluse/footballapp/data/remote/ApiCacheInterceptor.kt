package com.footballpluse.footballapp.data.remote

import android.util.Log
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody

/**
 * Resilience layer for the FootballCharts backend.
 *
 *  1. FC returns HTTP 200 with {"error":{"code":..,"message":..}} for known misses
 *     (e.g. an unknown match slug). That body must NOT be stored in the cache, so a
 *     later genuine response can replace any earlier error copy.
 *  2. When the backend rate-limits or fails (Render free tier can be slow/cold),
 *     we serve the last known good copy of the same URL so previously-seen data
 *     (leagues, tables, results) stays visible instead of blank screens.
 */
class ApiCacheInterceptor : Interceptor {

    companion object {
        private const val TAG = "ApiCacheInterceptor"
        private const val MAX_ENTRIES = 128
        private const val MAX_CACHED_BYTES = 2L * 1024 * 1024 // 2 MB per response
    }

    private val cache = object : LinkedHashMap<String, CachedEntry>(64, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, CachedEntry>): Boolean =
            size > MAX_ENTRIES
    }

    private data class CachedEntry(val code: Int, val body: String)

    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        val key = response.request.url.toString()

        val bodyText = try {
            response.peekBody(MAX_CACHED_BYTES).string()
        } catch (_: Exception) {
            null
        }

        // ── 1. Rate limited / backend failure: serve the last known good copy ──
        val isRateLimited = response.code == 429 ||
            (bodyText != null && looksLikeRateLimit(bodyText))
        if (isRateLimited) {
            Log.w(TAG, "Rate limited / unavailable: ${response.request.url.encodedPath}")
            val cached = synchronized(cache) { cache[key] }
            if (cached != null) {
                response.close()
                return cachedResponse(response.request, cached.body)
            }
        }

        // ── 2. Successful JSON → remember it (error bodies are never cached) ──
        if (response.isSuccessful && bodyText != null &&
            !looksLikeErrorBody(bodyText) &&
            bodyText.length < MAX_CACHED_BYTES &&
            (bodyText.trimStart().startsWith("{") || bodyText.trimStart().startsWith("["))
        ) {
            synchronized(cache) { cache[key] = CachedEntry(200, bodyText) }
        }

        return response
    }

    private fun cachedResponse(request: Request, body: String): Response =
        Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK (cached)")
            .body(body.toResponseBody("application/json".toMediaTypeOrNull()))
            .build()

    private fun looksLikeRateLimit(body: String): Boolean {
        val t = body.trim().lowercase()
        return t.contains("rate limit") || t.contains("too many requests") ||
            t.contains("quota") || t.contains("capacity")
    }

    /** FC error envelope: {"error":{"code":"not_found","message":"..."}} */
    private fun looksLikeErrorBody(body: String): Boolean {
        val t = body.trim()
        if (!t.startsWith("{")) return false
        return t.contains("\"error\"") && (t.contains("\"code\"") || t.contains("\"message\""))
    }
}
