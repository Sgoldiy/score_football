package com.example.footballapp.di

import android.content.Context
import androidx.room.Room
import com.example.footballapp.data.local.db.AppDatabase
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

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "football_plus_db"
        ).build()
    }

    @Provides
    fun provideFixtureDao(db: AppDatabase): FixtureDao = db.fixtureDao()

    @Provides
    fun provideStandingDao(db: AppDatabase): StandingDao = db.standingDao()

    @Provides
    fun provideLeagueDao(db: AppDatabase): LeagueDao = db.leagueDao()
}
