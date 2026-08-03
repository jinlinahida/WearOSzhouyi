package com.boompala.engine.calendar

import com.nlf.calendar.EightChar
import com.nlf.calendar.Solar
import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class SixTailGanzhiCalendarTest {

    private val zone = ZoneId.of("Asia/Shanghai")

    @Test
    fun `adapter follows exact solar term pillars and selected late zi convention`() {
        val instant = Instant.parse("2024-02-04T15:30:00Z")
        val local = instant.atZone(zone).toLocalDateTime()
        val lunar = Solar.fromYmdHms(
            local.year,
            local.monthValue,
            local.dayOfMonth,
            local.hour,
            local.minute,
            local.second,
        ).getLunar()
        val eightChar = EightChar.fromLunar(lunar).apply { setSect(1) }
        val result = SixTailGanzhiCalendar(lateZiCountsAsNextDay = true)
            .divinationTimeInfo(instant, zone)

        assertEquals(eightChar.getYear(), result.yearGanzhi.displayName)
        assertEquals(eightChar.getMonth(), result.monthGanzhi.displayName)
        assertEquals(eightChar.getDay(), result.dayGanzhi.displayName)
        assertEquals(lunar.getTimeZhi(), result.hourGanzhi.earthlyBranch.displayName)
    }

    @Test
    fun `Gregorian date converts to the correct lunar date before producing pillars`() {
        val instant = Instant.parse("2026-07-30T14:00:00Z")
        val local = instant.atZone(zone).toLocalDateTime()
        val lunar = Solar.fromYmdHms(
            local.year,
            local.monthValue,
            local.dayOfMonth,
            local.hour,
            local.minute,
            local.second,
        ).getLunar()
        val expected = EightChar.fromLunar(lunar).apply { setSect(1) }

        val result = SixTailGanzhiCalendar(lateZiCountsAsNextDay = true)
            .divinationTimeInfo(instant, zone)

        assertEquals("丙午年 六月十七 亥时", result.lunarDate)
        assertEquals("丙午", result.lunarYearGanzhi.displayName)
        assertEquals(6, result.lunarMonth)
        assertEquals(17, result.lunarDay)
        assertEquals("2026-07-30T22:00+08:00[Asia/Shanghai]", result.gregorianDateTime.toString())
        assertEquals(expected.getYear(), result.yearGanzhi.displayName)
        assertEquals(expected.getMonth(), result.monthGanzhi.displayName)
        assertEquals(expected.getDay(), result.dayGanzhi.displayName)
        assertEquals(lunar.getTimeZhi(), result.hourGanzhi.earthlyBranch.displayName)
    }

    @Test
    fun `late zi switch changes day pillar but keeps local hour branch`() {
        val instant = Instant.parse("2024-02-04T15:30:00Z") // 23:30 in Shanghai
        val nextDay = SixTailGanzhiCalendar(lateZiCountsAsNextDay = true)
            .divinationTimeInfo(instant, zone)
        val sameDay = SixTailGanzhiCalendar(lateZiCountsAsNextDay = false)
            .divinationTimeInfo(instant, zone)

        check(nextDay.dayGanzhi != sameDay.dayGanzhi) {
            "The selected source date must exercise the late-Zi boundary."
        }
        assertEquals(nextDay.hourGanzhi.earthlyBranch, sameDay.hourGanzhi.earthlyBranch)
        assertEquals(
            (nextDay.dayGanzhi.heavenlyStem.index % 5) * 2 +
                nextDay.hourGanzhi.earthlyBranch.index,
            nextDay.hourGanzhi.heavenlyStem.index,
        )
        assertEquals(
            (sameDay.dayGanzhi.heavenlyStem.index % 5) * 2 +
                sameDay.hourGanzhi.earthlyBranch.index,
            sameDay.hourGanzhi.heavenlyStem.index,
        )
        // Both values remain valid sexagenary pairs.
        assertEquals(
            nextDay.hourGanzhi.heavenlyStem.index % 2,
            nextDay.hourGanzhi.earthlyBranch.index % 2,
        )
        assertEquals(
            sameDay.hourGanzhi.heavenlyStem.index % 2,
            sameDay.hourGanzhi.earthlyBranch.index % 2,
        )
    }
}
