package com.boompala.engine.model

import java.time.ZonedDateTime

/**
 * The single calendar result consumed by both the Liu Yao engine and UI.
 *
 * Implementations obtain this from a Gregorian device time through a
 * Solar -> Lunar -> Ganzhi conversion. The display layer must not recalculate
 * calendar data independently.
 */
data class DivinationTimeInfo(
    val gregorianDateTime: ZonedDateTime,
    val lunarDate: String,
    /** Lunar-calendar year, distinct from the solar-term year pillar. */
    val lunarYearGanzhi: Ganzhi,
    /** The regular month number. A leap month uses the same 1..12 number. */
    val lunarMonth: Int,
    val lunarDay: Int,
    val yearGanzhi: Ganzhi,
    val monthGanzhi: Ganzhi,
    val dayGanzhi: Ganzhi,
    val hourGanzhi: Ganzhi,
) {
    init {
        require(lunarMonth in 1..12) { "Lunar month must be in 1..12." }
        require(lunarDay in 1..30) { "Lunar day must be in 1..30." }
    }
}
