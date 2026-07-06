package com.footballpluse.footballapp.di

import android.content.Context
import androidx.room.migration.Migration
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import com.footballpluse.footballapp.data.local.db.AppDatabase
import com.footballpluse.footballapp.data.local.db.FavouriteClubDao
import com.footballpluse.footballapp.data.local.db.FavouritePlayerDao
import com.footballpluse.footballapp.data.local.db.FixtureDao
import com.footballpluse.footballapp.data.local.db.LeagueDao
import com.footballpluse.footballapp.data.local.db.StandingDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS favourite_clubs (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    clubId INTEGER NOT NULL,
                    clubName TEXT NOT NULL,
                    leagueId INTEGER NOT NULL,
                    logoUrl TEXT NOT NULL,
                    addedAt INTEGER NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS favourite_players (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    playerId INTEGER NOT NULL,
                    playerName TEXT NOT NULL,
                    clubId INTEGER NOT NULL,
                    clubName TEXT NOT NULL,
                    position TEXT NOT NULL,
                    photoUrl TEXT NOT NULL,
                    addedAt INTEGER NOT NULL
                )
                """.trimIndent()
            )
        }
    }

    private val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Add leagueName to favourite_clubs. Existing rows get empty string.
            db.execSQL("ALTER TABLE favourite_clubs ADD COLUMN leagueName TEXT NOT NULL DEFAULT ''")
        }
    }

    private val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS favorite_clubs (
                    clubId TEXT PRIMARY KEY NOT NULL,
                    clubName TEXT NOT NULL,
                    leagueId TEXT NOT NULL,
                    logoUrl TEXT,
                    addedAt INTEGER NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS user_profile (
                    id INTEGER PRIMARY KEY NOT NULL,
                    uid TEXT NOT NULL,
                    username TEXT NOT NULL,
                    displayUsername TEXT NOT NULL,
                    favoriteLeague TEXT,
                    createdAt INTEGER NOT NULL,
                    onboardingComplete INTEGER NOT NULL
                )
                """.trimIndent()
            )
        }
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "football_plus_db"
        ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideFixtureDao(db: AppDatabase): FixtureDao = db.fixtureDao()

    @Provides
    fun provideStandingDao(db: AppDatabase): StandingDao = db.standingDao()

    @Provides
    fun provideLeagueDao(db: AppDatabase): LeagueDao = db.leagueDao()

    @Provides
    fun provideFavouriteClubDao(db: AppDatabase): FavouriteClubDao = db.favouriteClubDao()

    @Provides
    fun provideFavouriteLeagueDao(db: AppDatabase): com.footballpluse.footballapp.data.local.db.FavouriteLeagueDao = db.favouriteLeagueDao()

    @Provides
    fun provideFavouritePlayerDao(db: AppDatabase): FavouritePlayerDao = db.favouritePlayerDao()

    @Provides
    fun provideFavoriteClubDao(db: AppDatabase): com.footballpluse.footballapp.data.local.db.FavoriteClubDao = db.favoriteClubDao()

    @Provides
    fun provideUserProfileDao(db: AppDatabase): com.footballpluse.footballapp.data.local.db.UserProfileDao = db.userProfileDao()
}
