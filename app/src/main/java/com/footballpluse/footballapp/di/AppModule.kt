package com.footballpluse.footballapp.di

import android.content.Context
import android.content.SharedPreferences
import androidx.room.Room
import com.footballpluse.footballapp.data.local.db.FavoriteClubDao
import com.footballpluse.footballapp.data.local.db.FootballPlusDatabase
import com.footballpluse.footballapp.data.local.db.UserProfileDao
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideSharedPreferences(@ApplicationContext context: Context): SharedPreferences {
        return context.getSharedPreferences("football_plus_prefs", Context.MODE_PRIVATE)
    }

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore {
        return FirebaseFirestore.getInstance()
    }

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth {
        return FirebaseAuth.getInstance()
    }

    @Provides
    @Singleton
    fun provideFootballPlusDatabase(@ApplicationContext context: Context): FootballPlusDatabase {
        return Room.databaseBuilder(
            context,
            FootballPlusDatabase::class.java,
            "football_plus_onboarding_db"
        ).build()
    }

    @Provides
    fun provideUserProfileDao(db: FootballPlusDatabase): UserProfileDao {
        return db.userProfileDao()
    }

    @Provides
    fun provideFavoriteClubDao(db: FootballPlusDatabase): FavoriteClubDao {
        return db.favoriteClubDao()
    }
}
