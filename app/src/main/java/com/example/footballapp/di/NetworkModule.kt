package com.example.footballapp.di

import com.example.footballapp.data.remote.ApiService
import com.example.footballapp.data.remote.AuthInterceptor
import com.example.footballapp.data.remote.FlexibleJsonAdapters
import com.example.footballapp.data.remote.provideLoggingInterceptor
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideMoshi(): Moshi {
        return Moshi.Builder()
            .add(FlexibleJsonAdapters())
            .add(KotlinJsonAdapterFactory())
            .build()
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(authInterceptor: AuthInterceptor): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(provideLoggingInterceptor())
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, moshi: Moshi): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://api-football-v1.p.rapidapi.com/v3/")
            .client(okHttpClient)
            .addConverterMoshi(MoshiConverterFactory.create(moshi))
            .build()
    }

    @Provides
    @Singleton
    fun provideApiService(retrofit: Retrofit): ApiService {
        return retrofit.create(ApiService::class.java)
    }

    // Helper extension if not available
    private fun Retrofit.Builder.addConverterMoshi(factory: MoshiConverterFactory): Retrofit.Builder {
        return this.addConverterFactory(factory)
    }
}
