package com.example.footballapp.data.mapper

import com.example.footballapp.data.model.LineupPlayer as DataLineupPlayer
import com.example.footballapp.domain.model.LineupPlayer as DomainLineupPlayer
import com.example.footballapp.data.local.db.FixtureEntity
import com.example.footballapp.data.model.*
import com.example.footballapp.domain.model.*

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
        logo = team?.logo
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
        rating = games?.rating
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
