package com.footballpluse.footballapp.data.mapper

import com.footballpluse.footballapp.domain.model.LineupPlayer as DomainLineupPlayer
import com.footballpluse.footballapp.data.local.db.FixtureEntity
import com.footballpluse.footballapp.data.model.*
import com.footballpluse.footballapp.domain.model.*

// ─── New API model → Old model mappers ───
internal fun String?.toIntOr(def: Int = 0): Int = this?.toIntOrNull() ?: def
private fun mapStatus(status: String?, matchLive: String?): String = when {
    status.isNullOrEmpty() -> "NS"
    status == "Finished" -> "FT"
    status == "Not Started" -> "NS"
    status == "In Play" || matchLive == "1" -> "LIVE"
    status == "Halftime" -> "HT"
    status == "Extra Time" -> "ET"
    status == "Penalties" -> "P"
    status == "Postponed" -> "PST"
    status == "Cancelled" -> "CAN"
    status == "Suspended" -> "SUS"
    status == "Interrupted" -> "INT"
    status == "After Extra Time" || status == "After ET" -> "AET"
    status == "After Penalties" || status == "After Pen." -> "AP"
    status == "Awarded" -> "AW"
    status.firstOrNull()?.isDigit() == true -> "LIVE"
    else -> status.take(3).uppercase()
}

fun ApiEvent.toFixtureResponse(): FixtureResponse {
    val fixtureId = match_id.toIntOr(0)
    val homeId = match_hometeam_id.toIntOr(0)
    val awayId = match_awayteam_id.toIntOr(0)
    val timestamp = try {
        java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            .parse(match_date ?: "")?.time ?: 0L
    } catch (_: Exception) { 0L }
    return FixtureResponse(
        fixture = Fixture(
            id = fixtureId, referee = match_referee, timezone = null,
            date = match_date, timestamp = timestamp,
            periods = Periods(null, null),
            venue = Venue(id = null, name = match_stadium, address = null, city = null, capacity = null, surface = null, image = null),
            status = FixtureStatus(long = match_status, short = mapStatus(match_status, match_live), elapsed = match_status?.takeWhile { it.isDigit() }?.toIntOrNull(), extra = null)
        ),
        league = League(id = league_id.toIntOr(0), name = league_name, type = null, country = country_name, logo = league_logo, flag = null, season = null, round = match_round, standings = null),
        teams = FixtureTeams(
            home = FixtureTeam(id = homeId, name = match_hometeam_name, logo = team_home_badge, winner = null, update = null, colors = null),
            away = FixtureTeam(id = awayId, name = match_awayteam_name, logo = team_away_badge, winner = null, update = null, colors = null)
        ),
        goals = FixtureGoals(home = match_hometeam_score?.toIntOrNull(), away = match_awayteam_score?.toIntOrNull()),
        score = FixtureScore(
            halftime = FixtureGoals(home = match_hometeam_halftime_score?.toIntOrNull(), away = match_awayteam_halftime_score?.toIntOrNull()),
            fulltime = FixtureGoals(home = match_hometeam_ft_score?.toIntOrNull() ?: match_hometeam_score?.toIntOrNull(), away = match_awayteam_ft_score?.toIntOrNull() ?: match_awayteam_score?.toIntOrNull()),
            extratime = FixtureGoals(home = match_hometeam_extra_score?.toIntOrNull(), away = match_awayteam_extra_score?.toIntOrNull()),
            penalty = FixtureGoals(home = match_hometeam_penalty_score?.toIntOrNull(), away = match_awayteam_penalty_score?.toIntOrNull())
        ),
        events = (goalscorer?.map { it.toFixtureEvent(homeId, awayId) } ?: emptyList()) +
            (cards?.map { it.toFixtureEvent(homeId, awayId) } ?: emptyList()) +
            (substitutions?.home?.map { it.toFixtureEvent(homeId) } ?: emptyList()) +
            (substitutions?.away?.map { it.toFixtureEvent(awayId) } ?: emptyList()),
        lineups = lineup?.let { l ->
            listOfNotNull(
                l.home?.toFixtureLineup(homeId, match_hometeam_name, team_home_badge),
                l.away?.toFixtureLineup(awayId, match_awayteam_name, team_away_badge)
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
        time = EventTime(elapsed = time?.toIntOrNull(), extra = null),
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
    val scorerName = if (home_scorer != null && home_scorer != score) home_scorer else away_scorer
    val isHome = home_scorer != null && home_scorer != score
    return FixtureEvent(
        time = EventTime(elapsed = time?.toIntOrNull(), extra = null),
        team = EventTeam(id = if (isHome) homeId else awayId, name = if (isHome) null else null, logo = null),
        player = EventPlayer(id = null, name = scorerName),
        assist = null,
        type = "Goal",
        detail = score,
        comments = null
    )
}

internal fun ApiCard.toFixtureEvent(homeId: Int, awayId: Int): FixtureEvent {
    val isHome = home_fault != null
    return FixtureEvent(
        time = EventTime(elapsed = time?.toIntOrNull(), extra = info_time?.toIntOrNull()),
        team = EventTeam(id = if (isHome) homeId else awayId, name = null, logo = null),
        player = EventPlayer(id = null, name = if (isHome) home_fault else away_fault),
        assist = null,
        type = card,
        detail = info,
        comments = null
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

internal fun ApiTeamLineup.toFixtureLineup(teamId: Int, teamName: String?, badge: String?): FixtureLineup {
    return FixtureLineup(
        team = FixtureTeam(id = teamId, name = teamName, logo = badge, winner = null, update = null, colors = null),
        coach = coaches?.firstOrNull()?.let { LineupCoach(id = null, name = it.player, photo = null) },
        formation = null,
        startXI = starting_lineups?.map { it.toLineupPlayerWrapper() } ?: emptyList(),
        substitutes = substitutes?.map { it.toLineupPlayerWrapper() } ?: emptyList()
    )
}

private fun ApiLineupPlayer.toLineupPlayerWrapper(): LineupPlayerWrapper {
    val num = player_number?.toIntOrNull()
    return LineupPlayerWrapper(
        player = LineupPlayer(id = player_key?.toIntOrNull(), name = player, number = num ?: 0, pos = player_pos, grid = null)
    )
}

fun List<ApiStanding>.toStanding(): Standing {
    val records = map { it.toStandingRecord() }
    val first = firstOrNull()
    return Standing(
        league = LeagueStanding(
            id = first?.league_id.toIntOr(0), name = first?.league_name,
            country = first?.country_name, logo = first?.league_logo,
            flag = null, season = null,
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
    return StandingRecord(
        rank = rank, team = Team(id = team_id.toIntOr(0), name = team_name, code = null, country = country_name,
            founded = null, national = null, logo = team_badge),
        points = standing_PTS?.toIntOrNull(), goalsDiff = goalsDiff, group = standing_group,
        form = overall_form, status = standing_place_type, description = null,
        all = StandingGoals(played = played, win = wins, draw = draws, lose = loses,
            goals = StandingGoalsDetail(goalsFor = goalsFor, against = goalsAgainst)),
        home = StandingGoals(played = null, win = null, draw = null, lose = null, goals = null),
        away = StandingGoals(played = null, win = null, draw = null, lose = null, goals = null),
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
        season = league_season?.takeWhile { it.isDigit() }?.toIntOrNull()
    )
}

fun ApiLeague.toLeagueResponse(): LeagueResponse {
    val seasonInt = league_season?.takeWhile { it.isDigit() }?.toIntOrNull() ?: 2025
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
            nationality = player_country, height = null, weight = null, injured = player_injured?.toIntOrNull()?.let { it == 1 },
            photo = player_image, type = player_type, reason = null
        ),
        statistics = listOf(
            PlayerStatistics(
                player = null, team = null, league = null,
                games = PlayerGames(appearances = player_match_played?.toIntOrNull(), lineups = null,
                    minutes = null, number = player_number?.toIntOrNull(), position = player_type, rating = player_rating ?: "0.0", captain = player_is_captain?.toIntOrNull()?.let { it == 1 }),
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
            winner = null, win_or_draw = null, under_over = null, goals = null, advice = null,
            percent = PredictionPercent(home = homeWin, draw = draw, away = awayWin)
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
        isLive = fixture?.status?.short in listOf("1H", "2H", "HT", "ET", "BT", "P", "INT", "LIVE") || fixture?.status?.elapsed != null
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
        isLive = fixture?.status?.short in listOf("1H", "2H", "HT", "ET", "BT", "P", "INT", "LIVE") || fixture?.status?.elapsed != null
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
        awayPercent = predictions?.percent?.away
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
        photo = player?.photo
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
