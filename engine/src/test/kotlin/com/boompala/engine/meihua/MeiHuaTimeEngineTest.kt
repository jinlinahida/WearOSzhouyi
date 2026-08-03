package com.boompala.engine.meihua

import com.boompala.engine.calendar.SixTailGanzhiCalendar
import com.boompala.engine.model.YaoPolarity
import com.boompala.engine.model.YaoPosition
import com.boompala.engine.rules.Trigram
import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class MeiHuaTimeEngineTest {
    private val zone = ZoneId.of("Asia/Shanghai")
    private val engine = MeiHuaTimeEngine(SixTailGanzhiCalendar())

    @Test
    fun `fixed Gregorian time produces the documented lunar casting`() {
        // 2026-07-31 17:25 Asia/Shanghai = 丙午年六月十八酉时.
        val reading = engine.calculate(Instant.parse("2026-07-31T09:25:00Z"), zone)

        assertEquals("丙午年 六月十八 酉时", reading.timeInfo.lunarDate)
        assertEquals("丙午", reading.timeInfo.lunarYearGanzhi.displayName)
        assertEquals(6, reading.timeInfo.lunarMonth)
        assertEquals(18, reading.timeInfo.lunarDay)
        assertEquals(listOf(7, 6, 18, 10), listOf(
            reading.numbers.yearBranch,
            reading.numbers.lunarMonth,
            reading.numbers.lunarDay,
            reading.numbers.hourBranch,
        ))

        // 上 = 7 + 6 + 18 = 31 -> 7 艮；下、动 = 41 -> 1 乾、5 五爻。
        assertEquals(Trigram.GEN, reading.upperTrigram)
        assertEquals(Trigram.QIAN, reading.lowerTrigram)
        assertEquals("山天大畜", reading.original.name)
        assertEquals("111001", reading.original.codeFromBottom)
        assertEquals(YaoPosition.FIFTH, reading.movingPosition)
        assertEquals("雷泽归妹", reading.mutual.name)
        assertEquals("110100", reading.mutual.codeFromBottom)
        assertEquals("风天小畜", reading.changed.name)
        assertEquals("111011", reading.changed.codeFromBottom)
        assertEquals(Trigram.QIAN, reading.bodyTrigram)
        assertEquals(Trigram.GEN, reading.useTrigram)
    }

    @Test
    fun `zero remainders map to Kun and the top line`() {
        assertEquals(8, positiveRemainder(16, 8))
        assertEquals(6, positiveRemainder(18, 6))
        assertEquals(Trigram.KUN, TrigramRules.fromCastingSum(16))
        assertEquals(YaoPosition.TOP, YaoPosition.entries[positiveRemainder(18, 6) - 1])
    }

    @Test
    fun `mutual hexagram takes second through fifth lines without changing polarity`() {
        val original = listOf(
            YaoPolarity.YANG,
            YaoPolarity.YANG,
            YaoPolarity.YANG,
            YaoPolarity.YIN,
            YaoPolarity.YIN,
            YaoPolarity.YANG,
        )

        val mutual = MutualHexagramCalculator.calculate(original)

        assertEquals(listOf(
            YaoPolarity.YANG,
            YaoPolarity.YANG,
            YaoPolarity.YIN,
            YaoPolarity.YANG,
            YaoPolarity.YIN,
            YaoPolarity.YIN,
        ), mutual.linesFromBottom)
    }
}
