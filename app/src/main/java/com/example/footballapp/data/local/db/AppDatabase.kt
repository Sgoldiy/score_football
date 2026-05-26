package com.example.footballapp.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        FixtureEntity::class,
        StandingEntity::class,
        LeagueEntity::class,
        FavouriteClubEntity::class,
        FavouritePlayerEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun fixtureDao(): FixtureDao
    abstract fun standingDao(): StandingDao
    abstract fun leagueDao(): LeagueDao
    abstract fun favouriteClubDao(): FavouriteClubDao
    abstract fun favouritePlayerDao(): FavouritePlayerDao
}
