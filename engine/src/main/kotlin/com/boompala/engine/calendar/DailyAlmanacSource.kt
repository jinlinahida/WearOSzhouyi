package com.boompala.engine.calendar

import com.boompala.engine.dailyfortune.AuspiciousDirection
import com.boompala.engine.dailyfortune.LuckyHour
import java.time.LocalDate

/**
 * Almanac data for one Gregorian day: deity directions and lucky hours.
 *
 * Implementations must derive everything from the Gregorian date alone, with
 * no randomness, and map library-specific Chinese strings onto the engine
 * enums at the seam boundary.
 */
data class DailyAlmanacInfo(
    val directions: List<AuspiciousDirection>,
    val hours: List<LuckyHour>,
)

fun interface DailyAlmanacSource {
    fun almanacDay(dayGregorian: LocalDate): DailyAlmanacInfo
}

/** Degraded source used when no almanac implementation is available. */
object EmptyDailyAlmanac : DailyAlmanacSource {
    override fun almanacDay(dayGregorian: LocalDate): DailyAlmanacInfo =
        DailyAlmanacInfo(directions = emptyList(), hours = emptyList())
}
