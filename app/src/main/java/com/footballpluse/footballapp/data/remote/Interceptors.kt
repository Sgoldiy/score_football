package com.footballpluse.footballapp.data.remote

import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import javax.inject.Inject

class AuthInterceptor @Inject constructor() : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val requestWithHeaders = originalRequest.newBuilder()
            .header("x-rapidapi-host", "apifootball3.p.rapidapi.com")
            .header("x-rapidapi-key", "cfd7436d97mshd821fb1a26e77c5p1cd2c4jsn6f8e04c498fa")
            .header("Content-Type", "application/json")
            .build()
        return chain.proceed(requestWithHeaders)
    }
}

fun provideLoggingInterceptor(): HttpLoggingInterceptor {
    return HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }
}
