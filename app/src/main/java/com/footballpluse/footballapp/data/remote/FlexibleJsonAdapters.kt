package com.footballpluse.footballapp.data.remote

import com.footballpluse.footballapp.data.model.*
import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonReader

class FlexibleJsonAdapters {

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
