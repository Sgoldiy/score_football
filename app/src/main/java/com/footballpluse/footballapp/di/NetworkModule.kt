package com.footballpluse.footballapp.di

import com.footballpluse.footballapp.data.remote.ApiCacheInterceptor
import com.footballpluse.footballapp.data.remote.ApiService
import com.footballpluse.footballapp.data.remote.FlexibleJsonAdapters
import com.footballpluse.footballapp.data.remote.uitslagen.UitslagenAdapter
import com.footballpluse.footballapp.data.remote.uitslagen.UitslagenApiService
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
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

    /** Appends the upstream's global query params and a polite User-Agent. */
    class UitslagenParamsInterceptor : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val original = chain.request()
            val url = original.url.newBuilder()
                .addQueryParameter("lang", "en")
                .addQueryParameter("version", "2800")
                .build()
            return chain.proceed(
                original.newBuilder()
                    .url(url)
                    .header("Accept", "application/json")
                    .header("User-Agent", "FootballPulse/1.0")
                    .build()
            )
        }
    }

    /** Disk-backed store for the response cache; survives app restarts. */
    @Provides
    @Singleton
    fun provideHttpCacheDir(@ApplicationContext context: Context): File {
        return File(context.filesDir, "http_cache").also { it.mkdirs() }
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(cacheDir: File): OkHttpClient {
        return OkHttpClient.Builder()
            // Params first so the cache keys on the FINAL upstream URL; the cache
            // then serves fresh entries without a network call, revalidates stale
            // ones in the background, coalesces duplicate requests and backs off
            // on rate limits — cutting upstream calls dramatically.
            .addInterceptor(UitslagenParamsInterceptor())
            .addInterceptor(ApiCacheInterceptor(dir = cacheDir))
            .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, moshi: Moshi): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://uitslagen.live/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    @Provides
    @Singleton
    fun provideUitslagenApiService(retrofit: Retrofit): UitslagenApiService {
        return retrofit.create(UitslagenApiService::class.java)
    }

    /**
     * The whole app consumes the legacy [ApiService] interface; the adapter
     * serves every call from the open uitslagen.live/footapi upstream with
     * real values only, filtered through the response cache.
     */
    @Provides
    @Singleton
    fun provideApiService(adapter: UitslagenAdapter): ApiService {
        return adapter
    }
}
