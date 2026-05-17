package com.example.footballapp.data.remote

import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import javax.inject.Inject

class AuthInterceptor @Inject constructor() : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val requestWithHeaders = originalRequest.newBuilder()
            .header("x-rapidapi-host", "api-football-v1.p.rapidapi.com")
            .header("x-rapidapi-key", "3bdb371035msh4a88eff8edf2ec5p103690jsnd48d0c24e31b")
            .header("Accept", "application/json")
            .header("User-Agent", "PostmanRuntime/7.32.3") // Imitate Postman if it works there
            .build()
        return chain.proceed(requestWithHeaders)
    }
}

fun provideLoggingInterceptor(): HttpLoggingInterceptor {
    return HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }
}
