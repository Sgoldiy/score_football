package com.example.footballapp.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        FixtureEntity::class,
        StandingEntity::class,
        LeagueEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun fixtureDao(): FixtureDao
    abstract fun standingDao(): StandingDao
    abstract fun leagueDao(): LeagueDao
}
