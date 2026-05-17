package com.example.footballapp.data.remote

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ApiResponse<T>(
    val get: String?,
    val parameters: Any?, // Changed from Map<String, String>? to Any? to handle [] vs {}
    val errors: Any?,     // Changed to Any? to handle [] vs {}
    val results: Int?,
    val paging: Paging?,
    val response: T
)

@JsonClass(generateAdapter = true)
data class Paging(
    val current: Int?,
    val total: Int?
)
