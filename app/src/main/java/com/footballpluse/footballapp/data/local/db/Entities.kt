package com.footballpluse.footballapp.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "fixtures")
data class FixtureEntity(
    @PrimaryKey val id: Int,
    val date: String,
    val leagueId: Int,
    val leagueName: String,
    val leagueLogo: String?,
    val homeTeamId: Int,
    val homeTeamName: String,
    val homeTeamLogo: String?,
    val awayTeamId: Int,
    val awayTeamName: String,
    val awayTeamLogo: String?,
    val homeScore: Int?,
    val awayScore: Int?,
    val statusShort: String?,
    val elapsed: Int?,
    val timestamp: Long,
    val isLive: Boolean
)

@Entity(tableName = "standings")
data class StandingEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val leagueId: Int,
    val season: Int,
    val rank: Int,
    val teamId: Int,
    val teamName: String,
    val teamLogo: String?,
    val played: Int,
    val win: Int,
    val draw: Int,
    val lose: Int,
    val goalsFor: Int,
    val goalsAgainst: Int,
    val goalsDiff: Int,
    val points: Int,
    val form: String?
)

@Entity(tableName = "leagues")
data class LeagueEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val country: String?,
    val logo: String?,
    val flag: String?,
    val isPriority: Boolean = false
)

@Entity(tableName = "favourite_clubs")
data class FavouriteClubEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val clubId: Int,
    val clubName: String,
    val leagueId: Int,
    val leagueName: String,
    val logoUrl: String,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "favourite_leagues")
data class FavouriteLeagueEntity(
    @PrimaryKey val leagueId: Int,
    val leagueName: String,
    val country: String?,
    val logoUrl: String?,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "favourite_players")
data class FavouritePlayerEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val playerId: Int,
    val playerName: String,
    val clubId: Int,
    val clubName: String,
    val position: String,
    val photoUrl: String,
    val addedAt: Long = System.currentTimeMillis()
)
