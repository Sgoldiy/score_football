package com.footballpluse.footballapp.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        UserProfileEntity::class,
        FavoriteClubEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class FootballPlusDatabase : RoomDatabase() {
    abstract fun userProfileDao(): UserProfileDao
    abstract fun favoriteClubDao(): FavoriteClubDao
}
