package com.example.footballapp.domain.model

data class Match(
    val id: Int,
    val date: String,
    val timestamp: Long,
    val status: MatchStatus,
    val elapsed: Int?,
    val league: LeagueInfo,
    val homeTeam: TeamInfo,
    val awayTeam: TeamInfo,
    val homeScore: Int?,
    val awayScore: Int?,
    val isLive: Boolean
)

data class MatchStatus(
    val long: String,
    val short: String,
    val elapsed: Int?
)

data class LeagueInfo(
    val id: Int,
    val name: String,
    val logo: String?,
    val country: String?,
    val flag: String?,
    val season: Int?
)

data class TeamInfo(
    val id: Int,
    val name: String,
    val logo: String?,
    val winner: Boolean? = null
)

data class StandingItem(
    val rank: Int,
    val team: TeamInfo,
    val points: Int,
    val goalsDiff: Int,
    val played: Int,
    val win: Int,
    val draw: Int,
    val lose: Int,
    val goalsFor: Int,
    val goalsAgainst: Int,
    val form: String?
)

data class MatchDetail(
    val match: Match,
    val events: List<MatchEvent>,
    val lineups: MatchLineups?,
    val stats: List<MatchStat>,
    val players: List<PlayerMatchStats>,
    val prediction: MatchPrediction? = null,
    val odds: List<MatchOdd> = emptyList(),
    val injuries: List<MatchInjury> = emptyList(),
    val headToHead: List<Match> = emptyList(),
    val venue: VenueInfo? = null,
    val referee: String? = null
)

data class MatchPrediction(
    val advice: String?,
    val winnerId: Int?,
    val winnerName: String?,
    val homePercent: String?,
    val drawPercent: String?,
    val awayPercent: String?
)

data class MatchOdd(
    val bookmaker: String,
    val label: String,
    val values: List<OddValue>
)

data class OddValue(
    val value: String,
    val odd: String
)

data class MatchInjury(
    val playerId: Int?,
    val playerName: String?,
    val teamId: Int,
    val type: String?,
    val reason: String?
)

data class TeamDetail(
    val info: TeamInfo,
    val venue: VenueInfo?,
    val stats: TeamStats?,
    val squad: List<SquadMember>,
    val coaches: List<CoachInfo>,
    val transfers: List<TransferRecord>
)

data class VenueInfo(
    val id: Int?,
    val name: String?,
    val city: String?,
    val capacity: Int?,
    val image: String?
)

data class TeamStats(
    val form: String?,
    val played: Int,
    val wins: Int,
    val draws: Int,
    val loses: Int,
    val goalsFor: Int,
    val goalsAgainst: Int
)

data class SquadMember(
    val id: Int,
    val name: String,
    val position: String?,
    val number: Int?,
    val photo: String?
)

data class TransferRecord(
    val player: String,
    val date: String,
    val type: String,
    val teamIn: String,
    val teamOut: String,
    val playerId: Int = 0,
    val playerPhotoUrl: String? = null,
    val teamInLogoUrl: String? = null,
    val teamOutLogoUrl: String? = null
)

data class PlayerDetail(
    val info: PlayerInfo,
    val stats: List<PlayerStatDetail>,
    val trophies: List<PlayerTrophyInfo>,
    val sidelined: List<PlayerInjuryInfo>
)

data class PlayerInfo(
    val id: Int,
    val name: String,
    val firstname: String?,
    val lastname: String?,
    val age: Int?,
    val nationality: String?,
    val height: String?,
    val weight: String?,
    val photo: String?
)

data class PlayerStatDetail(
    val team: TeamInfo,
    val league: LeagueInfo,
    val appearances: Int,
    val goals: Int,
    val assists: Int,
    val rating: String?
)

data class PlayerTrophyInfo(
    val league: String,
    val country: String,
    val season: String,
    val place: String
)

data class PlayerInjuryInfo(
    val type: String,
    val start: String,
    val end: String?
)

data class MatchEvent(
    val time: Int,
    val extraTime: Int?,
    val teamId: Int,
    val playerName: String?,
    val assistName: String?,
    val type: String,
    val detail: String
)

data class MatchLineups(
    val home: TeamLineup,
    val away: TeamLineup
)

data class TeamLineup(
    val team: TeamInfo,
    val formation: String?,
    val startXI: List<LineupPlayer>,
    val substitutes: List<LineupPlayer>,
    val coach: CoachInfo?
)

data class LineupPlayer(
    val id: Int,
    val name: String,
    val number: Int,
    val position: String,
    val grid: String?
)

data class CoachInfo(
    val id: Int,
    val name: String,
    val photo: String?
)

data class MatchStat(
    val teamId: Int,
    val type: String,
    val value: String
)

data class PlayerMatchStats(
    val teamId: Int,
    val players: List<PlayerPerformance>
)

data class PlayerPerformance(
    val id: Int,
    val name: String,
    val photo: String?,
    val rating: String?,
    val position: String,
    val goals: Int,
    val assists: Int
)
