package com.boompala.engine.dailyfortune

import com.boompala.engine.calendar.SixTailGanzhiCalendar
import com.boompala.engine.model.EarthlyBranch
import com.nlf.calendar.Solar
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class JianChuRulesTest {
    private val calendar = SixTailGanzhiCalendar()
    private val zone = ZoneId.of("Asia/Shanghai")

    private fun expectedFromSixTail(date: LocalDate): String =
        Solar.fromYmdHms(date.year, date.monthValue, date.dayOfMonth, 12, 0, 0)
            .getLunar()
            .getZhiXing()

    private fun computedFromRules(date: LocalDate): JianChu {
        val noon = date.atTime(12, 0).atZone(zone).toInstant()
        val timeInfo = calendar.divinationTimeInfo(noon, zone)
        return JianChuRules.fromDayAndMonth(
            dayBranch = timeInfo.dayGanzhi.earthlyBranch,
            solarTermMonthBranch = timeInfo.monthGanzhi.earthlyBranch,
        )
    }

    @Test
    fun `sampled dates agree with sixtail zhi xing`() {
        val samples = listOf(
            LocalDate.of(2000, 3, 15),
            LocalDate.of(2025, 7, 25),
            LocalDate.of(2025, 8, 23),
            LocalDate.of(2026, 2, 3),
            LocalDate.of(2026, 2, 4),
            LocalDate.of(2026, 2, 5),
            LocalDate.of(2026, 8, 11),
            LocalDate.of(2026, 12, 31),
        )

        samples.forEach { date ->
            assertEquals(
                "JianChu mismatch on $date",
                expectedFromSixTail(date),
                computedFromRules(date).displayName,
            )
        }
    }

    @Test
    fun `solar term month switch flips the month branch at lichun`() {
        // 立春 2026 falls on 02-04: the solar-term month branch moves 丑 -> 寅.
        val beforeLichun = calendar.divinationTimeInfo(
            LocalDate.of(2026, 2, 3).atTime(12, 0).atZone(zone).toInstant(),
            zone,
        )
        val afterLichun = calendar.divinationTimeInfo(
            LocalDate.of(2026, 2, 4).atTime(12, 0).atZone(zone).toInstant(),
            zone,
        )

        assertEquals(EarthlyBranch.CHOU, beforeLichun.monthGanzhi.earthlyBranch)
        assertEquals(EarthlyBranch.YIN, afterLichun.monthGanzhi.earthlyBranch)

        // Both days still agree with 6tail across the switch.
        assertEquals("危", computedFromRules(LocalDate.of(2026, 2, 3)).displayName)
        assertEquals("危", computedFromRules(LocalDate.of(2026, 2, 4)).displayName)
        assertEquals("成", computedFromRules(LocalDate.of(2026, 2, 5)).displayName)
    }

    @Test
    fun `day branch equal to month branch is jian`() {
        assertEquals(
            JianChu.JIAN,
            JianChuRules.fromDayAndMonth(EarthlyBranch.WU, EarthlyBranch.WU),
        )
        assertEquals(
            JianChu.CHU,
            JianChuRules.fromDayAndMonth(EarthlyBranch.WEI, EarthlyBranch.WU),
        )
        assertEquals(
            JianChu.BI,
            JianChuRules.fromDayAndMonth(EarthlyBranch.SI, EarthlyBranch.WU),
        )
    }
}
