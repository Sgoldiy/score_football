package com.footballpluse.footballapp.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * DTOs for the FootballCharts backend (https://footballcharts-backend.onrender.com).
 * Shapes verified live on 2026-09-28. Every response carries "attribution" at the
 * top level; wrapped payloads differ per endpoint as modelled below.
 */
@JsonClass(generateAdapter = false)
data class FcLeaguesResponse(
    val leagues: List<FcLeagueDto> = emptyList(),
    val count: Int? = null,
    @Json(name = "season_window") val seasonWindow: String? = null,
    val attribution: String? = null
)

@JsonClass(generateAdapter = false)
data class FcLeagueDto(
    val league: String,
    val name: String,
    val country: String,
    val seasons: List<String> = emptyList(),
    val url: String? = null
)

@JsonClass(generateAdapter = false)
data class FcTableResponse(
    val league: String,
    val season: String? = null,
    val view: String? = null,
    @Json(name = "season_state") val seasonState: String? = null,
    @Json(name = "current_season") val currentSeason: String? = null,
    val table: List<FcTableRowDto> = emptyList(),
    val attribution: String? = null
)

@JsonClass(generateAdapter = false)
data class FcTableRowDto(
    val id: Int? = null,
    val team: String,
    val position: Int,
    val played: Int = 0,
    val won: Int = 0,
    val drawn: Int = 0,   // missing in "goals" view
    val lost: Int = 0,
    @Json(name = "goals_for") val goalsFor: Int? = null,
    @Json(name = "goals_against") val goalsAgainst: Int? = null,
    @Json(name = "goal_difference") val goalDifference: Int? = null,
    val points: Int = 0,
    @Json(name = "last_5_form") val last5Form: String? = null,
    @Json(name = "last_5_points") val last5Points: Int? = null,
    @Json(name = "biggest_win") val biggestWin: String? = null,
    @Json(name = "biggest_loss") val biggestLoss: String? = null,
    @Json(name = "expected_points") val expectedPoints: Double? = null,
    @Json(name = "luck_difference") val luckDifference: Double? = null,
    @Json(name = "luck_category") val luckCategory: String? = null,
    @Json(name = "expected_position") val expectedPosition: Int? = null,
    @Json(name = "home_luck") val homeLuck: Double? = null,
    @Json(name = "away_luck") val awayLuck: Double? = null,
    @Json(name = "luckiest_result") val luckiestResult: String? = null,
    @Json(name = "unluckiest_result") val unluckiestResult: String? = null,
    @Json(name = "avg_goals_scored") val avgGoalsScored: Double? = null,
    @Json(name = "avg_goals_conceded") val avgGoalsConceded: Double? = null,
    @Json(name = "avg_total_goals") val avgTotalGoals: Double? = null,
    @Json(name = "clean_sheets") val cleanSheets: Int? = null,
    @Json(name = "failed_to_score") val failedToScore: Int? = null,
    @Json(name = "home_goals_avg") val homeGoalsAvg: Double? = null,
    @Json(name = "away_goals_avg") val awayGoalsAvg: Double? = null,
    @Json(name = "over_25_percentage") val over25Percentage: Double? = null,
    @Json(name = "under_25_percentage") val under25Percentage: Double? = null,
    @Json(name = "highest_scoring_match") val highestScoringMatch: String? = null,
    @Json(name = "lowest_scoring_match") val lowestScoringMatch: String? = null,
    @Json(name = "roi_backing") val roiBacking: Double? = null,
    @Json(name = "profit_backing") val profitBacking: Double? = null,
    @Json(name = "roi_category") val roiCategory: String? = null,
    @Json(name = "roi_backing_home") val roiBackingHome: Double? = null,
    @Json(name = "roi_backing_away") val roiBackingAway: Double? = null,
    @Json(name = "updated_at") val updatedAt: String? = null
)

@JsonClass(generateAdapter = false)
data class FcMatchesResponse(
    val league: String? = null,
    val season: String? = null,
    @Json(name = "season_state") val seasonState: String? = null,
    @Json(name = "current_season") val currentSeason: String? = null,
    val count: Int = 0,
    val matches: List<FcMatchDto> = emptyList(),
    val attribution: String? = null
)

/**
 * One match row. TWO live shapes exist and both must parse:
 *
 *  - /results/ rows: camelCase, numeric id, score/ht_result
 *    {"id":216791,"date":"2026-08-21","time":"20:00:00","homeTeam":"Arsenal",
 *     "awayTeam":"Coventry","score":"3:0","ht_result":"2:0",...}
 *  - /fixtures/ rows: snake_case, no numeric id, full slug + status
 *    {"slug":"algeria/ligue-1/2026-10-03-kabylie-vs-biskra","home_team":"Kabylie",
 *     "away_team":"Biskra","match_date":"2026-10-03","time":"15:00","status":"scheduled",
 *     "league":"algir1","country":"Algeria","real_league_name":"Ligue 1","model_predictions":{..}}
 */
@JsonClass(generateAdapter = false)
data class FcMatchDto(
    // results-shape fields
    val id: Long? = null,
    @Json(name = "game_index") val gameIndex: Int? = null,
    val date: String? = null,
    val time: String? = null,
    @Json(name = "homeTeam") val homeTeam: String? = null,
    @Json(name = "awayTeam") val awayTeam: String? = null,
    val score: String? = null,
    @Json(name = "ht_result") val htResult: String? = null,
    @Json(name = "first_goal_time") val firstGoalTime: Int? = null,
    @Json(name = "first_goal_time_extra") val firstGoalTimeExtra: Int? = null,
    val goalless: Boolean? = null,
    // Team-page match-log variant
    val home: String? = null,
    val away: String? = null,
    val venue: String? = null,
    val ht: String? = null,
    val outcome: String? = null,
    @Json(name = "match_slug") val matchSlug: String? = null,
    // fixtures-shape fields (verified live 2026-09-28)
    val slug: String? = null,
    @Json(name = "home_team") val homeTeamAlt: String? = null,
    @Json(name = "away_team") val awayTeamAlt: String? = null,
    @Json(name = "match_date") val matchDate: String? = null,
    val status: String? = null,
    val country: String? = null,
    @Json(name = "real_league_name") val realLeagueName: String? = null,
    @Json(name = "model_predictions") val modelPredictions: FcModelPredictionsDto? = null
)
