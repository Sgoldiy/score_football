package com.example.footballapp.data.local.db

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
