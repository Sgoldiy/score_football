package com.example.footballapp.data.remote

import com.example.footballapp.data.model.StatisticValue
import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonReader

class FlexibleJsonAdapters {

    @FromJson
    fun apiParametersFromJson(reader: JsonReader): ApiParameters {
        return when (val raw = reader.readJsonValue()) {
            is Map<*, *> -> ApiParameters(
                raw.mapNotNull { (key, value) ->
                    val k = key?.toString() ?: return@mapNotNull null
                    k to value.toString()
                }.toMap()
            )
            else -> ApiParameters()
        }
    }

    @FromJson
    fun apiErrorsFromJson(reader: JsonReader): ApiErrors {
        return when (val raw = reader.readJsonValue()) {
            is Map<*, *> -> ApiErrors(
                raw.mapNotNull { (key, value) ->
                    val k = key?.toString() ?: return@mapNotNull null
                    "$k: $value"
                }
            )
            is List<*> -> ApiErrors(raw.mapNotNull { it?.toString() })
            is String -> ApiErrors(listOf(raw))
            null -> ApiErrors()
            else -> ApiErrors(listOf(raw.toString()))
        }
    }

    @FromJson
    fun statisticValueFromJson(reader: JsonReader): StatisticValue {
        val raw = reader.readJsonValue()
        val display = when (raw) {
            null -> "-"
            is Double -> raw.toInt().toString()
            is Float -> raw.toInt().toString()
            is Number -> raw.toString()
            else -> raw.toString()
        }
        val numeric = display
            .replace("%", "")
            .replace(",", ".")
            .filter { it.isDigit() || it == '.' || it == '-' }
            .toFloatOrNull()
        return StatisticValue(display = display, numeric = numeric)
    }
}
