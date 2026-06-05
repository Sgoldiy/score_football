package com.footballpluse.footballapp.data.local.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FixtureDao {
    @Query("SELECT * FROM fixtures WHERE date = :date ORDER BY timestamp ASC")
    fun getFixturesByDate(date: String): Flow<List<FixtureEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFixtures(fixtures: List<FixtureEntity>)

    @Query("DELETE FROM fixtures WHERE date = :date")
    suspend fun deleteFixturesByDate(date: String)

    @Query("SELECT COUNT(*) FROM fixtures WHERE date = :date")
    suspend fun getFixtureCountByDate(date: String): Int
}

@Dao
interface StandingDao {
    @Query("SELECT * FROM standings WHERE leagueId = :leagueId AND season = :season ORDER BY rank ASC")
    fun getStandings(leagueId: Int, season: Int): Flow<List<StandingEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStandings(standings: List<StandingEntity>)

    @Query("DELETE FROM standings WHERE leagueId = :leagueId AND season = :season")
    suspend fun deleteStandings(leagueId: Int, season: Int)
}

@Dao
interface LeagueDao {
    @Query("SELECT * FROM leagues ORDER BY name ASC")
    fun getAllLeagues(): Flow<List<LeagueEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLeagues(leagues: List<LeagueEntity>)
}

@Dao
interface FavouriteClubDao {
    @Query("SELECT * FROM favourite_clubs ORDER BY addedAt DESC")
    fun getFavouriteClubs(): Flow<List<FavouriteClubEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(clubs: List<FavouriteClubEntity>)

    @Query("DELETE FROM favourite_clubs")
    suspend fun clear()
}

@Dao
interface FavouritePlayerDao {
    @Query("SELECT * FROM favourite_players ORDER BY addedAt DESC")
    fun getFavouritePlayers(): Flow<List<FavouritePlayerEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(players: List<FavouritePlayerEntity>)

    @Query("DELETE FROM favourite_players")
    suspend fun clear()
}
