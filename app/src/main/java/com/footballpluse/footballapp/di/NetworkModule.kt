package com.footballpluse.footballapp.di

import com.footballpluse.footballapp.data.remote.ApiCacheInterceptor
import com.footballpluse.footballapp.data.remote.ApiConfig
import com.footballpluse.footballapp.data.remote.ApiService
import com.footballpluse.footballapp.data.remote.AuthInterceptor
import com.footballpluse.footballapp.data.remote.FcApiAdapter
import com.footballpluse.footballapp.data.remote.FcApiService
import com.footballpluse.footballapp.data.remote.FlexibleJsonAdapters
import com.footballpluse.footballapp.data.remote.provideLoggingInterceptor
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit
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
            // Resilience: serves the last known response when the backend rate-limits
            // or cold-starts instead of letting every screen go blank.
            .addInterceptor(ApiCacheInterceptor())
            .addInterceptor(provideLoggingInterceptor())
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, moshi: Moshi): Retrofit {
        return Retrofit.Builder()
            .baseUrl(ApiConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    @Provides
    @Singleton
    fun provideFcApiService(retrofit: Retrofit): FcApiService {
        return retrofit.create(FcApiService::class.java)
    }

    /**
     * The whole app consumes the legacy [ApiService] interface; the adapter serves
     * every call from the FootballCharts backend with real values only.
     */
    @Provides
    @Singleton
    fun provideApiService(adapter: FcApiAdapter): ApiService {
        return adapter
    }
}
