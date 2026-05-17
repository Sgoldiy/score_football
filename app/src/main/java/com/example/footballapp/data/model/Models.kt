package com.example.footballapp.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Fixture(
    val id: Int,
    val referee: String?,
    val timezone: String?,
    val date: String?,
    val timestamp: Long?,
    val periods: Periods?,
    val venue: Venue?,
    val status: FixtureStatus?
)

@JsonClass(generateAdapter = true)
data class Periods(
    val first: Int?,
    val second: Int?
)

@JsonClass(generateAdapter = true)
data class FixtureStatus(
    val long: String?,
    val short: String?,
    val elapsed: Int?
)

@JsonClass(generateAdapter = true)
data class Venue(
    val id: Int?,
    val name: String?,
    val city: String?,
    val capacity: Int?,
    val surface: String?,
    val image: String?
)

@JsonClass(generateAdapter = true)
data class FixtureResponse(
    val fixture: Fixture?,
    val league: League?,
    val teams: FixtureTeams?,
    val goals: FixtureGoals?,
    val score: FixtureScore?
)

@JsonClass(generateAdapter = true)
data class FixtureTeams(
    val home: FixtureTeam?,
    val away: FixtureTeam?
)

@JsonClass(generateAdapter = true)
data class FixtureTeam(
    val id: Int?,
    val name: String?,
    val logo: String?,
    val winner: Boolean?
)

@JsonClass(generateAdapter = true)
data class FixtureGoals(
    val home: Int?,
    val away: Int?
)

@JsonClass(generateAdapter = true)
data class FixtureScore(
    val halftime: FixtureGoals?,
    val fulltime: FixtureGoals?,
    val extratime: FixtureGoals?,
    val penalty: FixtureGoals?
)

@JsonClass(generateAdapter = true)
data class League(
    val id: Int,
    val name: String?,
    val country: String?,
    val logo: String?,
    val flag: String?,
    val season: Int?,
    val round: String?
)

@JsonClass(generateAdapter = true)
data class Season(
    val year: Int,
    val start: String?,
    val end: String?,
    val current: Boolean?,
    val coverage: SeasonCoverage?
)

@JsonClass(generateAdapter = true)
data class SeasonCoverage(
    val fixtures: FixtureCoverage?,
    val standings: Boolean?,
    val players: Boolean?,
    val top_scorers: Boolean?,
    val top_assists: Boolean?,
    val top_cards: Boolean?,
    val injuries: Boolean?,
    val predictions: Boolean?,
    val odds: Boolean?
)

@JsonClass(generateAdapter = true)
data class FixtureCoverage(
    val events: Boolean?,
    val lineups: Boolean?,
    val statistics_fixtures: Boolean?,
    val statistics_players: Boolean?
)

@JsonClass(generateAdapter = true)
data class Team(
    val id: Int,
    val name: String?,
    val code: String?,
    val country: String?,
    val founded: Int?,
    val national: Boolean?,
    val logo: String?
)

@JsonClass(generateAdapter = true)
data class TeamStatistics(
    val league: League?,
    val team: Team?,
    val form: String?,
    val fixtures: TeamFixturesStats?,
    val goals: TeamGoalsStats?
)

@JsonClass(generateAdapter = true)
data class TeamFixturesStats(
    val played: FixtureCount?,
    val wins: FixtureCount?,
    val draws: FixtureCount?,
    val loses: FixtureCount?
)

@JsonClass(generateAdapter = true)
data class FixtureCount(
    val home: Int?,
    val away: Int?,
    val total: Int?
)

@JsonClass(generateAdapter = true)
data class TeamGoalsStats(
    @Json(name = "for") val goalsFor: GoalStatsDetail?,
    val against: GoalStatsDetail?
)

@JsonClass(generateAdapter = true)
data class GoalStatsDetail(
    val total: FixtureCount?,
    val average: FixtureAverage?
)

@JsonClass(generateAdapter = true)
data class FixtureAverage(
    val home: String?,
    val away: String?,
    val total: String?
)

@JsonClass(generateAdapter = true)
data class Player(
    val id: Int,
    val name: String?,
    val firstname: String?,
    val lastname: String?,
    val age: Int?,
    val birth: PlayerBirth?,
    val nationality: String?,
    val height: String?,
    val weight: String?,
    val injured: Boolean?,
    val photo: String?
)

@JsonClass(generateAdapter = true)
data class PlayerBirth(
    val date: String?,
    val place: String?,
    val country: String?
)

@JsonClass(generateAdapter = true)
data class PlayerStatistics(
    val team: Team?,
    val league: League?,
    val games: PlayerGames?,
    val substitutes: PlayerSubstitutes?,
    val shots: PlayerShots?,
    val goals: PlayerGoals?,
    val passes: PlayerPasses?,
    val tackles: PlayerTackles?,
    val duels: PlayerDuels?,
    val dribbles: PlayerDribbles?,
    val fouls: PlayerFouls?,
    val cards: PlayerCards?,
    val penalty: PlayerPenalty?
)

@JsonClass(generateAdapter = true)
data class PlayerGames(
    val appearances: Int?,
    val lineups: Int?,
    val minutes: Int?,
    val number: Int?,
    val position: String?,
    val rating: String?,
    val captain: Boolean?
)

@JsonClass(generateAdapter = true)
data class PlayerSubstitutes(
    val `in`: Int?,
    val out: Int?,
    val bench: Int?
)

@JsonClass(generateAdapter = true)
data class PlayerShots(
    val total: Int?,
    val on: Int?
)

@JsonClass(generateAdapter = true)
data class PlayerGoals(
    val total: Int?,
    val conceded: Int?,
    val assists: Int?,
    val saves: Int?
)

@JsonClass(generateAdapter = true)
data class PlayerPasses(
    val total: Int?,
    val key: Int?,
    val accuracy: Int?
)

@JsonClass(generateAdapter = true)
data class PlayerTackles(
    val total: Int?,
    val blocks: Int?,
    val interceptions: Int?
)

@JsonClass(generateAdapter = true)
data class PlayerDuels(
    val total: Int?,
    val won: Int?
)

@JsonClass(generateAdapter = true)
data class PlayerDribbles(
    val attempts: Int?,
    val success: Int?,
    val past: Int?
)

@JsonClass(generateAdapter = true)
data class PlayerFouls(
    val drawn: Int?,
    val committed: Int?
)

@JsonClass(generateAdapter = true)
data class PlayerCards(
    val yellow: Int?,
    val yellowred: Int?,
    val red: Int?
)

@JsonClass(generateAdapter = true)
data class PlayerPenalty(
    val won: Int?,
    val commited: Int?,
    val scored: Int?,
    val missed: Int?,
    val saved: Int?
)

@JsonClass(generateAdapter = true)
data class Standing(
    val league: LeagueStanding?
)

@JsonClass(generateAdapter = true)
data class LeagueStanding(
    val id: Int,
    val name: String?,
    val country: String?,
    val logo: String?,
    val flag: String?,
    val season: Int?,
    val standings: List<List<StandingRecord>>?
)

@JsonClass(generateAdapter = true)
data class StandingRecord(
    val rank: Int,
    val team: Team?,
    val points: Int?,
    val goalsDiff: Int?,
    val group: String?,
    val form: String?,
    val status: String?,
    val description: String?,
    val all: StandingGoals?,
    val home: StandingGoals?,
    val away: StandingGoals?,
    val update: String?
)

@JsonClass(generateAdapter = true)
data class StandingGoals(
    val played: Int?,
    val win: Int?,
    val draw: Int?,
    val lose: Int?,
    val goals: StandingGoalsDetail?
)

@JsonClass(generateAdapter = true)
data class StandingGoalsDetail(
    @Json(name = "for") val goalsFor: Int?,
    val against: Int?
)

@JsonClass(generateAdapter = true)
data class Transfer(
    val player: PlayerBrief?,
    val transfers: List<TransferEntry>?
)

@JsonClass(generateAdapter = true)
data class PlayerBrief(
    val id: Int,
    val name: String?
)

@JsonClass(generateAdapter = true)
data class TransferEntry(
    val date: String?,
    val type: String?,
    val teams: TransferTeams?
)

@JsonClass(generateAdapter = true)
data class TransferTeams(
    @Json(name = "in") val teamIn: Team?,
    val out: Team?
)

@JsonClass(generateAdapter = true)
data class Injury(
    val player: Player?,
    val team: Team?,
    val fixture: FixtureBrief?,
    val league: League?
)

@JsonClass(generateAdapter = true)
data class FixtureBrief(
    val id: Int?
)

@JsonClass(generateAdapter = true)
data class Prediction(
    val predictions: PredictionDetail?,
    val teams: PredictionTeams?,
    val comparison: PredictionComparison?
)

@JsonClass(generateAdapter = true)
data class PredictionDetail(
    val winner: PredictionWinner?,
    val win_or_draw: Boolean?,
    val under_over: String?,
    val goals: PredictionGoals?,
    val percent: PredictionPercent?
)

@JsonClass(generateAdapter = true)
data class PredictionWinner(
    val id: Int?,
    val name: String?,
    val comment: String?
)

@JsonClass(generateAdapter = true)
data class PredictionGoals(
    val home: String?,
    val away: String?
)

@JsonClass(generateAdapter = true)
data class PredictionPercent(
    val home: String?,
    val draw: String?,
    val away: String?
)

@JsonClass(generateAdapter = true)
data class PredictionTeams(
    val home: TeamPrediction?,
    val away: TeamPrediction?
)

@JsonClass(generateAdapter = true)
data class TeamPrediction(
    val id: Int,
    val name: String?,
    val logo: String?,
    val last_5: TeamPredictionLast5?,
    val league: TeamPredictionLeagueStats?
)

@JsonClass(generateAdapter = true)
data class TeamPredictionLast5(
    val form: String?,
    val att: String?,
    val def: String?,
    val goals: TeamPredictionLast5Goals?
)

@JsonClass(generateAdapter = true)
data class TeamPredictionLast5Goals(
    @Json(name = "for") val goalsFor: TeamPredictionGoalDetail?,
    val against: TeamPredictionGoalDetail?
)

@JsonClass(generateAdapter = true)
data class TeamPredictionGoalDetail(
    val total: Int?,
    val average: String?
)

@JsonClass(generateAdapter = true)
data class TeamPredictionLeagueStats(
    val form: String?,
    val fixtures: TeamFixturesStats?,
    val goals: TeamGoalsStats?
)

@JsonClass(generateAdapter = true)
data class PredictionComparison(
    val form: PredictionComparisonDetail?,
    val att: PredictionComparisonDetail?,
    val def: PredictionComparisonDetail?,
    val poisson_distribution: PredictionComparisonDetail?,
    val h2h: PredictionComparisonDetail?,
    val goals: PredictionComparisonDetail?,
    val total: PredictionComparisonDetail?
)

@JsonClass(generateAdapter = true)
data class PredictionComparisonDetail(
    val home: String?,
    val away: String?
)

@JsonClass(generateAdapter = true)
data class OddsResponse(
    val league: League?,
    val fixture: FixtureBrief?,
    val bookmakers: List<Bookmaker>?
)

@JsonClass(generateAdapter = true)
data class Bookmaker(
    val id: Int,
    val name: String?,
    val bets: List<OddsBet>?
)

@JsonClass(generateAdapter = true)
data class OddsBet(
    val id: Int,
    val name: String?,
    val values: List<OddsValue>?
)

@JsonClass(generateAdapter = true)
data class OddsValue(
    val value: String?,
    val odd: String?
)

@JsonClass(generateAdapter = true)
data class Country(
    val name: String,
    val code: String?,
    val flag: String?
)

@JsonClass(generateAdapter = true)
data class Timezone(
    val timezone: String
)
