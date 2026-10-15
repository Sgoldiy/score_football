package com.footballpluse.footballapp.data.remote.uitslagen

import com.footballpluse.footballapp.data.model.ApiCard
import com.footballpluse.footballapp.data.model.ApiCoach
import com.footballpluse.footballapp.data.model.ApiEvent
import com.footballpluse.footballapp.data.model.ApiGoalScorer
import com.footballpluse.footballapp.data.model.ApiLineupPlayer
import com.footballpluse.footballapp.data.model.ApiLineupWrapper
import com.footballpluse.footballapp.data.model.ApiPlayer
import com.footballpluse.footballapp.data.model.ApiStanding
import com.footballpluse.footballapp.data.model.ApiTeam
import com.footballpluse.footballapp.data.model.ApiTeamLineup
import com.footballpluse.footballapp.data.model.ApiTopScorer
import com.footballpluse.footballapp.data.model.ApiVenue
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * Pure mapping functions from footapi DTOs to the app's legacy Api models.
 * Kept free of Retrofit/Android deps so unit tests run against the captured
 * JSON payloads directly.
 */
object UitslagenMapper {

    // ─── shared helpers ───────────────────────────────────────────────────────

    /** Lowercase, strip diacritics/punctuation — for team-name comparisons. */
    fun normalize(name: String?): String {
        if (name.isNullOrBlank()) return ""
        val folded = java.text.Normalizer.normalize(name, java.text.Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
        return folded.lowercase(Locale.US).replace(Regex("[^a-z0-9]"), "")
    }

    fun parseScoreCell(cell: String?): Pair<Int, Int>? = UitslagenStatus.parseScore(cell)

    private fun scoreText(pair: Pair<Int, Int>?): String? = pair?.let { "${it.first} - ${it.second}" }

    private fun htPair(row: UitslagenMatchRow): Pair<Int, Int>? =
        row.ht?.takeIf { it.trim() != "-" }?.let { parseScoreCell(it) }

    /** True when the row date lies inside the [fromIso, toIso] window. Supports dd/MM/yyyy and yyyy-MM-dd. */
    fun inRangeUtc(rowDate: String?, fromIso: String?, toIso: String?): Boolean {
        if (rowDate.isNullOrBlank()) return true // keep rows without a date
        val row = parseAnyUtc(rowDate) ?: return true
        val from = fromIso?.let { parseAnyUtc(it) }
        val to = toIso?.let { parseAnyUtc(it) }?.let {
            // End of day (23:59:59.999 UTC) so matches played on `to` date aren't excluded
            java.util.Date(it.time + 86_399_999L)
        }
        if (from != null && row.before(from)) return false
        if (to != null && row.after(to)) return false
        return true
    }

    private fun parseAnyUtc(value: String): java.util.Date? {
        val trimmed = value.trim()
        val pattern = if (trimmed.contains("/")) "dd/MM/yyyy" else "yyyy-MM-dd"
        return parseUtc(trimmed.take(10), pattern)
    }

    private fun parseUtc(value: String, pattern: String) = try {
        SimpleDateFormat(pattern, Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
            .parse(value)
    } catch (_: Exception) {
        null
    }

    // ─── feed/fixture row -> ApiEvent ─────────────────────────────────────────

    fun rowToApiEvent(
        row: UitslagenMatchRow,
        leagueIdOverride: String? = null,
        leagueNameOverride: String? = null,
        nowUtcMs: Long = System.currentTimeMillis()
    ): ApiEvent {
        val kickoff = UitslagenStatus.kickoffUtcMillis(row.date, row.time)
        val phase = UitslagenStatus.decode(row.status, kickoff, nowUtcMs)
        val score = parseScoreCell(row.scoretime)
        val ht = htPair(row)
        val lId = leagueIdOverride ?: row.leagueid
        val lName = leagueNameOverride ?: row.leaguename
        val resolvedCountry = UitslagenLeagues.countryFor(
            leagueId = lId?.toIntOrNull(),
            leagueName = lName,
            rawCountry = row.filegroup
        )
        return ApiEvent(
            match_id = row.id,
            country_name = resolvedCountry,
            league_id = lId,
            league_name = lName,
            match_date = row.date,               // kept in upstream dd/MM/yyyy format
            match_status = when {
                phase.label == "HT" -> "HT"
                phase.isLive && phase.minute != null -> "${phase.minute}"
                phase.isLive && row.status?.toIntOrNull() != null -> row.status
                phase.isLive -> "1"
                else -> phase.label
            },
            match_live = if (phase.isLive) "1" else null,
            match_hometeam_id = row.gs_localteamid,
            match_hometeam_name = row.localteam,
            match_hometeam_score = score?.first?.toString(),
            match_awayteam_id = row.gs_visitorteamid,
            match_awayteam_name = row.visitorteam,
            match_awayteam_score = score?.second?.toString(),
            match_hometeam_halftime_score = ht?.first?.toString(),
            match_awayteam_halftime_score = ht?.second?.toString(),
            league_year = row.season,
            match_round = row.week,
            team_home_badge = UitslagenApiService.teamLogoUrl(row.gs_localteamid),
            team_away_badge = UitslagenApiService.teamLogoUrl(row.gs_visitorteamid)
        )
    }

    /** Map every row of every league of a country feed. */
    fun flattenFeeds(
        feeds: List<UitslagenCountryFeed>,
        nowUtcMs: Long = System.currentTimeMillis()
    ): List<ApiEvent> {
        val out = mutableListOf<ApiEvent>()
        for (country in feeds) {
            for (lg in country.leagues.orEmpty()) {
                for (row in lg.matches.orEmpty()) {
                    out += rowToApiEvent(row, nowUtcMs = nowUtcMs)
                }
            }
        }
        return out
    }

    // ─── match detail -> enriched ApiEvent ────────────────────────────────────

    fun detailToApiEvent(detail: UitslagenMatchDetail, nowUtcMs: Long = System.currentTimeMillis()): ApiEvent {
        val base = detail.let { d ->
            rowToApiEvent(
                UitslagenMatchRow(
                    id = d.id, date = d.date, time = d.time, status = d.status,
                    scoretime = d.scoretime, ht = d.ht,
                    localteam = d.localteam, visitorteam = d.visitorteam,
                    gs_localteamid = d.gs_localteamid, gs_visitorteamid = d.gs_visitorteamid,
                    leagueid = d.leagueid, leaguename = d.leaguename, leagueKey = d.leagueKey,
                    filegroup = d.filegroup, stageRound = d.stageRound, stageName = d.stageName,
                    season = d.season, isET = d.isET, isOT = d.isOT
                ),
                nowUtcMs = nowUtcMs
            )
        }
        val goals = detail.events.orEmpty().filter { it.type.equals("goal", true) }
        val cards = detail.events.orEmpty().filter { it.type.equals("card", true) }
        return base.copy(
            match_stadium = detail.venue,
            match_referee = detail.refereeId?.toIntOrNull()?.takeIf { it > 0 }?.let { "Referee #$it" },
            match_hometeam_system = detail.localteamshape,
            match_awayteam_system = detail.visitorteamshape,
            goalscorer = goals.map { e ->
                ApiGoalScorer(
                    time = e.minute,
                    home_scorer = e.player?.takeIf { it.isNotBlank() && e.team.equals("localteam", true) },
                    away_scorer = e.player?.takeIf { it.isNotBlank() && e.team.equals("visitorteam", true) },
                    score = e.result?.trim('[', ']'),
                    info = e.extra_min?.takeIf { it.isNotBlank() },
                    home_assist = e.assist?.takeIf { it.isNotBlank() && e.team.equals("localteam", true) },
                    away_assist = e.assist?.takeIf { it.isNotBlank() && e.team.equals("visitorteam", true) }
                )
            },
            cards = cards.map { e ->
                ApiCard(
                    time = e.minute,
                    home_fault = e.player?.takeIf { it.isNotBlank() && e.team.equals("localteam", true) },
                    away_fault = e.player?.takeIf { it.isNotBlank() && e.team.equals("visitorteam", true) },
                    card = null, // upstream event rows carry no card colour
                    info = e.result?.trim('[', ']'),
                    info_time = null
                )
            },
            lineup = lineupWrapper(detail.lineups)
        )
    }

    fun lineupWrapper(lineups: UitslagenLineups?): ApiLineupWrapper? {
        if (lineups == null) return null
        val home = lineups.localteam.orEmpty()
        val away = lineups.visitorteam.orEmpty()
        if (home.isEmpty() && away.isEmpty()) return null
        return ApiLineupWrapper(
            home = ApiTeamLineup(
                starting_lineups = home.map(::lineupPlayer),
                substitutes = emptyList(),
                coaches = emptyList()
            ),
            away = ApiTeamLineup(
                starting_lineups = away.map(::lineupPlayer),
                substitutes = emptyList(),
                coaches = emptyList()
            )
        )
    }

    private fun lineupPlayer(p: UitslagenLineupPlayer) = ApiLineupPlayer(
        player = p.name,
        player_number = p.number,
        player_pos = p.position,
        player_key = p.id,
        player_image = UitslagenApiService.playerImageUrl(p.id)
    )

    // ─── league block -> standings / teams / top scorers ──────────────────────

    fun tableToStandings(
        table: UitslagenTable?,
        leagueId: String?,
        leagueName: String?
    ): List<ApiStanding> {
        val groups = table?.groups.orEmpty()
        if (groups.isEmpty()) return emptyList()
        val countryFallback = table?.filegroup
        val leagueNameFallback = table?.leaguename
        val lId = leagueId?.toIntOrNull()
        val lName = leagueName ?: leagueNameFallback
        return groups.flatMap { g ->
            val resolvedCountry = UitslagenLeagues.countryFor(
                leagueId = lId,
                leagueName = lName ?: g.league,
                rawCountry = g.country ?: countryFallback
            )
            g.teams.orEmpty().mapIndexed { _, t ->
                val won = t.totalWon?.toIntOrNull() ?: 0
                val drawn = t.totalDraw?.toIntOrNull() ?: 0
                val lost = t.totalLost?.toIntOrNull() ?: 0
                ApiStanding(
                    country_name = resolvedCountry,
                    league_id = leagueId,
                    league_name = lName ?: g.league,
                    team_id = t.id_gs,
                    team_name = t.team,
                    standing_place = t.position,
                    standing_group = t.group ?: g.group,
                    standing_total = (won + drawn + lost).toString(),
                    standing_W = t.totalWon,
                    standing_D = t.totalDraw,
                    standing_L = t.totalLost,
                    standing_PTS = t.points,
                    overall_form = t.recentForm,
                    overall_GF = t.totalGoalsFor,
                    overall_GA = t.totalGoalsAgainst,
                    team_badge = UitslagenApiService.teamLogoUrl(t.id_gs)
                )
            }
        }
    }

    fun blockToTeams(block: UitslagenFixturesBlock?, leagueKey: String?): List<ApiTeam> {
        val tableFilegroup = block?.table?.filegroup
        val lId = leagueKey?.let { UitslagenLeagues.idForKey(it) }
        val resolvedCountry = UitslagenLeagues.countryFor(
            leagueId = lId,
            leagueName = block?.table?.leaguename,
            rawCountry = tableFilegroup
        )
        val fromTable = block?.table?.groups.orEmpty()
            .flatMap { it.teams.orEmpty() }
            .map { t ->
                ApiTeam(
                    team_key = t.id_gs,
                    team_name = t.team,
                    team_country = resolvedCountry,
                    team_founded = null,
                    team_badge = UitslagenApiService.teamLogoUrl(t.id_gs),
                    venue = null, players = null, coaches = null
                )
            }
            .distinctBy { it.team_key ?: it.team_name }
        if (fromTable.isNotEmpty()) return fromTable
        // Fallback: unique teams from the fixture rows (no ids on all payloads).
        return block?.fixtures.orEmpty()
            .flatMap { listOf(it.localteam to it.gs_localteamid, it.visitorteam to it.gs_visitorteamid) }
            .filter { it.first != null }
            .distinctBy { normalize(it.first) }
            .map { (name, id) ->
                ApiTeam(
                    team_key = id, team_name = name, team_country = null, team_founded = null,
                    team_badge = UitslagenApiService.teamLogoUrl(id),
                    venue = null, players = null, coaches = null
                )
            }
    }

    fun topScorers(block: UitslagenFixturesBlock?): List<ApiTopScorer> =
        block?.topscorers?.tournaments.orEmpty()
            .flatMap { t -> t.players.orEmpty().ifEmpty { t.topscorers.orEmpty() } }
            .mapIndexed { idx, s ->
                val pId = s.id
                val tId = s.team_id_gs ?: s.teamid
                ApiTopScorer(
                    player_name = s.name,
                    team_name = s.team,
                    goals = s.goals,
                    assists = s.assists,
                    penalty_goals = s.penalty_goals ?: s.penalty,
                    player_id = pId?.toLongOrNull(),
                    team_id = tId,
                    player_image = UitslagenApiService.playerImageUrl(pId),
                    team_badge = UitslagenApiService.teamLogoUrl(tId),
                    player_place = s.pos ?: (idx + 1).toString()
                )
            }

    // ─── team / player pages ──────────────────────────────────────────────────

    fun teamPageToApiTeam(page: UitslagenTeamPage): ApiTeam {
        val trophies = page.trophies.orEmpty().map { t ->
            com.footballpluse.footballapp.domain.model.ClubTrophy(
                league = t.league.orEmpty(),
                country = t.country,
                status = t.status ?: "Winner",
                count = t.count?.toIntOrNull() ?: 1,
                seasons = t.seasons
            )
        }

        val incomingTransfers = page.transfers?.`in`.orEmpty().map {
            com.footballpluse.footballapp.domain.model.TransferRecord(
                player = it.name.orEmpty(),
                date = it.date.orEmpty(),
                type = it.type?.takeIf { type -> type.isNotBlank() && type != "-" } ?: "Signing",
                teamIn = page.teamname.orEmpty(),
                teamOut = it.from?.takeIf { f -> f.isNotBlank() } ?: "Unknown",
                playerId = it.id?.toIntOrNull() ?: 0,
                playerPhotoUrl = UitslagenApiService.playerImageUrl(it.id),
                teamInLogoUrl = UitslagenApiService.teamLogoUrl(page.id_gs),
                teamOutLogoUrl = UitslagenApiService.teamLogoUrl(it.teamid)
            )
        }
        val outgoingTransfers = page.transfers?.out.orEmpty().map {
            com.footballpluse.footballapp.domain.model.TransferRecord(
                player = it.name.orEmpty(),
                date = it.date.orEmpty(),
                type = it.type?.takeIf { type -> type.isNotBlank() && type != "-" } ?: "Departure",
                teamIn = it.to?.takeIf { t -> t.isNotBlank() } ?: "Unknown",
                teamOut = page.teamname.orEmpty(),
                playerId = it.id?.toIntOrNull() ?: 0,
                playerPhotoUrl = UitslagenApiService.playerImageUrl(it.id),
                teamInLogoUrl = UitslagenApiService.teamLogoUrl(it.teamid),
                teamOutLogoUrl = UitslagenApiService.teamLogoUrl(page.id_gs)
            )
        }
        val transfers = incomingTransfers + outgoingTransfers

        val st = page.stats
        val wTot = st?.winTotal?.toIntOrNull() ?: 0
        val dTot = st?.drawTotal?.toIntOrNull() ?: 0
        val lTot = st?.lostTotal?.toIntOrNull() ?: 0
        val gfTot = st?.goalsForTotal?.toIntOrNull() ?: 0
        val gaTot = st?.goalsAgainstTotal?.toIntOrNull() ?: 0
        val playedTot = wTot + dTot + lTot

        val teamStats = if (st != null || page.shape != null) {
            com.footballpluse.footballapp.domain.model.TeamStats(
                form = page.shape,
                played = playedTot,
                wins = wTot,
                draws = dTot,
                loses = lTot,
                goalsFor = gfTot,
                goalsAgainst = gaTot
            )
        } else null

        val detailedStats = st?.let {
            com.footballpluse.footballapp.domain.model.TeamDetailedStats(
                rank = it.rank,
                winTotal = wTot,
                winHome = it.winHome?.toIntOrNull() ?: 0,
                winAway = it.winAway?.toIntOrNull() ?: 0,
                drawTotal = dTot,
                drawHome = it.drawHome?.toIntOrNull() ?: 0,
                drawAway = it.drawAway?.toIntOrNull() ?: 0,
                lostTotal = lTot,
                lostHome = it.lostHome?.toIntOrNull() ?: 0,
                lostAway = it.lostAway?.toIntOrNull() ?: 0,
                goalsForTotal = gfTot,
                goalsForHome = it.goalsForHome?.toIntOrNull() ?: 0,
                goalsForAway = it.goalsForAway?.toIntOrNull() ?: 0,
                goalsAgainstTotal = gaTot,
                goalsAgainstHome = it.goalsAgainstHome?.toIntOrNull() ?: 0,
                goalsAgainstAway = it.goalsAgainstAway?.toIntOrNull() ?: 0,
                cleanSheetTotal = it.cleanSheetTotal?.toIntOrNull() ?: 0,
                cleanSheetHome = it.cleanSheetHome?.toIntOrNull() ?: 0,
                cleanSheetAway = it.cleanSheetAway?.toIntOrNull() ?: 0,
                avgGoalsScored = it.avgGoalsPerGameScoredTotal,
                avgGoalsConceded = it.avgGoalsPerGameConcededTotal,
                failedToScore = it.failedToScoreTotal?.toIntOrNull() ?: 0
            )
        }

        val resolvedTeamCountry = UitslagenLeagues.countryFor(
            leagueId = null,
            leagueName = null,
            rawCountry = page.country
        )
        return ApiTeam(
            team_key = page.id_gs,
            team_name = page.teamname,
            team_country = resolvedTeamCountry,
            team_founded = page.founded,
            team_badge = UitslagenApiService.teamLogoUrl(page.id_gs),
            venue = ApiVenue(
                venue_name = page.venue,
                venue_address = page.venueaddress,
                venue_city = page.venuecity,
                venue_capacity = page.venuecapacity,
                venue_surface = page.venuesurface
            ),
            players = page.squad.orEmpty().map { p ->
                ApiPlayer(
                    player_key = p.id?.toLongOrNull(),
                    player_id = p.id,
                    player_name = p.name,
                    player_number = p.number,
                    player_type = p.position,
                    player_age = p.age,
                    player_goals = p.goals,
                    player_yellow_cards = p.yellowcards,
                    player_red_cards = p.redcards,
                    player_injured = p.injured,
                    player_is_captain = p.isCaptain,
                    player_rating = p.rating,
                    player_match_played = p.appearances,
                    player_minutes = p.minutes,
                    player_image = UitslagenApiService.playerImageUrl(p.id),
                    player_country = null,
                    player_birthdate = null,
                    player_substitute_out = null,
                    player_substitutes_on_bench = null,
                    player_assists = null,
                    player_shots_total = null,
                    player_goals_conceded = null,
                    player_fouls_committed = null,
                    player_tackles = null,
                    player_blocks = null,
                    player_crosses_total = null,
                    player_interceptions = null,
                    player_clearances = null,
                    player_dispossesed = null,
                    player_saves = null,
                    player_inside_box_saves = null,
                    player_duels_total = null,
                    player_duels_won = null,
                    player_dribble_attempts = null,
                    player_dribble_succ = null,
                    player_pen_comm = null,
                    player_pen_won = null,
                    player_pen_scored = null,
                    player_pen_missed = null,
                    player_passes = null,
                    player_passes_accuracy = null,
                    player_key_passes = null,
                    player_woordworks = null
                )
            },
            coaches = listOfNotNull(
                page.coach?.takeIf { it.isNotBlank() }?.let {
                    ApiCoach(
                        coach_name = it,
                        coach_country = page.coachnationality,
                        coach_age = null,
                        coach_image = UitslagenApiService.coachImageUrl(page.coachid)
                    )
                }
            ),
            transfers = transfers,
            trophies = trophies,
            teamStats = teamStats,
            detailedStats = detailedStats
        )
    }

    fun playerPageToApiPlayer(page: UitslagenPlayerPage): ApiPlayer = ApiPlayer(
        player_key = page.id?.toLongOrNull(),
        player_id = page.id,
        player_name = page.commonname ?: page.name,
        player_country = page.nationality,
        player_type = page.position,
        player_age = page.age,
        player_birthdate = page.birthdate,
        team_name = page.team,
        team_key = page.teamid,
        player_image = UitslagenApiService.playerImageUrl(page.id),
        player_number = null,
        player_match_played = null,
        player_goals = null,
        player_yellow_cards = null,
        player_red_cards = null,
        player_injured = null,
        player_substitute_out = null,
        player_substitutes_on_bench = null,
        player_assists = null,
        player_is_captain = null,
        player_shots_total = null,
        player_goals_conceded = null,
        player_fouls_committed = null,
        player_tackles = null,
        player_blocks = null,
        player_crosses_total = null,
        player_interceptions = null,
        player_clearances = null,
        player_dispossesed = null,
        player_saves = null,
        player_inside_box_saves = null,
        player_duels_total = null,
        player_duels_won = null,
        player_dribble_attempts = null,
        player_dribble_succ = null,
        player_pen_comm = null,
        player_pen_won = null,
        player_pen_scored = null,
        player_pen_missed = null,
        player_passes = null,
        player_passes_accuracy = null,
        player_key_passes = null,
        player_woordworks = null,
        player_rating = null
    )

    // ─── search ───────────────────────────────────────────────────────────────

    fun searchTeams(resp: UitslagenSearchResponse): List<ApiTeam> =
        resp.result.orEmpty().filter { it.type == "t" }.map { hit ->
            ApiTeam(
                team_key = hit.id_gs,
                team_name = hit.name,
                team_country = hit.country,
                team_founded = null,
                team_badge = UitslagenApiService.teamLogoUrl(hit.id_gs),
                venue = null, players = null, coaches = null
            )
        }

    fun searchPlayers(resp: UitslagenSearchResponse): List<ApiPlayer> =
        resp.result.orEmpty().filter { it.type == "p" }.map { hit ->
            ApiPlayer(
                player_key = hit.id_gs?.toLongOrNull(),
                player_id = hit.id_gs,
                player_name = hit.name,
                player_country = hit.country,
                player_image = UitslagenApiService.playerImageUrl(hit.id_gs),
                player_number = null,
                player_goals = null,
                player_yellow_cards = null,
                player_red_cards = null,
                player_injured = null,
                player_type = null,
                player_age = null,
                player_birthdate = null,
                player_match_played = null,
                player_minutes = null,
                team_name = null,
                team_key = null,
                player_substitute_out = null,
                player_substitutes_on_bench = null,
                player_assists = null,
                player_is_captain = null,
                player_shots_total = null,
                player_goals_conceded = null,
                player_fouls_committed = null,
                player_tackles = null,
                player_blocks = null,
                player_crosses_total = null,
                player_interceptions = null,
                player_clearances = null,
                player_dispossesed = null,
                player_saves = null,
                player_inside_box_saves = null,
                player_duels_total = null,
                player_duels_won = null,
                player_dribble_attempts = null,
                player_dribble_succ = null,
                player_pen_comm = null,
                player_pen_won = null,
                player_pen_scored = null,
                player_pen_missed = null,
                player_passes = null,
                player_passes_accuracy = null,
                player_key_passes = null,
                player_woordworks = null,
                player_rating = null
            )
        }
}
