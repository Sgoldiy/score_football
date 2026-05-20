package com.example.footballapp.data.remote

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ApiResponse<T>(
    val get: String?,
    val parameters: ApiParameters?,
    val errors: ApiErrors?,
    val results: Int?,
    val paging: Paging?,
    val response: T
)

@JsonClass(generateAdapter = true)
data class Paging(
    val current: Int?,
    val total: Int?
)

data class ApiParameters(
    val values: Map<String, String> = emptyMap()
)

data class ApiErrors(
    val messages: List<String> = emptyList()
) {
    val isNotEmpty: Boolean get() = messages.isNotEmpty()
}
