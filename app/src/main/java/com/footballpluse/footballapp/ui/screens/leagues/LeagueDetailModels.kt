package com.footballpluse.footballapp.ui.screens.leagues

enum class LeagueTab(val label: String) {
    STANDINGS("Standings"),
    FIXTURES("Fixtures"),
    PLAYER_STATS("Player Stats"),
    SEASON_STATS("Stats"),
    H2H("H2H")
}

enum class MatchStatusUi { LIVE, COMPLETED, UPCOMING }

enum class StatCategory(val label: String) {
    GOALS("\u26BD Goals"),
    ASSISTS("\uD83C\uDFAF Assists"),
    XG("\uD83D\uDCCA xG"),
    CLEAN_SHEETS("\uD83D\uDEE1 Clean Sheets"),
    CARDS("\uD83D\uDFE8 Cards"),
    FORM("\uD83D\uDD25 Form"),
    SHOTS("\uD83D\uDC5F Shots"),
    RATING("\uD83C\uDF96 Rating")
}

data class TeamUiModel(
    val id: Int,
    val name: String,
    val logo: String?
)

data class StandingRowUiModel(
    val rank: Int,
    val team: TeamUiModel,
    val points: Int,
    val goalsDiff: Int,
    val played: Int,
    val win: Int,
    val draw: Int,
    val lose: Int,
    val goalsFor: Int,
    val goalsAgainst: Int,
    val form: String?,
    val homePlayed: Int = 0,
    val homeWon: Int = 0,
    val homeDraw: Int = 0,
    val homeLost: Int = 0,
    val homeGoalsFor: Int = 0,
    val homeGoalsAgainst: Int = 0,
    val awayPlayed: Int = 0,
    val awayWon: Int = 0,
    val awayDraw: Int = 0,
    val awayLost: Int = 0,
    val awayGoalsFor: Int = 0,
    val awayGoalsAgainst: Int = 0
)

data class FixtureUiModel(
    val id: String,
    val homeTeam: TeamUiModel,
    val awayTeam: TeamUiModel,
    val homeScore: Int?,
    val awayScore: Int?,
    val status: MatchStatusUi,
    val minute: Int?,
    val goalEvents: List<GoalEvent>,
    val yellowCards: Int,
    val redCards: Int,
    val attendance: Int?,
    val kickoffTime: String?
)

data class GoalEvent(
    val minute: Int,
    val playerName: String,
    val teamId: Int,
    val isHome: Boolean
)

data class PlayerStatUiModel(
    val rank: Int,
    val playerName: String,
    val clubName: String,
    val avatarUrl: String?,
    val statValue: Int,
    val secondaryStatLabel: String,
    val progressFraction: Float
)

data class SeasonStatsUiModel(
    val totalGoals: Int,
    val avgGoalsPerGame: Float,
    val mostCommonScoreline: String,
    val totalRedCards: Int,
    val totalYellowCards: Int,
    val biggestWin: String,
    val goalsByMinuteBand: List<GoalBand>,
    val bestAttack: Pair<String, Int>,
    val bestDefense: Pair<String, Int>,
    val homeWinPct: Float,
    val awayWinPct: Float,
    val drawPct: Float,
    val formTable: FormTableData
)

data class GoalBand(
    val label: String,
    val count: Int
)

data class FormTableData(
    val inForm: List<FormTeamRow>,
    val outOfForm: List<FormTeamRow>
)

data class FormTeamRow(
    val teamName: String,
    val form: String,
    val pointsGained: Int
)

data class H2HUiModel(
    val teamAWins: Int,
    val teamBWins: Int,
    val draws: Int,
    val lastMeetings: List<PastMeeting>,
    val comparisonStats: List<ComparisonStat>
)

data class PastMeeting(
    val date: String,
    val score: String,
    val competition: String,
    val winnerId: Int?
)

data class ComparisonStat(
    val label: String,
    val teamAValue: Float,
    val teamBValue: Float,
    val teamADisplay: String,
    val teamBDisplay: String
)

data class MatchdaySummaryData(
    val totalGoals: Int = 0,
    val cleanSheets: Int = 0,
    val avgGoals: Float = 0f,
    val mostCardsTeam: String = ""
)
