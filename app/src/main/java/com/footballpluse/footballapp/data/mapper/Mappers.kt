package com.footballpluse.footballapp.data.mapper

import com.footballpluse.footballapp.domain.model.LineupPlayer as DomainLineupPlayer
import com.footballpluse.footballapp.data.local.db.FixtureEntity
import com.footballpluse.footballapp.data.model.*
import com.footballpluse.footballapp.domain.model.*

// ─── New API model → Old model mappers ───
internal fun String?.toIntOr(def: Int = 0): Int = this?.toIntOrNull() ?: def

/** "45+2" -> 45, "90+4" -> 90, "13" -> 13 */
internal fun String?.minuteToElapsed(): Int? =
    this?.trim()?.takeWhile { it.isDigit() }?.toIntOrNull()

/** "45+4" -> 4; returns null when there is no stoppage part */
internal fun String?.stoppageMinute(): Int? =
    this?.takeIf { it.contains("+") }?.substringAfter("+")?.takeWhile { it.isDigit() }?.toIntOrNull()

private fun mapStatus(status: String?, matchLive: String?, elapsed: Int? = null): String = when {
    status.isNullOrEmpty() -> "NS"
    status == "Finished" -> "FT"
    status == "Not Started" -> "NS"
    status == "Halftime" || status == "Half Time" -> "HT"
    status == "Extra Time" -> "ET"
    status == "Penalties" -> "P"
    status == "Postponed" -> "PST"
    status == "Cancelled" -> "CAN"
    status == "Suspended" -> "SUS"
    status == "Interrupted" -> "INT"
    status == "After Extra Time" || status == "After ET" -> "AET"
    status == "After Penalties" || status == "After Pen." -> "AP"
    status == "Awarded" -> "AW"
    status == "In Play" -> "LIVE"
    matchLive == "1" -> "LIVE"
    // v3 API: in-play matches carry the minute as the status, e.g. "13", "45+2"
    status.firstOrNull()?.isDigit() == true -> if (elapsed != null) "LIVE" else "NS"
    else -> status.take(3).uppercase()
}

internal fun isLiveStatus(status: String?, matchLive: String?): Boolean =
    mapStatus(status, matchLive, status.minuteToElapsed()) == "LIVE" ||
        (status?.firstOrNull()?.isDigit() == true && status.minuteToElapsed() != null)

/** "2026/2027" -> 2026, "2026" -> 2026 */
internal fun String?.seasonToStartYear(): Int? =
    this?.takeWhile { it.isDigit() }?.toIntOrNull()

/**
 * Match timestamp from v3 API. match_date is "yyyy-MM-dd" and match_time is the
 * kickoff in Europe/Berlin local time. Combining them gives a correct UTC instant
 * (CET = UTC+1, CEST = UTC+2, DST handled automatically by the Europe/Berlin zone).
 */
internal fun eventTimestamp(date: String?, time: String?): Long {
    if (date.isNullOrBlank()) return 0L
    return try {
        val zone = java.time.ZoneId.of("Europe/Berlin")
        val d = java.time.LocalDate.parse(date.take(10))
        val (h, m) = if (time != null && time.contains(":")) {
            val parts = time.split(":")
            Pair(parts[0].toIntOrNull() ?: 0, parts[1].toIntOrNull() ?: 0)
        } else Pair(0, 0)
        d.atTime(h, m).atZone(zone).toInstant().toEpochMilli() / 1000L
    } catch (_: Exception) {
        try {
            java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                .parse(date.take(10))?.time?.div(1000L) ?: 0L
        } catch (_: Exception) { 0L }
    }
}

fun ApiEvent.toFixtureResponse(): FixtureResponse {
    val fixtureId = match_id.toIntOr(0)
    val homeId = match_hometeam_id.toIntOr(0)
    val awayId = match_awayteam_id.toIntOr(0)
    val timestamp = eventTimestamp(match_date, match_time)
    val elapsed = match_status.minuteToElapsed()
    val statusShort = mapStatus(match_status, match_live, elapsed)
    val isLive = statusShort == "LIVE"

    fun winner(isHome: Boolean): Boolean? {
        val hs = match_hometeam_score?.trim()?.toIntOrNull()
        val as_ = match_awayteam_score?.trim()?.toIntOrNull()
        if (hs == null || as_ == null) return null
        val mine = if (isHome) hs else as_
        val other = if (isHome) as_ else hs
        return if (mine > other && statusShort == "FT") true
        else if (other > mine && statusShort == "FT") false
        else null
    }

    return FixtureResponse(
        fixture = Fixture(
            id = fixtureId, referee = match_referee, timezone = null,
            date = match_date, timestamp = timestamp,
            periods = Periods(null, null),
            venue = Venue(id = null, name = match_stadium, address = null, city = null, capacity = null, surface = null, image = null),
            status = FixtureStatus(long = match_status ?: "", short = statusShort, elapsed = elapsed, extra = null)
        ),
        league = League(
            id = league_id.toIntOr(0), name = league_name, type = null, country = country_name,
            logo = league_logo, flag = null,
            season = league_year.seasonToStartYear(),
            round = match_round, standings = null
        ),
        teams = FixtureTeams(
            home = FixtureTeam(id = homeId, name = match_hometeam_name, logo = team_home_badge, winner = winner(true), update = null, colors = null),
            away = FixtureTeam(id = awayId, name = match_awayteam_name, logo = team_away_badge, winner = winner(false), update = null, colors = null)
        ),
        goals = FixtureGoals(home = match_hometeam_score?.trim()?.toIntOrNull(), away = match_awayteam_score?.trim()?.toIntOrNull()),
        score = FixtureScore(
            halftime = FixtureGoals(home = match_hometeam_halftime_score?.toIntOrNull(), away = match_awayteam_halftime_score?.toIntOrNull()),
            fulltime = FixtureGoals(
                home = match_hometeam_ft_score?.toIntOrNull() ?: match_hometeam_score?.trim()?.toIntOrNull(),
                away = match_awayteam_ft_score?.toIntOrNull() ?: match_awayteam_score?.trim()?.toIntOrNull()
            ),
            extratime = FixtureGoals(home = match_hometeam_extra_score?.toIntOrNull(), away = match_awayteam_extra_score?.toIntOrNull()),
            penalty = FixtureGoals(home = match_hometeam_penalty_score?.toIntOrNull(), away = match_awayteam_penalty_score?.toIntOrNull())
        ),
        events = (goalscorer?.map { it.toFixtureEvent(homeId, awayId) } ?: emptyList()) +
            (cards?.map { it.toFixtureEvent(homeId, awayId) } ?: emptyList()) +
            (substitutions?.home?.map { it.toFixtureEvent(homeId) } ?: emptyList()) +
            (substitutions?.away?.map { it.toFixtureEvent(awayId) } ?: emptyList()),
        lineups = lineup?.let { l ->
            listOfNotNull(
                l.home?.toFixtureLineup(homeId, match_hometeam_name, team_home_badge, match_hometeam_system),
                l.away?.toFixtureLineup(awayId, match_awayteam_name, team_away_badge, match_awayteam_system)
            )
        } ?: emptyList(),
        statistics = statistics?.map { it.toFixtureTeamStatistics(homeId, awayId) } ?: emptyList(),
        players = null
    )
}

internal fun ApiSubstitution.toFixtureEvent(teamId: Int): FixtureEvent {
    val players = substitution?.split("|") ?: emptyList()
    val outPlayer = players.getOrNull(0)?.trim()
    val inPlayer = players.getOrNull(1)?.trim()
    return FixtureEvent(
        time = EventTime(elapsed = time.minuteToElapsed(), extra = null),
        team = EventTeam(id = teamId, name = null, logo = null),
        player = EventPlayer(id = null, name = outPlayer),
        assist = EventPlayer(id = null, name = inPlayer),
        type = "subst",
        detail = "Substitution",
        comments = null
    )
}

fun List<ApiEvent>.toFixtureResponseList(): List<FixtureResponse> = map { it.toFixtureResponse() }

internal fun ApiGoalScorer.toFixtureEvent(homeId: Int, awayId: Int): FixtureEvent {
    val isHome = !home_scorer.isNullOrBlank()
    val scorerName = if (isHome) home_scorer else away_scorer
    val assistName = if (isHome) home_assist else away_assist
    val isPenalty = info?.contains("Penalty", ignoreCase = true) == true ||
            info?.contains("pen.", ignoreCase = true) == true
    val isOwnGoal = info?.contains("Own", ignoreCase = true) == true ||
            info?.contains("o.g.", ignoreCase = true) == true
    val extra = time.stoppageMinute()
    val detail = when {
        isOwnGoal -> "Own Goal"
        isPenalty -> "Penalty"
        else -> "Goal"
    }
    return FixtureEvent(
        time = EventTime(
            elapsed = time.minuteToElapsed() ?: 0,
            extra = extra
        ),
        team = EventTeam(id = if (isHome) homeId else awayId, name = null, logo = null),
        player = EventPlayer(id = null, name = scorerName),
        assist = EventPlayer(id = null, name = assistName),
        type = "Goal",
        detail = detail,
        comments = null
    )
}

internal fun ApiCard.toFixtureEvent(homeId: Int, awayId: Int): FixtureEvent {
    val isHome = !home_fault.isNullOrBlank()
    val cardDetail = card ?: "yellow card"
    return FixtureEvent(
        time = EventTime(
            elapsed = time.minuteToElapsed() ?: 0,
            extra = info_time.stoppageMinute()
        ),
        team = EventTeam(id = if (isHome) homeId else awayId, name = null, logo = null),
        player = EventPlayer(id = null, name = if (isHome) home_fault else away_fault),
        assist = null,
        type = "Card",
        detail = cardDetail.replaceFirstChar { it.uppercase() },
        comments = info
    )
}

internal fun ApiMatchStatistic.toFixtureTeamStatistics(homeId: Int, awayId: Int): FixtureTeamStatistics {
    return FixtureTeamStatistics(
        team = FixtureTeam(id = homeId, name = null, logo = null, winner = null, update = null, colors = null),
        statistics = listOf(
            StatisticItem(type = type, value = home),
            StatisticItem(type = "${type}_away", value = away)
        )
    )
}

/** Position digit -> tactical grid row. v3 positions: 1=GK, 2-5=DEF, 6-8=MID, 9-11=FWD */
internal fun lineupPositionToRow(pos: String?): Int = when (pos?.trim()?.toIntOrNull()) {
    1 -> 1
    in 2..5 -> 2
    in 6..8 -> 3
    in 9..11 -> 4
    else -> 4
}

internal fun ApiTeamLineup.toFixtureLineup(teamId: Int, teamName: String?, badge: String?, formation: String? = null): FixtureLineup {
    val startXI = starting_lineups ?: emptyList()
    // Count players per row so grid columns are correct ("row:col")
    val perRow = IntArray(6)
    val mappedStartXI = startXI.map { it.toLineupPlayerWrapper(perRow) }
    return FixtureLineup(
        team = FixtureTeam(id = teamId, name = teamName, logo = badge, winner = null, update = null, colors = null),
        coach = coaches?.firstOrNull()?.let { LineupCoach(id = it.player_key?.toIntOrNull(), name = it.player, photo = null) },
        formation = formation,
        startXI = mappedStartXI,
        substitutes = substitutes?.map { it.toLineupPlayerWrapper(null) } ?: emptyList()
    )
}

private fun ApiLineupPlayer.toLineupPlayerWrapper(rowCounter: IntArray?): LineupPlayerWrapper {
    val num = player_number?.toIntOrNull()
    val posDigit = player_pos?.trim()?.toIntOrNull()
    val row = lineupPositionToRow(player_pos)
    val col = if (rowCounter != null) {
        rowCounter[row] += 1
        rowCounter[row]
    } else 1
    val grid = if (rowCounter != null) "$row:$col" else null
    return LineupPlayerWrapper(
        player = LineupPlayer(
            id = player_key?.toIntOrNull() ?: 0,
            name = player ?: "",
            number = num ?: 0,
            pos = player_pos ?: "",
            grid = grid
        )
    )
}

fun List<ApiStanding>.toStanding(): Standing {
    val records = map { it.toStandingRecord() }
    val first = firstOrNull()
    return Standing(
        league = LeagueStanding(
            id = first?.league_id.toIntOr(0), name = first?.league_name,
            country = first?.country_name, logo = first?.league_logo,
            flag = null,
            season = null,
            standings = listOf(records.sortedBy { it.rank })
        )
    )
}

internal fun ApiStanding.toStandingRecord(): StandingRecord {
    val rank = standing_place.toIntOr(0)
    val wins = standing_W?.toIntOrNull()
    val draws = standing_D?.toIntOrNull()
    val loses = standing_L?.toIntOrNull()
    val played = standing_total?.toIntOrNull() ?: (wins?.let { w -> draws?.let { d -> loses?.let { l -> w + d + l } } } ?: 0)
    val goalsFor = overall_GF?.toIntOrNull()
    val goalsAgainst = overall_GA?.toIntOrNull()
    val goalsDiff = if (goalsFor != null && goalsAgainst != null) goalsFor - goalsAgainst else null

    val homeRecord = StandingGoals(
        played = home_league_payed?.toIntOrNull(),
        win = home_W?.toIntOrNull(),
        draw = home_D?.toIntOrNull(),
        lose = home_L?.toIntOrNull(),
        goals = StandingGoalsDetail(
            goalsFor = home_GF?.toIntOrNull(),
            against = home_GA?.toIntOrNull()
        )
    )
    val awayRecord = StandingGoals(
        played = away_league_payed?.toIntOrNull(),
        win = away_W?.toIntOrNull(),
        draw = away_D?.toIntOrNull(),
        lose = away_L?.toIntOrNull(),
        goals = StandingGoalsDetail(
            goalsFor = away_GF?.toIntOrNull(),
            against = away_GA?.toIntOrNull()
        )
    )

    return StandingRecord(
        rank = rank, team = Team(id = team_id.toIntOr(0), name = team_name, code = null, country = country_name,
            founded = null, national = null, logo = team_badge),
        points = standing_PTS?.toIntOrNull(), goalsDiff = goalsDiff, group = standing_group,
        form = overall_form, status = standing_place_type, description = overallPromotion,
        all = StandingGoals(played = played, win = wins, draw = draws, lose = loses,
            goals = StandingGoalsDetail(goalsFor = goalsFor, against = goalsAgainst)),
        home = homeRecord,
        away = awayRecord,
        update = null
    )
}

fun ApiLeague.toLeagueInfo(): LeagueInfo {
    return LeagueInfo(
        id = league_id.toIntOr(0),
        name = league_name ?: "",
        logo = league_logo,
        country = country_name,
        flag = country_logo,
        season = league_season.seasonToStartYear()
    )
}

fun ApiLeague.toLeagueResponse(): LeagueResponse {
    val seasonInt = league_season.seasonToStartYear() ?: java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
    val isCup = league_name?.contains("cup", ignoreCase = true) == true ||
            league_name?.contains("copa", ignoreCase = true) == true ||
            league_name?.contains("trophy", ignoreCase = true) == true ||
            league_name?.contains("champions league", ignoreCase = true) == true ||
            league_name?.contains("europa league", ignoreCase = true) == true ||
            league_name?.contains("conference league", ignoreCase = true) == true ||
            league_name?.contains("pokal", ignoreCase = true) == true ||
            league_name?.contains("copetta", ignoreCase = true) == true ||
            league_name?.contains("fa ", ignoreCase = true) == true
    val isInt = country_name == "World" || country_name == "Europe" || country_name.isNullOrBlank()
    return LeagueResponse(
        league = League(
            id = league_id.toIntOr(0),
            name = league_name,
            type = if (isCup) "Cup" else "League",
            country = country_name,
            logo = league_logo,
            flag = country_logo,
            season = seasonInt,
            round = null,
            standings = null
        ),
        country = Country(name = country_name ?: "", code = if (isInt) null else "country", flag = country_logo),
        seasons = listOf(Season(year = seasonInt, start = null, end = null, current = true, coverage = null))
    )
}

fun ApiTeam.toTeamInfoResponse(): TeamInfoResponse {
    return TeamInfoResponse(
        team = Team(id = team_key.toIntOr(0), name = team_name, code = null, country = team_country,
            founded = team_founded?.toIntOrNull(), national = null, logo = team_badge),
        venue = venue?.let { Venue(id = null, name = it.venue_name, address = it.venue_address,
            city = it.venue_city, capacity = it.venue_capacity?.toIntOrNull(), surface = it.venue_surface, image = null) }
    )
}

fun ApiPlayer.toPlayerProfileStatisticsResponse(): PlayerProfileStatisticsResponse {
    return PlayerProfileStatisticsResponse(
        player = Player(
            id = (player_key ?: 0).toInt(), name = player_name, firstname = null, lastname = null,
            age = player_age?.toIntOrNull(),
            birth = player_birthdate?.let { PlayerBirth(date = it, place = null, country = player_country) },
            nationality = player_country, height = null, weight = null,
            injured = player_injured?.equals("Yes", ignoreCase = true) ?: (player_injured == "1"),
            photo = player_image, type = player_type, reason = null
        ),
        statistics = listOf(
            PlayerStatistics(
                player = null,
                // get_players embeds team_name/team_key on the player object
                team = Team(id = team_key.toIntOr(0), name = team_name, code = null, country = null, founded = null, national = null, logo = null),
                league = null,
                games = PlayerGames(
                    appearances = player_match_played?.toIntOrNull(),
                    lineups = null,
                    minutes = player_minutes?.toIntOrNull(),
                    number = player_number?.toIntOrNull(),
                    position = player_type,
                    rating = player_rating?.takeIf { it.isNotBlank() } ?: "0.0",
                    captain = player_is_captain?.toIntOrNull()?.let { it > 0 }
                ),
                offsides = null,
                substitutes = PlayerSubstitutes(`in` = null, out = player_substitute_out?.toIntOrNull(), bench = player_substitutes_on_bench?.toIntOrNull()),
                shots = PlayerShots(total = player_shots_total?.toIntOrNull(), on = null),
                goals = PlayerGoals(total = player_goals?.toIntOrNull(), conceded = player_goals_conceded?.toIntOrNull(), assists = player_assists?.toIntOrNull(), saves = player_saves?.toIntOrNull()),
                passes = PlayerPasses(total = player_passes?.toIntOrNull(), key = player_key_passes?.toIntOrNull(), accuracy = player_passes_accuracy?.toIntOrNull()),
                tackles = PlayerTackles(total = player_tackles?.toIntOrNull(), blocks = player_blocks?.toIntOrNull(), interceptions = player_interceptions?.toIntOrNull()),
                duels = PlayerDuels(total = player_duels_total?.toIntOrNull(), won = player_duels_won?.toIntOrNull()),
                dribbles = PlayerDribbles(attempts = player_dribble_attempts?.toIntOrNull(), success = player_dribble_succ?.toIntOrNull(), past = null),
                fouls = PlayerFouls(drawn = null, committed = player_fouls_committed?.toIntOrNull()),
                cards = PlayerCards(yellow = player_yellow_cards?.toIntOrNull(), yellowred = null, red = player_red_cards?.toIntOrNull()),
                penalty = PlayerPenalty(won = player_pen_won?.toIntOrNull(), commited = player_pen_comm?.toIntOrNull(),
                    scored = player_pen_scored?.toIntOrNull(), missed = player_pen_missed?.toIntOrNull(), saved = null)
            )
        )
    )
}

fun ApiTopScorer.toPlayerProfileStatisticsResponse(): PlayerProfileStatisticsResponse {
    return PlayerProfileStatisticsResponse(
        player = Player(
            id = (player_id ?: 0).toInt(), name = player_name, firstname = null, lastname = null,
            age = null, birth = null, nationality = null, height = null, weight = null,
            injured = null, photo = player_image, type = null, reason = null
        ),
        statistics = listOf(
            PlayerStatistics(
                player = null,
                team = Team(id = team_id?.toIntOrNull() ?: 0, name = team_name, code = null, country = null,
                    founded = null, national = null, logo = team_badge),
                league = null,
                games = null, offsides = null, substitutes = null, shots = null,
                goals = PlayerGoals(total = goals?.toIntOrNull(), conceded = null, assists = assists?.toIntOrNull(), saves = null),
                passes = null, tackles = null, duels = null, dribbles = null, fouls = null, cards = null,
                penalty = PlayerPenalty(won = null, commited = null, scored = penalty_goals?.toIntOrNull(), missed = null, saved = null)
            )
        )
    )
}

fun ApiOdd.toOddsResponse(): OddsResponse {
    return OddsResponse(
        league = null, fixture = FixtureBrief(id = null),
        bookmakers = listOf(
            Bookmaker(id = 0, name = bookmaker, bets = listOf(
                OddsBet(id = 0, name = "Match Winner", values = listOf(
                    OddsValue(value = "Home", odd = homeOdd),
                    OddsValue(value = "Draw", odd = drawOdd),
                    OddsValue(value = "Away", odd = awayOdd)
                ))
            ))
        )
    )
}

fun ApiPrediction.toPrediction(): Prediction {
    return Prediction(
        predictions = PredictionDetail(
            winner = null, win_or_draw = null, under_over = null, goals = null,
            advice = advice,
            percent = PredictionPercent(home = homeWin, draw = draw, away = awayWin),
            extras = listOfNotNull(overLadder, xgSummary, modelNote).joinToString(" \u00b7 ").ifBlank { null }
        ),
        league = null, teams = null, comparison = null, h2h = null
    )
}

fun List<ApiPlayer>.toSquadPlayers(): List<SquadPlayer> = map {
    SquadPlayer(id = it.player_key?.toInt(), name = it.player_name, age = it.player_age?.toIntOrNull(),
        number = it.player_number?.toIntOrNull(), position = it.player_type, photo = it.player_image)
}

fun ApiMatchPlayerStatistic.toPlayerPerformance(): PlayerPerformance {
    return PlayerPerformance(
        id = player_key.toIntOr(0),
        name = player_name ?: "",
        photo = null, // Not available in match stats
        rating = player_rating,
        position = player_position ?: "",
        goals = player_goals.toIntOr(0),
        assists = player_assists.toIntOr(0)
    )
}

fun ApiTeam.toTeamDetail(standing: ApiStanding? = null): TeamDetail {
    val wins = standing?.standing_W?.toIntOrNull() ?: 0
    val draws = standing?.standing_D?.toIntOrNull() ?: 0
    val loses = standing?.standing_L?.toIntOrNull() ?: 0
    val stats = TeamStats(
        form = standing?.overall_form,
        played = standing?.standing_total?.toIntOrNull() ?: (wins + draws + loses),
        wins = wins,
        draws = draws,
        loses = loses,
        goalsFor = standing?.overall_GF?.toIntOrNull() ?: 0,
        goalsAgainst = standing?.overall_GA?.toIntOrNull() ?: 0
    )

    return TeamDetail(
        info = TeamInfo(
            id = team_key.toIntOr(0),
            name = team_name ?: "",
            logo = team_badge,
            country = team_country
        ),
        venue = venue?.let {
            VenueInfo(
                id = null,
                name = it.venue_name,
                city = it.venue_city,
                capacity = it.venue_capacity?.toIntOrNull(),
                image = null
            )
        },
        stats = stats,
        squad = players?.toSquadPlayers()?.map { it.toSquadMember() } ?: emptyList(),
        coaches = coaches?.toCoaches()?.map { CoachInfo(it.id ?: 0, it.name ?: "", it.photo) } ?: emptyList(),
        transfers = emptyList()
    )
}

fun PlayerProfileStatisticsResponse.toPlayerDetail(): PlayerDetail {
    return PlayerDetail(
        info = this.toPlayerInfo(),
        stats = this.statistics?.map { it.toPlayerStatDetail() } ?: emptyList(),
        trophies = emptyList(),
        sidelined = emptyList()
    )
}

fun List<ApiCoach>.toCoaches(): List<Coach> = map {
    Coach(
        id = it.coach_name?.hashCode(),
        name = it.coach_name,
        firstname = null,
        lastname = null,
        age = it.coach_age?.toIntOrNull(),
        birth = null,
        nationality = it.coach_country,
        height = null,
        weight = null,
        photo = null,
        team = null,
        career = null
    )
}

// ─── Existing mappers (old model → domain) ───

fun FixtureResponse.toMatch(): Match {
    return Match(
        id = fixture?.id ?: 0,
        date = fixture?.date ?: "",
        timestamp = fixture?.timestamp ?: 0L,
        status = MatchStatus(
            long = fixture?.status?.long ?: "",
            short = fixture?.status?.short ?: "",
            elapsed = fixture?.status?.elapsed
        ),
        elapsed = fixture?.status?.elapsed,
        league = LeagueInfo(
            id = league?.id ?: 0,
            name = league?.name ?: "",
            logo = league?.logo,
            country = league?.country,
            flag = league?.flag,
            season = league?.season
        ),
        homeTeam = TeamInfo(
            id = teams?.home?.id ?: 0,
            name = teams?.home?.name ?: "",
            logo = teams?.home?.logo,
            winner = teams?.home?.winner
        ),
        awayTeam = TeamInfo(
            id = teams?.away?.id ?: 0,
            name = teams?.away?.name ?: "",
            logo = teams?.away?.logo,
            winner = teams?.away?.winner
        ),
        homeScore = goals?.home,
        awayScore = goals?.away,
        isLive = fixture?.status?.short in listOf("1H", "2H", "HT", "ET", "BT", "P", "INT", "LIVE")
    )
}

fun FixtureResponse.toEntity(date: String): FixtureEntity {
    return FixtureEntity(
        id = fixture?.id ?: 0,
        date = date,
        leagueId = league?.id ?: 0,
        leagueName = league?.name ?: "",
        leagueLogo = league?.logo,
        homeTeamId = teams?.home?.id ?: 0,
        homeTeamName = teams?.home?.name ?: "",
        homeTeamLogo = teams?.home?.logo,
        awayTeamId = teams?.away?.id ?: 0,
        awayTeamName = teams?.away?.name ?: "",
        awayTeamLogo = teams?.away?.logo,
        homeScore = goals?.home,
        awayScore = goals?.away,
        statusShort = fixture?.status?.short,
        elapsed = fixture?.status?.elapsed,
        timestamp = fixture?.timestamp ?: 0L,
        isLive = fixture?.status?.short in listOf("1H", "2H", "HT", "ET", "BT", "P", "INT", "LIVE")
    )
}

fun FixtureEntity.toMatch(): Match {
    return Match(
        id = id,
        date = date,
        timestamp = timestamp,
        status = MatchStatus(long = "", short = statusShort ?: "", elapsed = elapsed),
        elapsed = elapsed,
        league = LeagueInfo(id = leagueId, name = leagueName, logo = leagueLogo, country = null, flag = null, season = null),
        homeTeam = TeamInfo(id = homeTeamId, name = homeTeamName, logo = homeTeamLogo),
        awayTeam = TeamInfo(id = awayTeamId, name = awayTeamName, logo = awayTeamLogo),
        homeScore = homeScore,
        awayScore = awayScore,
        isLive = isLive
    )
}

fun FixtureEvent.toMatchEvent(): MatchEvent {
    return MatchEvent(
        time = time?.elapsed ?: 0,
        extraTime = time?.extra,
        teamId = team?.id ?: 0,
        playerName = player?.name,
        assistName = assist?.name,
        type = type ?: "",
        detail = detail ?: ""
    )
}

fun FixtureLineup.toMatchLineups(away: FixtureLineup?): MatchLineups {
    return MatchLineups(
        home = this.toTeamLineup(),
        away = away?.toTeamLineup() ?: TeamLineup(
            TeamInfo(0, "", null), null, emptyList(), emptyList(), null
        )
    )
}

fun FixtureLineup.toTeamLineup(): TeamLineup {
    return TeamLineup(
        team = TeamInfo(team?.id ?: 0, team?.name ?: "", team?.logo),
        formation = formation,
        startXI = startXI?.map { it.toLineupPlayer() } ?: emptyList(),
        substitutes = substitutes?.map { it.toLineupPlayer() } ?: emptyList(),
        coach = coach?.let { CoachInfo(it.id ?: 0, it.name ?: "", it.photo) }
    )
}

fun LineupPlayerWrapper.toLineupPlayer(): DomainLineupPlayer {
    return DomainLineupPlayer(
        id = player?.id ?: 0,
        name = player?.name ?: "",
        number = player?.number ?: 0,
        position = player?.pos ?: "",
        grid = player?.grid
    )
}

fun Prediction.toMatchPrediction(): MatchPrediction {
    return MatchPrediction(
        advice = predictions?.advice,
        winnerId = predictions?.winner?.id,
        winnerName = predictions?.winner?.name,
        homePercent = predictions?.percent?.home,
        drawPercent = predictions?.percent?.draw,
        awayPercent = predictions?.percent?.away,
        extras = predictions?.extras
    )
}

fun OddsResponse.toMatchOdds(): List<MatchOdd> {
    return bookmakers?.map { bookmaker ->
        MatchOdd(
            bookmaker = bookmaker.name ?: "",
            label = bookmaker.bets?.firstOrNull()?.name ?: "",
            values = bookmaker.bets?.firstOrNull()?.values?.map {
                OddValue(it.value ?: "", it.odd ?: "")
            } ?: emptyList()
        )
    } ?: emptyList()
}

fun Injury.toMatchInjury(): MatchInjury {
    return MatchInjury(
        playerId = player?.id,
        playerName = player?.name,
        teamId = team?.id ?: 0,
        type = player?.type,
        reason = player?.reason
    )
}

fun TeamInfoResponse.toTeamInfo(): TeamInfo {
    return TeamInfo(
        id = team?.id ?: 0,
        name = team?.name ?: "",
        logo = team?.logo,
        country = team?.country
    )
}

fun Venue.toVenueInfo(): VenueInfo {
    return VenueInfo(
        id = id,
        name = name,
        city = city,
        capacity = capacity,
        image = image
    )
}

fun TeamStatistics.toTeamStats(): TeamStats {
    return TeamStats(
        form = form,
        played = fixtures?.played?.total ?: 0,
        wins = fixtures?.wins?.total ?: 0,
        draws = fixtures?.draws?.total ?: 0,
        loses = fixtures?.loses?.total ?: 0,
        goalsFor = goals?.goalsFor?.total?.total ?: 0,
        goalsAgainst = goals?.against?.total?.total ?: 0
    )
}

fun SquadPlayer.toSquadMember(): SquadMember {
    return SquadMember(
        id = id ?: 0,
        name = name ?: "",
        position = position,
        number = number,
        photo = photo
    )
}

fun PlayerProfileStatisticsResponse.toPlayerInfo(): PlayerInfo {
    return PlayerInfo(
        id = player?.id ?: 0,
        name = player?.name ?: "",
        firstname = player?.firstname,
        lastname = player?.lastname,
        age = player?.age,
        nationality = player?.nationality,
        height = player?.height,
        weight = player?.weight,
        photo = player?.photo,
        type = player?.type
    )
}

fun PlayerStatistics.toPlayerStatDetail(): PlayerStatDetail {
    return PlayerStatDetail(
        team = TeamInfo(team?.id ?: 0, team?.name ?: "", team?.logo),
        league = LeagueInfo(league?.id ?: 0, league?.name ?: "", league?.logo, null, null, league?.season),
        appearances = games?.appearances ?: 0,
        goals = goals?.total ?: 0,
        assists = goals?.assists ?: 0,
        rating = games?.rating,
        shotsTotal = shots?.total ?: 0,
        shotsOnTarget = shots?.on ?: 0,
        passesTotal = passes?.total ?: 0,
        passesKey = passes?.key ?: 0,
        passesAccuracy = passes?.accuracy ?: 0,
        tacklesTotal = tackles?.total ?: 0,
        interceptions = tackles?.interceptions ?: 0,
        blocks = tackles?.blocks ?: 0,
        duelsTotal = duels?.total ?: 0,
        duelsWon = duels?.won ?: 0,
        dribblesAttempts = dribbles?.attempts ?: 0,
        dribblesSuccess = dribbles?.success ?: 0,
        foulsDrawn = fouls?.drawn ?: 0,
        foulsCommitted = fouls?.committed ?: 0,
        cardsYellow = cards?.yellow ?: 0,
        cardsRed = cards?.red ?: 0,
        penaltyScored = penalty?.scored ?: 0,
        penaltyMissed = penalty?.missed ?: 0
    )
}

fun PlayerTrophy.toPlayerTrophyInfo(): PlayerTrophyInfo {
    return PlayerTrophyInfo(
        league = league ?: "",
        country = country ?: "",
        season = season ?: "",
        place = place ?: ""
    )
}

fun PlayerSidelined.toPlayerInjuryInfo(): PlayerInjuryInfo {
    return PlayerInjuryInfo(
        type = type ?: "",
        start = start ?: "",
        end = end
    )
}

fun Transfer.toTransferRecord(): List<TransferRecord> {
    return transfers?.map { entry ->
        TransferRecord(
            player = player?.name ?: "",
            date = entry.date ?: "",
            type = entry.type ?: "",
            teamIn = entry.teams?.teamIn?.name ?: "Unknown",
            teamOut = entry.teams?.out?.name ?: "Unknown"
        )
    } ?: emptyList()
}

fun StandingRecord.toStandingItem(): StandingItem {
    return StandingItem(
        rank = rank,
        team = TeamInfo(team?.id ?: 0, team?.name ?: "", team?.logo),
        points = points ?: 0,
        goalsDiff = goalsDiff ?: 0,
        played = all?.played ?: 0,
        win = all?.win ?: 0,
        draw = all?.draw ?: 0,
        lose = all?.lose ?: 0,
        goalsFor = all?.goals?.goalsFor ?: 0,
        goalsAgainst = all?.goals?.against ?: 0,
        form = form
    )
}
