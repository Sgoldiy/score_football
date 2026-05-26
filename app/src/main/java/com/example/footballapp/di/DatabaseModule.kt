package com.example.footballapp.di

import android.content.Context
import androidx.room.migration.Migration
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.footballapp.data.local.db.AppDatabase
import com.example.footballapp.data.local.db.FavouriteClubDao
import com.example.footballapp.data.local.db.FavouritePlayerDao
import com.example.footballapp.data.local.db.FixtureDao
import com.example.footballapp.data.local.db.LeagueDao
import com.example.footballapp.data.local.db.StandingDao
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

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "football_plus_db"
        ).addMigrations(MIGRATION_1_2, MIGRATION_2_3)
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
    fun provideFavouritePlayerDao(db: AppDatabase): FavouritePlayerDao = db.favouritePlayerDao()
}
