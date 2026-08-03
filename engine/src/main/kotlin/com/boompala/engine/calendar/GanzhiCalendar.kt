package com.boompala.engine.calendar

import com.boompala.engine.model.DivinationTimeInfo
import java.time.Instant
import java.time.ZoneId

/**
 * Calendar seam for the traditional calendar data used by Liu Yao.
 *
 * Implementations start from the device Gregorian instant but must convert it
 * through Solar and Lunar before deriving Ganzhi. True solar time and
 * geographic correction remain outside the confirmed rule set.
 */
fun interface GanzhiCalendar {
    fun divinationTimeInfo(
        instant: Instant,
        zoneId: ZoneId,
    ): DivinationTimeInfo
}
