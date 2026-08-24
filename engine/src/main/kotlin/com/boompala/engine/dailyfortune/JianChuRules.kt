package com.boompala.engine.dailyfortune

import com.boompala.engine.model.EarthlyBranch

/**
 * The twelve day officers (十二建除) derived from the day and the solar-term
 * month branch.
 *
 * The month branch must be the solar-term month pillar (节气月支), i.e. the
 * earthly branch of the four-pillar month, not the lunar calendar month. The
 * day whose earthly branch equals the month branch is 建, and the cycle
 * advances one officer per day.
 */
internal object JianChuRules {

    fun fromDayAndMonth(
        dayBranch: EarthlyBranch,
        solarTermMonthBranch: EarthlyBranch,
    ): JianChu {
        val index = (dayBranch.index - solarTermMonthBranch.index + 12) % 12
        return JianChu.entries[index]
    }
}
