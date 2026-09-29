package com.footballpluse.footballapp.domain.model

/**
 * Domain models for FootballCharts-only features (goal timing, season projection,
 * track record, match probability blocks). All values come straight from the API —
 * nothing here is simulated.
 */

/** League-wide goal timing summary from /leagues/{league}/goal-timing/. */
data class FcGoalTimingSummary(
    val bins: List<String>,
    val periodTotals: List<Int>,
    val totalGoals: Int,
    val lateGoals: Int,
    val matchCount: Int,
    val mostActivePeriod: String?
)

/** Per-team goal timing row (heat-map bins) from the same endpoint. */
data class FcGoalTimingRow(
    val teamName: String,
    val teamId: Int,
    val total: Int,
    val goals: List<Int>,
    val lateSharePct: Int?,
    val firstHalfPct: Int?,
    val logoUrl: String?
)

/** One team of the Monte Carlo season projection from /leagues/{league}/projection/. */
data class FcProjectionRow(
    val teamName: String,
    val teamId: Int,
    val played: Int,
    val pointsNow: Int,
    val goalDifferenceNow: Int?,
    val meanPoints: Double?,
    val p10Points: Int?,
    val p90Points: Int?,
    val titlePct: Double?,
    val top4Pct: Double?,
    val relegationPct: Double?,
    val logoUrl: String?
)

/** Track record headline numbers from /track-record/. */
data class FcTrackSummary(
    val picks: Int,
    val won: Int,
    val lost: Int,
    val void: Int,
    val profitLoss: Double?,
    val hitRate: Double?,
    val pending: Int?,
    val brier: Double?,
    val accuracyN: Int?
)

/** Per-market settled record (1x2, bts, ft_ou_25, ...). */
data class FcMarketRecord(
    val market: String,
    val picks: Int,
    val won: Int,
    val lost: Int,
    val hitRate: Double?,
    val profitLoss: Double?
)

/** One settled public prediction. */
data class FcTrackPick(
    val homeTeam: String,
    val awayTeam: String,
    val leagueName: String?,
    val matchDate: String?,
    val market: String,
    val side: String?,
    val modelProb: Double?,
    val outcome: String?,
    val resultScore: String?
)

/** Over/under probability block for a single match (/matches/{slug}/). */
data class FcMatchProbs(
    val ftOverProb: Double?,
    val ftUnderProb: Double?,
    val ftConfidence: String?,
    val htOverProb: Double?,
    val htUnderProb: Double?,
    val htConfidence: String?
)
