package com.footballpluse.footballapp.data.remote.uitslagen

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.floor

/**
 * Status decoder for the uitslagen.live/footapi feeds (Step 1 of mission-uitslagen).
 *
 * Everything in this file is pure and unit-testable: the upstream payloads were
 * captured live on 2026-09-29 into `app/src/test/resources/uitslagen/` and the
 * findings are documented in `docs/LIVE_FEED.md`.
 *
 * ## What the raw `status` field looks like
 *
 * - Day feed (`feed_matches_aggregated.json`), team page fixtures and league
 *   fixtures use human strings: `"Not Started"`, `"FT"`, `"Postp."`, `"Cancelled"`,
 *   or a kickoff time like `"18:45"`.
 * - The live feed (`feed_livenow.json`) uses short numeric codes while a match
 *   is running: `"3"`, `"5"`, `"14"`, `"15"`, `"17"`, `"43"`, `"57"` were all
 *   observed live on 2026-09-29. The SAME match reported a different code in
 *   `matches/{id}.json` than in the live feed (43->45, 57->59, 5->7, 14->16,
 *   15->17, 17->19), and no stable mapping to a football meaning could be
 *   established from capture (see docs/LIVE_FEED.md, "Status codes" section).
 *
 * ## The reliable interpretation
 *
 * Because the codes are unstable between endpoints, this decoder derives the
 * match phase from the UTC kickoff time rather than trusting the numeric code:
 *
 *  - `HH:MM` string            -> scheduled (kickoff time)
 *  - literal string            -> mapped word (FT, Not Started, Postp., ...)
 *  - numeric code + no kickoff -> unknown live state
 *  - numeric code + kickoff    -> derive from elapsed playing time:
 *    < 0 not started; <=45 first half; <=60 half time; <=105 second half
 *    (minute = elapsed - 15); a short stoppage grace window shows 90';
 *    beyond that -> finished.
 *
 * Minute display convention (same as the website): a match in the first half
 * shows the elapsed minute; after the break it shows `45 + elapsed-since-45`.
 */
object UitslagenStatus {

    /** Half-time break length assumed when deriving phases from wall-clock time. */
    const val HALF_LENGTH_MIN: Long = 45
    const val HT_BREAK_MIN: Long = 15

    /** Decoded, UI-ready match state. */
    data class Phase(
        val isLive: Boolean,
        val isFinished: Boolean,
        val isScheduled: Boolean,
        /** Display word, e.g. "In Play", "HT", "FT", "Not Started", "Postp.". */
        val label: String,
        /** Live minute when [isLive], else null. */
        val minute: Int? = null,
    )

    /**
     * Decode a raw status value.
     *
     * @param raw           the `status` string exactly as it appears in the feed
     * @param kickoffUtcMs  UTC epoch millis of kickoff, if known (null otherwise)
     * @param nowUtcMs      current wall clock in UTC millis
     */
    fun decode(raw: String?, kickoffUtcMs: Long?, nowUtcMs: Long): Phase {
        val text = raw?.trim().orEmpty()
        if (text.isEmpty()) return Phase(false, false, false, "Unknown")

        // Literal word statuses (day feed / fixtures / team page).
        when (text.uppercase(Locale.US)) {
            "FT", "AET", "AP" -> return Phase(false, true, false, "FT")
            "NOT STARTED" -> return Phase(false, false, true, "Not Started")
            "POSTP.", "POSTPONED" -> return Phase(false, false, false, "Postponed")
            "CANC", "CANCELLED", "CANCELED" -> return Phase(false, false, false, "Cancelled")
            "ABD", "ABANDONED" -> return Phase(false, false, false, "Abandoned")
            "SUSP", "SUSPENDED" -> return Phase(false, false, false, "Suspended")
        }

        // Kickoff time shown as status => scheduled.
        if (STATUS_TIME_REGEX.matches(text)) {
            return Phase(false, false, true, "Not Started")
        }

        // Numeric code: only trustworthy signal is elapsed playing time.
        if (NUMERIC_REGEX.matches(text)) {
            val kickoff = kickoffUtcMs ?: return Phase(true, false, false, "In Play")
            return fromElapsed(kickoff, nowUtcMs)
        }

        return Phase(false, false, false, "Unknown")
    }

    /** Derive the phase purely from kickoff time vs now (codes ignored). */
    fun fromElapsed(kickoffUtcMs: Long, nowUtcMs: Long): Phase {
        val elapsedMin = floor((nowUtcMs - kickoffUtcMs) / 60_000.0)
        return when {
            elapsedMin < 0 -> Phase(false, false, true, "Not Started")
            elapsedMin <= HALF_LENGTH_MIN ->
                Phase(true, false, false, "In Play", minute = elapsedMin.toInt().coerceAtLeast(1))
            elapsedMin <= HALF_LENGTH_MIN + HT_BREAK_MIN ->
                Phase(true, false, false, "HT")
            elapsedMin <= 2 * HALF_LENGTH_MIN + HT_BREAK_MIN ->
                Phase(true, false, false, "In Play", minute = (elapsedMin - HT_BREAK_MIN).toInt())
            elapsedMin <= 2 * HALF_LENGTH_MIN + HT_BREAK_MIN + FINISHED_GRACE_MIN ->
                Phase(true, false, false, "In Play", minute = (2 * HALF_LENGTH_MIN).toInt())
            else -> Phase(false, true, false, "FT")
        }
    }

    /**
     * Parse a kickoff "date + time" pair as UTC epoch millis.
     * Upstream dates are `dd/MM/yyyy` and times `HH:MM`, both UTC.
     */
    fun kickoffUtcMillis(date: String?, time: String?): Long? {
        val d = date?.trim().orEmpty()
        val t = time?.trim().orEmpty()
        if (!DATE_REGEX.matches(d) || !TIME_REGEX.matches(t)) return null
        return try {
            val fmt = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.US)
            fmt.timeZone = TimeZone.getTimeZone("UTC")
            fmt.isLenient = false
            val parsed = fmt.parse("$d $t") ?: return null
            parsed.time
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Parse a score cell like `"1 - 0"` into (home, away).
     * Placeholder cells (`" - "`, `"-"`, blank) yield null.
     */
    fun parseScore(scoretime: String?): Pair<Int, Int>? {
        val m = SCORE_REGEX.find(scoretime?.trim().orEmpty()) ?: return null
        val h = m.groupValues[1].toIntOrNull() ?: return null
        val a = m.groupValues[2].toIntOrNull() ?: return null
        return h to a
    }

    /**
     * True when the match plausibly started (or finished) at [statusRow] time.
     * Used to decide whether a fixture row without a live code may still be
     * in play because its league is not tracked by the live feed.
     */
    fun mayBeLive(kickoffUtcMs: Long?, nowUtcMs: Long): Boolean {
        val kickoff = kickoffUtcMs ?: return false
        val elapsedMin = (nowUtcMs - kickoff) / 60_000
        return elapsedMin in 0..(2 * HALF_LENGTH_MIN + HT_BREAK_MIN + FINISHED_GRACE_MIN)
    }

    /** Remove a fixture-suffix from an id, e.g. `"3949779_f" -> "3949779"`. */
    fun baseId(id: String?): String? = id?.trim()?.takeIf { it.isNotEmpty() }?.substringBefore('_')

    private const val FINISHED_GRACE_MIN = 5L

    private val STATUS_TIME_REGEX = Regex("""^\d{1,2}:\d{2}$""")
    private val NUMERIC_REGEX = Regex("""^\d{1,3}$""")
    private val DATE_REGEX = Regex("""^\d{2}/\d{2}/\d{4}$""")
    private val TIME_REGEX = Regex("""^\d{1,2}:\d{2}$""")
    private val SCORE_REGEX = Regex("""^(\d+)\s*-\s*(\d+)$""")
}

/** Timestamp helper shared by adapters (kept out of the decoder for testability). */
fun utcDateFormatter(): SimpleDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
    timeZone = TimeZone.getTimeZone("UTC")
}
