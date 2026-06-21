package com.footballpluse.footballapp.data.local.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteClubDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(clubs: List<FavoriteClubEntity>)

    @Query("DELETE FROM favorite_clubs")
    suspend fun deleteAll()

    @Query("SELECT * FROM favorite_clubs ORDER BY addedAt DESC")
    fun getAllClubs(): Flow<List<FavoriteClubEntity>>
}
