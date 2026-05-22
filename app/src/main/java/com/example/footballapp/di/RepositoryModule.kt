package com.example.footballapp.di

import com.example.footballapp.data.repository.FootballRepositoryImpl
import com.example.footballapp.domain.repository.FootballRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindFootballRepository(
        footballRepositoryImpl: FootballRepositoryImpl
    ): FootballRepository
}
