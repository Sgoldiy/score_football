package com.footballpluse.footballapp.data.remote

import kotlin.math.roundToInt

/**
 * The FC API has no natural-language "advice" field, so the legacy prediction
 * cards derive one from the REAL model probabilities (dc_v2 calibrated output).
 * Nothing here is invented: every label shown was the actual top pick of the
 * probability set for that match.
 */
object FcAdvice {

    /**
     * Top pick across 1X2, BTTS and the 2.5 goals line — only reported when the
     * model is at least leaning one way (>= 50%). Returns null for coin flips,
     * which the UI renders as its neutral "win probability" state.
     */
    fun forMatch(
        homePct: Double?,
        drawPct: Double?,
        awayPct: Double?,
        bttsPct: Double?,
        over25Pct: Double?
    ): String? {
        val entries = buildList {
            homePct?.let { add("Home win" to it) }
            drawPct?.let { add("Draw" to it) }
            awayPct?.let { add("Away win" to it) }
            bttsPct?.let { add("BTTS Yes" to it) }
            over25Pct?.let { add("Over 2.5" to it) }
        }
        val best = entries.maxByOrNull { it.second } ?: return null
        return if (best.second >= 50.0) best.first else null
    }

    /** "Over 2.5: 35.6% · BTTS: 20.0%" — compact summary for the prediction card. */
    fun goalsSummary(over25Pct: Double?, bttsPct: Double?): String? {
        val parts = mutableListOf<String>()
        over25Pct?.let { parts.add("O2.5 ${fmt(it)}%") }
        bttsPct?.let { parts.add("BTTS ${fmt(it)}%") }
        return if (parts.isEmpty()) null else parts.joinToString(" \u00b7 ")
    }

    private fun fmt(v: Double): String {
        val r = (v * 10).roundToInt() / 10.0
        return if (r == r.toLong().toDouble()) r.toLong().toString() else r.toString()
    }
}
