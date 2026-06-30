package com.footballpluse.footballapp.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ApiCountry(
    val country_id: String?,
    val country_name: String?,
    val country_logo: String?
)

@JsonClass(generateAdapter = true)
data class ApiLeague(
    val country_id: String?,
    val country_name: String?,
    val league_id: String?,
    val league_name: String?,
    val league_season: String?,
    val league_logo: String?,
    val country_logo: String?
)

@JsonClass(generateAdapter = true)
data class ApiTeam(
    val team_key: String?,
    val team_name: String?,
    val team_country: String?,
    val team_founded: String?,
    val team_badge: String?,
    val venue: ApiVenue?,
    val players: List<ApiPlayer>?,
    val coaches: List<ApiCoach>?
)

@JsonClass(generateAdapter = true)
data class ApiVenue(
    val venue_name: String?,
    val venue_address: String?,
    val venue_city: String?,
    val venue_capacity: String?,
    val venue_surface: String?
)

@JsonClass(generateAdapter = true)
data class ApiCoach(
    val coach_name: String?,
    val coach_country: String?,
    val coach_age: String?
)

@JsonClass(generateAdapter = true)
data class ApiPlayer(
    val player_key: Int?,
    val player_id: String?,
    val player_image: String?,
    val player_name: String?,
    val player_number: String?,
    val player_country: String?,
    val player_type: String?,
    val player_age: String?,
    val player_birthdate: String?,
    val player_match_played: String?,
    val player_goals: String?,
    val player_yellow_cards: String?,
    val player_red_cards: String?,
    val player_injured: String?,
    val player_substitute_out: String?,
    val player_substitutes_on_bench: String?,
    val player_assists: String?,
    val player_is_captain: String?,
    val player_shots_total: String?,
    val player_goals_conceded: String?,
    val player_fouls_committed: String?,
    val player_tackles: String?,
    val player_blocks: String?,
    val player_crosses_total: String?,
    val player_interceptions: String?,
    val player_clearances: String?,
    val player_dispossesed: String?,
    val player_saves: String?,
    val player_inside_box_saves: String?,
    val player_duels_total: String?,
    val player_duels_won: String?,
    val player_dribble_attempts: String?,
    val player_dribble_succ: String?,
    val player_pen_comm: String?,
    val player_pen_won: String?,
    val player_pen_scored: String?,
    val player_pen_missed: String?,
    val player_passes: String?,
    val player_passes_accuracy: String?,
    val player_key_passes: String?,
    val player_woordworks: String?,
    val player_rating: String?
)

@JsonClass(generateAdapter = true)
data class ApiStanding(
    val country_name: String?,
    val league_id: String?,
    val league_name: String?,
    val team_id: String?,
    val team_name: String?,
    @Json(name = "overall_league_position") val standing_place: String?,
    val standing_place_type: String? = null,
    val standing_group: String? = null,
    val standing_home: String? = null,
    val standing_away: String? = null,
    @Json(name = "overall_league_payed") val standing_total: String?,
    @Json(name = "overall_league_D") val standing_D: String?,
    @Json(name = "overall_league_W") val standing_W: String?,
    @Json(name = "overall_league_L") val standing_L: String?,
    val standing_PE: String? = null,
    @Json(name = "overall_league_PTS") val standing_PTS: String?,
    @Json(name = "overall_league_GF") val overall_GF: String? = null,
    @Json(name = "overall_league_GA") val overall_GA: String? = null,
    val team_badge: String?,
    val league_logo: String? = null,
    val fk_stage_key: String? = null,
    val stage_name: String? = null,
    @Json(name = "overall_promotion") val overallPromotion: String? = null
)

@JsonClass(generateAdapter = true)
data class ApiEvent(
    val match_id: String?,
    val country_id: String?,
    val country_name: String?,
    val league_id: String?,
    val league_name: String?,
    val match_date: String?,
    val match_status: String?,
    val match_time: String?,
    val match_hometeam_id: String?,
    val match_hometeam_name: String?,
    val match_hometeam_score: String?,
    val match_awayteam_id: String?,
    val match_awayteam_name: String?,
    val match_awayteam_score: String?,
    val match_hometeam_halftime_score: String?,
    val match_awayteam_halftime_score: String?,
    val match_hometeam_extra_score: String?,
    val match_awayteam_extra_score: String?,
    val match_hometeam_penalty_score: String?,
    val match_awayteam_penalty_score: String?,
    val match_hometeam_ft_score: String?,
    val match_awayteam_ft_score: String?,
    val match_hometeam_system: String?,
    val match_awayteam_system: String?,
    val match_live: String?,
    val match_round: String?,
    val match_stadium: String?,
    val match_referee: String?,
    val team_home_badge: String?,
    val team_away_badge: String?,
    val league_logo: String?,
    val goalscorer: List<ApiGoalScorer>?,
    val cards: List<ApiCard>?,
    val substitutions: ApiSubstitutions?,
    val statistics: List<ApiMatchStatistic>?,
    val lineup: ApiLineupWrapper?
)

@JsonClass(generateAdapter = true)
data class ApiGoalScorer(
    val time: String?,
    val home_scorer: String?,
    val score: String?,
    val away_scorer: String?
)

@JsonClass(generateAdapter = true)
data class ApiCard(
    val time: String?,
    val home_fault: String?,
    val card: String?,
    val away_fault: String?,
    val info: String?,
    val info_time: String?
)

@JsonClass(generateAdapter = true)
data class ApiSubstitutions(
    val home: List<ApiSubstitution>?,
    val away: List<ApiSubstitution>?
)

@JsonClass(generateAdapter = true)
data class ApiSubstitution(
    val time: String?,
    val substitution_in: String?,
    val substitution_out: String?
)

@JsonClass(generateAdapter = true)
data class ApiMatchStatistic(
    val type: String?,
    val home: StatisticValue?,
    val away: StatisticValue?
)

@JsonClass(generateAdapter = true)
data class ApiLineupWrapper(
    val home: ApiTeamLineup?,
    val away: ApiTeamLineup?
)

@JsonClass(generateAdapter = true)
data class ApiTeamLineup(
    val starting_lineups: List<ApiLineupPlayer>?,
    val substitutes: List<ApiLineupPlayer>?,
    val coaches: List<ApiLineupCoach>?
)

@JsonClass(generateAdapter = true)
data class ApiLineupPlayer(
    val player: String?,
    val player_number: String?,
    val player_pos: String?,
    val player_key: String?
)

@JsonClass(generateAdapter = true)
data class ApiLineupCoach(
    val coach_name: String?,
    val coach_country: String?,
    val coach_age: String?
)

@JsonClass(generateAdapter = true)
data class StatisticValue(
    val display: String,
    val numeric: Float?
)

@JsonClass(generateAdapter = true)
data class ApiOdd(
    val bookmaker: String?,
    @Json(name = "1") val homeOdd: String?,
    @Json(name = "X") val drawOdd: String?,
    @Json(name = "2") val awayOdd: String?
)

@JsonClass(generateAdapter = true)
data class ApiTopScorer(
    val player_name: String?,
    val team_name: String?,
    val goals: String?,
    val assists: String?,
    val penalty_goals: String?,
    @Json(name = "player_key") val player_id: Int? = null,
    @Json(name = "team_key") val team_id: String? = null
)

@JsonClass(generateAdapter = true)
data class ApiPrediction(
    val homeWin: String?,
    val draw: String?,
    val awayWin: String?
)

// Keep existing wrapper types needed by domain
@JsonClass(generateAdapter = true)
data class ApiResponse<T>(
    val response: T
)

// ─── Legacy types kept for ViewModel compatibility (mapped from new API models) ───

@JsonClass(generateAdapter = true)
data class PlayerProfileStatisticsResponse(
    val player: Player?,
    val statistics: List<PlayerStatistics>?
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
    val photo: String?,
    val type: String?,
    val reason: String?
)

@JsonClass(generateAdapter = true)
data class PlayerBirth(
    val date: String?,
    val place: String?,
    val country: String?
)

@JsonClass(generateAdapter = true)
data class PlayerStatistics(
    val player: Player? = null,
    val team: Team?,
    val league: League?,
    val games: PlayerGames?,
    val offsides: Int?,
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
    @Json(name = "appearences") val appearances: Int?,
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
    val elapsed: Int?,
    val extra: Int?
)

@JsonClass(generateAdapter = true)
data class Venue(
    val id: Int?,
    val name: String?,
    val address: String?,
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
    val score: FixtureScore?,
    val events: List<FixtureEvent>?,
    val lineups: List<FixtureLineup>?,
    val statistics: List<FixtureTeamStatistics>?,
    val players: List<FixturePlayerStatisticsResponse>?
)

@JsonClass(generateAdapter = true)
data class FixtureEvent(
    val time: EventTime?,
    val team: EventTeam?,
    val player: EventPlayer?,
    val assist: EventPlayer?,
    val type: String?,
    val detail: String?,
    val comments: String?
)

@JsonClass(generateAdapter = true)
data class EventTime(
    val elapsed: Int?,
    val extra: Int?
)

@JsonClass(generateAdapter = true)
data class EventTeam(
    val id: Int?,
    val name: String?,
    val logo: String?
)

@JsonClass(generateAdapter = true)
data class EventPlayer(
    val id: Int?,
    val name: String?
)

@JsonClass(generateAdapter = true)
data class FixtureTeamStatistics(
    val team: FixtureTeam?,
    val statistics: List<StatisticItem>?
)

@JsonClass(generateAdapter = true)
data class StatisticItem(
    val type: String?,
    val value: StatisticValue?
)

@JsonClass(generateAdapter = true)
data class FixtureLineup(
    val team: FixtureTeam?,
    val coach: LineupCoach?,
    val formation: String?,
    val startXI: List<LineupPlayerWrapper>?,
    val substitutes: List<LineupPlayerWrapper>?
)

@JsonClass(generateAdapter = true)
data class LineupCoach(
    val id: Int?,
    val name: String?,
    val photo: String?
)

@JsonClass(generateAdapter = true)
data class LineupPlayerWrapper(
    val player: LineupPlayer?
)

@JsonClass(generateAdapter = true)
data class LineupPlayer(
    val id: Int?,
    val name: String?,
    val number: Int?,
    val pos: String?,
    val grid: String?
)

@JsonClass(generateAdapter = true)
data class FixturePlayerStatisticsResponse(
    val team: FixtureTeam?,
    val players: List<FixturePlayerEntry>?
)

@JsonClass(generateAdapter = true)
data class FixturePlayerEntry(
    val player: Player?,
    val statistics: List<PlayerStatistics>?
)

@JsonClass(generateAdapter = true)
data class TeamInfoResponse(
    val team: Team?,
    val venue: Venue?
)

@JsonClass(generateAdapter = true)
data class SquadResponse(
    val team: FixtureTeam?,
    val players: List<SquadPlayer>?
)

@JsonClass(generateAdapter = true)
data class SquadPlayer(
    val id: Int?,
    val name: String?,
    val age: Int?,
    val number: Int?,
    val position: String?,
    val photo: String?
)

@JsonClass(generateAdapter = true)
data class Coach(
    val id: Int?,
    val name: String?,
    val firstname: String?,
    val lastname: String?,
    val age: Int?,
    val birth: PlayerBirth?,
    val nationality: String?,
    val height: String?,
    val weight: String?,
    val photo: String?,
    val team: Team?,
    val career: List<CoachCareer>?
)

@JsonClass(generateAdapter = true)
data class CoachCareer(
    val team: Team?,
    val start: String?,
    val end: String?
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
    val winner: Boolean?,
    val update: String?,
    val colors: TeamKitColors?
)

@JsonClass(generateAdapter = true)
data class TeamKitColors(
    val player: KitColor?,
    val goalkeeper: KitColor?
)

@JsonClass(generateAdapter = true)
data class KitColor(
    val primary: String?,
    val number: String?,
    val border: String?
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
    val type: String?,
    val country: String?,
    val logo: String?,
    val flag: String?,
    val season: Int?,
    val round: String?,
    val standings: Boolean?
)

@JsonClass(generateAdapter = true)
data class LeagueResponse(
    val league: League?,
    val country: Country?,
    val seasons: List<Season>?,
    val liveCount: Int = 0,
    val todayCount: Int = 0,
    val isFavorited: Boolean = false
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
data class TeamStatistics(
    val league: League?,
    val team: Team?,
    val form: String?,
    val fixtures: TeamFixturesStats?,
    val goals: TeamGoalsStats?,
    val biggest: TeamBiggestStats?,
    val clean_sheet: FixtureCount?,
    val failed_to_score: FixtureCount?,
    val penalty: TeamPenaltyStats?,
    val lineups: List<TeamLineupStat>?,
    val cards: TeamCardsByMinute?
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
    val average: FixtureAverage?,
    val minute: Map<String, PercentageTotal?>?,
    val under_over: Map<String, UnderOverTotal?>?
)

@JsonClass(generateAdapter = true)
data class FixtureAverage(
    val home: String?,
    val away: String?,
    val total: String?
)

@JsonClass(generateAdapter = true)
data class TeamBiggestStats(
    val streak: TeamStreak?,
    val wins: HomeAwayString?,
    val loses: HomeAwayString?,
    val goals: TeamBiggestGoals?
)

@JsonClass(generateAdapter = true)
data class TeamStreak(
    val wins: Int?,
    val draws: Int?,
    val loses: Int?
)

@JsonClass(generateAdapter = true)
data class HomeAwayString(
    val home: String?,
    val away: String?
)

@JsonClass(generateAdapter = true)
data class TeamBiggestGoals(
    @Json(name = "for") val goalsFor: HomeAwayInt?,
    val against: HomeAwayInt?
)

@JsonClass(generateAdapter = true)
data class HomeAwayInt(
    val home: Int?,
    val away: Int?
)

@JsonClass(generateAdapter = true)
data class PercentageTotal(
    val total: Int?,
    val percentage: String?
)

@JsonClass(generateAdapter = true)
data class UnderOverTotal(
    val over: Int?,
    val under: Int?
)

@JsonClass(generateAdapter = true)
data class TeamPenaltyStats(
    val scored: PercentageTotal?,
    val missed: PercentageTotal?,
    val total: Int?
)

@JsonClass(generateAdapter = true)
data class TeamLineupStat(
    val formation: String?,
    val played: Int?
)

@JsonClass(generateAdapter = true)
data class TeamCardsByMinute(
    val yellow: Map<String, PercentageTotal?>?,
    val red: Map<String, PercentageTotal?>?
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
data class Country(
    val name: String,
    val code: String?,
    val flag: String?
)

@JsonClass(generateAdapter = true)
data class Transfer(
    val player: PlayerBrief?,
    val update: String?,
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
    val fixture: InjuryFixture?,
    val league: League?
)

@JsonClass(generateAdapter = true)
data class FixtureBrief(
    val id: Int?
)

@JsonClass(generateAdapter = true)
data class InjuryFixture(
    val id: Int?,
    val timezone: String?,
    val date: String?,
    val timestamp: Long?
)

@JsonClass(generateAdapter = true)
data class Prediction(
    val predictions: PredictionDetail?,
    val league: League?,
    val teams: PredictionTeams?,
    val comparison: PredictionComparison?,
    val h2h: List<FixtureResponse>?
)

@JsonClass(generateAdapter = true)
data class PredictionDetail(
    val winner: PredictionWinner?,
    val win_or_draw: Boolean?,
    val under_over: String?,
    val goals: PredictionGoals?,
    val advice: String?,
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
    val played: Int?,
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
    val goals: TeamGoalsStats?,
    val biggest: TeamBiggestStats?,
    val clean_sheet: FixtureCount?,
    val failed_to_score: FixtureCount?,
    val penalty: TeamPenaltyStats?,
    val lineups: List<TeamLineupStat>?,
    val cards: TeamCardsByMinute?
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
data class PlayerTrophy(
    val league: String?,
    val country: String?,
    val season: String?,
    val place: String?
)

@JsonClass(generateAdapter = true)
data class PlayerSidelined(
    val type: String?,
    val start: String?,
    val end: String?
)

@JsonClass(generateAdapter = true)
data class Timezone(
    val timezone: String
)
