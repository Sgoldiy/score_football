package com.footballpluse.footballapp.data.remote.uitslagen

import com.squareup.moshi.JsonClass

/**
 * Lenient DTOs for the uitslagen.live/footapi upstream (verified against the
 * 2026-09-29 captures in app/src/test/resources/uitslagen/ — see
 * docs/LIVE_FEED.md). Every field is nullable: the upstream adds/removes
 * fields freely, so parsing must never throw on shape drift.
 */
@JsonClass(generateAdapter = false)
data class UitslagenCountryFeed(
    val country: String? = null,
    val leagues: List<UitslagenLeagueFeed>? = null
)

@JsonClass(generateAdapter = false)
data class UitslagenLeagueFeed(
    val league: String? = null,
    val country: String? = null,
    val key: String? = null,
    val matches: List<UitslagenMatchRow>? = null
)

/** Superset row covering feed rows, fixtures_v2 rows and search-embedded matches. */
@JsonClass(generateAdapter = false)
data class UitslagenMatchRow(
    val id: String? = null,
    val date: String? = null,          // dd/MM/yyyy (UTC)
    val time: String? = null,          // HH:MM (UTC)
    val status: String? = null,        // "Not Started" | "FT" | "Postp." | "HH:MM" | numeric code
    val scoretime: String? = null,     // "1 - 0" or " - "
    val ht: String? = null,            // half-time score cell, " - " when none
    val localteam: String? = null,
    val visitorteam: String? = null,
    val localteam_org: String? = null,
    val visitorteam_org: String? = null,
    val gs_localteamid: String? = null,
    val gs_visitorteamid: String? = null,
    val leagueid: String? = null,
    val leaguename: String? = null,
    val leagueKey: String? = null,
    val filegroup: String? = null,     // country
    val stageRound: String? = null,
    val stageName: String? = null,
    val season: String? = null,
    val week: String? = null,
    val isET: Int? = null,
    val isOT: Int? = null,
    val injurytime: String? = null,
    val injuryminute: String? = null,
    val refereeId: String? = null,
    val venue: String? = null
)

@JsonClass(generateAdapter = false)
data class UitslagenEvent(
    val id: String? = null,
    val type: String? = null,          // "goal", "card", ...
    val team: String? = null,          // "localteam" | "visitorteam"
    val minute: String? = null,
    val extra_min: String? = null,
    val player: String? = null,
    val playerId: String? = null,
    val assist: String? = null,
    val assistid: String? = null,
    val result: String? = null         // "[1 - 0]"
)

@JsonClass(generateAdapter = false)
data class UitslagenLineups(
    val localteam: List<UitslagenLineupPlayer>? = null,
    val visitorteam: List<UitslagenLineupPlayer>? = null
)

@JsonClass(generateAdapter = false)
data class UitslagenLineupPlayer(
    val id: String? = null,
    val name: String? = null,
    val number: String? = null,
    val position: String? = null,
    val age: String? = null
)

@JsonClass(generateAdapter = false)
data class UitslagenStatsSummary(
    val total_localteam_won: Int? = null,
    val total_visitorteam_won: Int? = null,
    val total_draws: Int? = null,
    val total_localteam_scored: Int? = null,
    val total_visitorteam_scored: Int? = null
)

@JsonClass(generateAdapter = false)
data class UitslagenMatchDetail(
    val id: String? = null,
    val date: String? = null,
    val time: String? = null,
    val status: String? = null,
    val scoretime: String? = null,
    val ht: String? = null,
    val localteam: String? = null,
    val visitorteam: String? = null,
    val localteam_org: String? = null,
    val visitorteam_org: String? = null,
    val gs_localteamid: String? = null,
    val gs_visitorteamid: String? = null,
    val localteamid: String? = null,
    val visitorteamid: String? = null,
    val leagueid: String? = null,
    val leaguename: String? = null,
    val leagueKey: String? = null,
    val filegroup: String? = null,
    val stageRound: String? = null,
    val stageName: String? = null,
    val season: String? = null,
    val isET: Int? = null,
    val isOT: Int? = null,
    val injurytime: String? = null,
    val injuryminute: String? = null,
    val refereeId: String? = null,
    val venue: String? = null,
    val coachLocal: String? = null,
    val coachVisitor: String? = null,
    val localteamshape: String? = null,
    val visitorteamshape: String? = null,
    val events: List<UitslagenEvent>? = null,
    val lineups: UitslagenLineups? = null,
    val stats: UitslagenStatsSummary? = null,
    val h2hMatches: List<UitslagenMatchRow>? = null
)

@JsonClass(generateAdapter = false)
data class UitslagenFixturesBlock(
    val fixtures: List<UitslagenMatchRow>? = null,
    val table: UitslagenTable? = null,
    val topscorers: UitslagenTopScorers? = null
)

@JsonClass(generateAdapter = false)
data class UitslagenTable(
    val filegroup: String? = null,
    val leaguename: String? = null,
    val groups: List<UitslagenTableGroup>? = null
)

@JsonClass(generateAdapter = false)
data class UitslagenTableGroup(
    val group: String? = null,
    val country: String? = null,
    val league: String? = null,
    val season: String? = null,
    val teams: List<UitslagenTableRow>? = null
)

@JsonClass(generateAdapter = false)
data class UitslagenTableRow(
    val id_gs: String? = null,
    val team: String? = null,
    val position: String? = null,
    val points: String? = null,
    val totalWon: String? = null,
    val totalDraw: String? = null,
    val totalLost: String? = null,
    val totalGoalsFor: String? = null,
    val totalGoalsAgainst: String? = null,
    val goalDifference: String? = null,
    val recentForm: String? = null,
    val description: String? = null,
    val group: String? = null
)

/** Empty in the capture; kept ultra-lenient so a populated payload still parses. */
@JsonClass(generateAdapter = false)
data class UitslagenTopScorers(
    val country: String? = null,
    val league: String? = null,
    val tournaments: List<UitslagenTopScorerTournament>? = null
)

@JsonClass(generateAdapter = false)
data class UitslagenTopScorerTournament(
    val name: String? = null,
    val topscorers: List<UitslagenScorer>? = null
)

@JsonClass(generateAdapter = false)
data class UitslagenScorer(
    val id: String? = null,
    val name: String? = null,
    val goals: String? = null,
    val assists: String? = null,
    val team: String? = null,
    val teamid: String? = null,
    val penalty: String? = null
)

@JsonClass(generateAdapter = false)
data class UitslagenSearchResponse(
    val query: String? = null,
    val lastUpdated: String? = null,
    val result: List<UitslagenSearchHit>? = null
)

@JsonClass(generateAdapter = false)
data class UitslagenSearchHit(
    val type: String? = null,          // "t" = team, "p" = player
    val id_gs: String? = null,
    val name: String? = null,
    val country: String? = null,
    val popularity: Long? = null,
    val score: Double? = null,
    val matches: List<UitslagenMatchRow>? = null
)

@JsonClass(generateAdapter = false)
data class UitslagenTeamPage(
    val id_gs: String? = null,
    val teamname: String? = null,
    val fullname: String? = null,
    val country: String? = null,
    val founded: String? = null,
    val venue: String? = null,
    val venuecity: String? = null,
    val venuecapacity: String? = null,
    val venuesurface: String? = null,
    val venueaddress: String? = null,
    val coach: String? = null,
    val coachnationality: String? = null,
    val shape: String? = null,
    val squad: List<UitslagenSquadPlayer>? = null,
    val fixtures: List<UitslagenMatchRow>? = null
)

@JsonClass(generateAdapter = false)
data class UitslagenSquadPlayer(
    val id: String? = null,
    val name: String? = null,
    val number: String? = null,
    val age: String? = null,
    val position: String? = null,
    val injured: String? = null,
    val minutes: String? = null,
    val appearances: String? = null,
    val goals: String? = null,
    val yellowcards: String? = null,
    val redcards: String? = null,
    val isCaptain: String? = null,
    val rating: String? = null
)

@JsonClass(generateAdapter = false)
data class UitslagenPlayerPage(
    val id: String? = null,
    val name: String? = null,
    val firstname: String? = null,
    val lastname: String? = null,
    val commonname: String? = null,
    val age: String? = null,
    val birthdate: String? = null,
    val birthcountry: String? = null,
    val birthplace: String? = null,
    val nationality: String? = null,
    val height: String? = null,
    val weight: String? = null,
    val position: String? = null,
    val preferredFoot: String? = null,
    val marketvalue: String? = null,
    val team: String? = null,
    val teamid: String? = null
)
