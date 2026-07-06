package com.footballpluse.footballapp.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        FixtureEntity::class,
        StandingEntity::class,
        LeagueEntity::class,
        FavouriteClubEntity::class,
        FavoriteClubEntity::class,
        FavouritePlayerEntity::class,
        FavouriteLeagueEntity::class,
        UserProfileEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun fixtureDao(): FixtureDao
    abstract fun standingDao(): StandingDao
    abstract fun leagueDao(): LeagueDao
    abstract fun favouriteClubDao(): FavouriteClubDao
    abstract fun favoriteClubDao(): FavoriteClubDao
    abstract fun favouriteLeagueDao(): FavouriteLeagueDao
    abstract fun favouritePlayerDao(): FavouritePlayerDao
    abstract fun userProfileDao(): UserProfileDao
}
