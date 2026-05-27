package com.footballpluse.footballapp.data.util

import java.util.Calendar

object SeasonUtils {
    fun currentSeasonStartYear(): Int {
        val now = Calendar.getInstance()
        val year = now.get(Calendar.YEAR)
        val month = now.get(Calendar.MONTH) + 1
        return if (month >= 7) year else year - 1
    }

    fun displaySeasonLabel(seasonStartYear: Int): String {
        val endYear2 = ((seasonStartYear + 1) % 100).toString().padStart(2, '0')
        return "$seasonStartYear/$endYear2"
    }
}

