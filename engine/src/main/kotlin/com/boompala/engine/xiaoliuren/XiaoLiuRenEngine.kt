package com.boompala.engine.xiaoliuren

import com.boompala.engine.calendar.GanzhiCalendar
import com.boompala.engine.model.DivinationTimeInfo
import java.time.Instant
import java.time.ZoneId

/** Common lunar month/day/hour inclusive-counting palm formula. */
class XiaoLiuRenEngine(private val calendar: GanzhiCalendar) {
    fun calculate(instant: Instant, zoneId: ZoneId): XiaoLiuRenReading = calculate(calendar.divinationTimeInfo(instant, zoneId))

    fun calculate(time: DivinationTimeInfo): XiaoLiuRenReading {
        val month = palace((time.lunarMonth - 1) % 6)
        val day = palace((month.ordinal + time.lunarDay - 1) % 6)
        val hourNumber = time.hourGanzhi.earthlyBranch.index + 1
        val hour = palace((day.ordinal + hourNumber - 1) % 6)
        return XiaoLiuRenReading(time, month, day, hour)
    }

    private fun palace(index: Int) = XiaoLiuRenPalace.entries[index.mod(6)]
}
