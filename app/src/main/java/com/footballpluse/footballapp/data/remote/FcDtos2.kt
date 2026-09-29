package com.footballpluse.footballapp.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = false)
data class FcProjectionResponse(
    val projection: FcProjectionPayload? = null,
    val attribution: String? = null
)

@JsonClass(generateAdapter = false)
data class FcProjectionPayload(
    val league: String? = null,
    val season: String? = null,
    @Json(name = "run_date") val runDate: String? = null,
    @Json(name = "n_sims") val nSims: Int? = null,
    @Json(name = "scheduled_remaining") val scheduledRemaining: Int? = null,
    @Json(name = "expected_remaining") val expectedRemaining: Int? = null,
    @Json(name = "partial_schedule") val partialSchedule: Boolean? = null,
    val teams: Map<String, FcProjectionTeamDto> = emptyMap()
)

@JsonClass(generateAdapter = false)
data class FcProjectionTeamDto(
    val played: Int = 0,
    @Json(name = "pts_now") val ptsNow: Int = 0,
    @Json(name = "gd_now") val gdNow: Int? = null,
    @Json(name = "mean_pts") val meanPts: Double? = null,
    @Json(name = "p10_pts") val p10Pts: Int? = null,
    @Json(name = "p90_pts") val p90Pts: Int? = null,
    val title: Double? = null,
    val top4: Double? = null,
    val bottom1: Double? = null,
    val bottom2: Double? = null,
    val bottom3: Double? = null,
    @Json(name = "position_matrix") val positionMatrix: List<Double> = emptyList()
)

@JsonClass(generateAdapter = false)
data class FcGoalTimingResponse(
    val league: String? = null,
    val season: String? = null,
    @Json(name = "time_bins") val timeBins: List<String> = emptyList(),
    val stats: FcGoalTimingSummaryDto? = null,
    val data: List<FcGoalTimingTeamDto> = emptyList(),
    val note: String? = null,
    val attribution: String? = null
)

@JsonClass(generateAdapter = false)
data class FcGoalTimingSummaryDto(
    @Json(name = "total_goals") val totalGoals: Int? = null,
    @Json(name = "most_active_period") val mostActivePeriod: String? = null,
    @Json(name = "period_totals") val periodTotals: List<Int> = emptyList(),
    @Json(name = "late_goals") val lateGoals: Int? = null,
    @Json(name = "match_count") val matchCount: Int? = null
)

@JsonClass(generateAdapter = false)
data class FcGoalTimingTeamDto(
    val team: String,
    val total: Int = 0,
    val bins: Map<String, Int> = emptyMap(),
    @Json(name = "peak_bins") val peakBins: List<String> = emptyList(),
    @Json(name = "peak_goals") val peakGoals: Int? = null,
    @Json(name = "late_share_pct") val lateSharePct: Int? = null,
    @Json(name = "first_half_pct") val firstHalfPct: Int? = null,
    val goals: List<Int> = emptyList(),
    @Json(name = "logo_url") val logoUrl: String? = null
)

@JsonClass(generateAdapter = false)
data class FcTeamsResponse(
    val league: String? = null,
    val season: String? = null,
    val teams: List<FcTeamListItemDto> = emptyList(),
    val attribution: String? = null
)

@JsonClass(generateAdapter = false)
data class FcTeamListItemDto(
    val team: String,
    val slug: String? = null,
    val position: Int? = null,
    val points: Int? = null,
    val played: Int? = null,
    @Json(name = "logo_url") val logoUrl: String? = null
)

@JsonClass(generateAdapter = false)
data class FcTeamPageResponse(
    val team: String,
    val slug: String? = null,
    val league: String? = null,
    val season: String? = null,
    val seasons: List<String> = emptyList(),
    @Json(name = "api_team_name") val apiTeamName: String? = null,
    @Json(name = "logo_url") val logoUrl: String? = null,
    val ranking: FcTableRowDto? = null,
    val matches: List<FcMatchDto> = emptyList(),
    @Json(name = "time_bins") val timeBins: List<String> = emptyList(),
    @Json(name = "goal_bins") val goalBins: List<Int> = emptyList(),
    @Json(name = "first_goal_bins") val firstGoalBins: List<FcFirstGoalBinDto> = emptyList(),
    val stats: FcTeamStatsDto? = null,
    val attribution: String? = null
)

@JsonClass(generateAdapter = false)
data class FcFirstGoalBinDto(
    val time: String,
    val count: Int = 0
)

@JsonClass(generateAdapter = false)
data class FcTeamStatsDto(
    @Json(name = "matches_with_stats") val matchesWithStats: Int? = null,
    @Json(name = "shots_avg") val shotsAvg: Double? = null,
    @Json(name = "shots_on_target_avg") val shotsOnTargetAvg: Double? = null,
    @Json(name = "shots_against_avg") val shotsAgainstAvg: Double? = null,
    @Json(name = "possession_avg") val possessionAvg: Double? = null,
    @Json(name = "corners_avg") val cornersAvg: Double? = null,
    @Json(name = "xg_avg") val xgAvg: Double? = null,
    @Json(name = "xg_against_avg") val xgAgainstAvg: Double? = null
)

@JsonClass(generateAdapter = false)
data class FcMatchDetailResponse(
    val match: FcMatchDetailDto? = null,
    val attribution: String? = null
)

/** model_predictions block of /matches/{slug}/ — DC v2 model output (verified live). */
@JsonClass(generateAdapter = false)
data class FcModelPredictionsDto(
    @Json(name = "dc_v2") val dcV2: FcDcModelDto? = null
)

@JsonClass(generateAdapter = false)
data class FcDcModelDto(
    val raw: FcProbsDto? = null,
    val calibrated: FcProbsDto? = null,
    val model: String? = null,
    @Json(name = "computed_at") val computedAt: String? = null,
    @Json(name = "data_status") val dataStatus: FcDataStatusDto? = null,
    @Json(name = "effective_matches") val effectiveMatches: FcEffectiveMatchesDto? = null
)

@JsonClass(generateAdapter = false)
data class FcProbsDto(
    val home: Double? = null,
    val away: Double? = null,
    @Json(name = "btts_yes") val bttsYes: Double? = null,
    @Json(name = "over_0.5") val over05: Double? = null,
    @Json(name = "over_1.5") val over15: Double? = null,
    @Json(name = "over_2.5") val over25: Double? = null,
    @Json(name = "over_3.5") val over35: Double? = null,
    @Json(name = "over_4.5") val over45: Double? = null,
    @Json(name = "ht_over_0.5") val htOver05: Double? = null,
    @Json(name = "ht_over_1.5") val htOver15: Double? = null,
    @Json(name = "expected_home_goals") val expectedHomeGoals: Double? = null,
    @Json(name = "expected_away_goals") val expectedAwayGoals: Double? = null
)

@JsonClass(generateAdapter = false)
data class FcDataStatusDto(
    val stale: Boolean? = null,
    @Json(name = "matches_behind") val matchesBehind: Int? = null
)

@JsonClass(generateAdapter = false)
data class FcEffectiveMatchesDto(
    val home: Double? = null,
    val away: Double? = null
)

@JsonClass(generateAdapter = false)
data class FcMatchDetailDto(
    val id: Long? = null,
    val label: String? = null,
    val league: String? = null,
    @Json(name = "real_league_name") val realLeagueName: String? = null,
    val country: String? = null,
    val season: String? = null,
    @Json(name = "match_date") val matchDate: String? = null,
    val time: String? = null,
    @Json(name = "match_datetime") val matchDatetime: String? = null,
    @Json(name = "home_team") val homeTeam: String? = null,
    @Json(name = "away_team") val awayTeam: String? = null,
    @Json(name = "home_score") val homeScore: Int? = null,
    @Json(name = "away_score") val awayScore: Int? = null,
    @Json(name = "ht_result") val htResult: String? = null,
    @Json(name = "match_status") val matchStatus: String? = null,
    val slug: String? = null,
    @Json(name = "home_team_logo") val homeTeamLogo: String? = null,
    @Json(name = "away_team_logo") val awayTeamLogo: String? = null,
    @Json(name = "model_predictions") val modelPredictions: FcModelPredictionsDto? = null,
    @Json(name = "prediction_probability") val predictionProbability: Double? = null,
    @Json(name = "match_minute") val matchMinute: Int? = null
)

@JsonClass(generateAdapter = false)
data class FcTrackRecordResponse(
    @Json(name = "track_record") val trackRecord: FcTrackRecordDto? = null,
    val attribution: String? = null
)

@JsonClass(generateAdapter = false)
data class FcTrackRecordDto(
    @Json(name = "signals_only") val signalsOnly: Boolean? = null,
    val summary: FcTrackSummaryDto? = null,
    @Json(name = "by_market") val byMarket: Map<String, FcMarketRecordDto> = emptyMap(),
    @Json(name = "by_market_all") val byMarketAll: Map<String, FcMarketRecordDto> = emptyMap(),
    val recent: List<FcTrackPickDto> = emptyList(),
    val pending: Int? = null,
    val accuracy: FcTrackAccuracyDto? = null,
    val daily: List<FcTrackDailyDto> = emptyList(),
    @Json(name = "daily_by_family") val dailyByFamily: List<FcTrackDailyFamilyDto> = emptyList(),
    @Json(name = "policy_markers") val policyMarkers: List<FcPolicyMarkerDto> = emptyList()
)

@JsonClass(generateAdapter = false)
data class FcTrackDailyDto(
    val date: String? = null,
    val n: Int? = null,
    val won: Int? = null,
    val pl: Double? = null,
    @Json(name = "cum_pl") val cumPl: Double? = null
)

@JsonClass(generateAdapter = false)
data class FcTrackDailyFamilyDto(
    val date: String? = null,
    @Json(name = "cum_goals") val cumGoals: Double? = null,
    @Json(name = "cum_1x2") val cum1x2: Double? = null,
    @Json(name = "cum_ou") val cumOu: Double? = null
)

@JsonClass(generateAdapter = false)
data class FcPolicyMarkerDto(
    val date: String? = null,
    val label: String? = null
)

@JsonClass(generateAdapter = false)
data class FcTrackSummaryDto(
    val n: Int? = null,
    val won: Int? = null,
    val lost: Int? = null,
    val void: Int? = null,
    val pl: Double? = null,
    @Json(name = "hit_rate") val hitRate: Double? = null
)

@JsonClass(generateAdapter = false)
data class FcMarketRecordDto(
    val n: Int? = null,
    val won: Int? = null,
    val lost: Int? = null,
    val void: Int? = null,
    val pl: Double? = null,
    @Json(name = "hit_rate") val hitRate: Double? = null
)

@JsonClass(generateAdapter = false)
data class FcTrackPickDto(
    val slug: String? = null,
    @Json(name = "home_team") val homeTeam: String? = null,
    @Json(name = "away_team") val awayTeam: String? = null,
    @Json(name = "real_league_name") val realLeagueName: String? = null,
    val country: String? = null,
    @Json(name = "match_date") val matchDate: String? = null,
    val market: String? = null,
    val side: String? = null,
    val line: Double? = null,
    @Json(name = "model_prob") val modelProb: Double? = null,
    @Json(name = "is_signal") val isSignal: Boolean? = null,
    val outcome: String? = null,
    val pl: Double? = null,
    @Json(name = "result_score") val resultScore: String? = null,
    @Json(name = "result_ht_score") val resultHtScore: String? = null
)

@JsonClass(generateAdapter = false)
data class FcTrackAccuracyDto(
    val n: Int? = null,
    val brier: Double? = null,
    val buckets: List<FcTrackBucketDto> = emptyList()
)

@JsonClass(generateAdapter = false)
data class FcTrackBucketDto(
    val lo: Double? = null,
    val hi: Double? = null,
    val n: Int? = null,
    @Json(name = "avg_prob") val avgProb: Double? = null,
    @Json(name = "hit_rate") val hitRate: Double? = null
)

@JsonClass(generateAdapter = false)
data class FcErrorResponse(
    val error: FcErrorDto? = null
)

@JsonClass(generateAdapter = false)
data class FcErrorDto(
    val code: String? = null,
    val message: String? = null
)
