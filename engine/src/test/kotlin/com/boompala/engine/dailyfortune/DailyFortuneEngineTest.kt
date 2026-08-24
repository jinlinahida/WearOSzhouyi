package com.boompala.engine.dailyfortune

import com.boompala.engine.calendar.SixTailDailyAlmanac
import com.boompala.engine.calendar.SixTailGanzhiCalendar
import com.boompala.engine.data.JsonHexagramInterpretationRepository
import com.boompala.engine.data.JsonLineTextRepository
import com.boompala.engine.model.EarthlyBranch
import com.boompala.engine.model.FiveElement
import com.boompala.engine.model.YaoPosition
import java.io.File
import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyFortuneEngineTest {
    private val zone = ZoneId.of("Asia/Shanghai")

    private val engine = DailyFortuneEngine(
        calendar = SixTailGanzhiCalendar(),
        almanac = SixTailDailyAlmanac(),
        lineTextRepository = JsonLineTextRepository.fromReader(
            File(requireNotNull(System.getProperty("yaoTextAssetPath"))).bufferedReader(),
        ),
        interpretationRepository = JsonHexagramInterpretationRepository.fromReader(
            File(requireNotNull(System.getProperty("hexagramInterpretationAssetPath"))).bufferedReader(),
        ),
    )

    @Test
    fun `fixed instant produces the documented golden reading`() {
        // 2026-08-11 11:25 Asia/Shanghai = 丙午年六月廿九, 丁巳日.
        val reading = engine.fortuneFor(Instant.parse("2026-08-11T03:25:00Z"), zone)

        assertEquals("2026-08-11", reading.date.toString())
        assertEquals("丙午年 六月廿九 午时", reading.lunarDateText)
        assertEquals("丁巳", reading.dayGanzhi.displayName)
        assertEquals(FiveElement.FIRE, reading.dayStemElement)

        // Rotation: 2026-08-11 is slot 173 -> 周文王序第 29 卦 坎为水, 上爻值日.
        assertEquals(173, reading.rotationIndex)
        assertEquals("010010", reading.dayHexagramCode)
        assertEquals("坎为水", reading.dayHexagramName)
        assertEquals(YaoPosition.TOP, reading.dayLinePosition)
        assertNotNull(reading.hexagramSummary)
        assertNotNull(reading.hexagramAdvice)
        assertNotNull(reading.dayLineText)

        // 丁 = 火: 生我者木 -> 青绿/3,8, 同我者火 -> 红, 克我者水 -> 黑.
        assertEquals(FortuneColor.GREEN, reading.luckyColor)
        assertEquals(FortuneColor.RED, reading.supportColor)
        assertEquals(FortuneColor.BLACK, reading.avoidColor)
        assertEquals(listOf(3, 8), reading.luckyNumbers)

        assertEquals(JianChu.SHOU, reading.jianChu)
        assertEquals(
            FortuneDeity.XI_SHEN to Direction.LI,
            reading.directions[0].deity to reading.directions[0].direction,
        )
        assertEquals("正南", reading.directions[0].description)
        assertEquals(
            FortuneDeity.YANG_GUI to Direction.QIAN,
            reading.directions[1].deity to reading.directions[1].direction,
        )
        assertEquals(
            listOf("玉堂", "司命", "青龙", "明堂", "金匮", "天德", "金匮"),
            reading.hours.map { it.deityName },
        )
        assertEquals(EarthlyBranch.ZI, reading.hours.last().branch)
        assertEquals("23:00-23:59", reading.hours.last().periodText)
    }

    @Test
    fun `same instant twice yields identical readings`() {
        val instant = Instant.parse("2026-08-11T03:25:00Z")
        assertEquals(engine.fortuneFor(instant, zone), engine.fortuneFor(instant, zone))
    }

    @Test
    fun `any moment inside one day yields the same reading`() {
        val morning = engine.fortuneFor(Instant.parse("2026-08-10T16:30:00Z"), zone)
        val evening = engine.fortuneFor(Instant.parse("2026-08-11T14:00:00Z"), zone)
        assertEquals(morning, evening)
    }

    @Test
    fun `2359 and the next 0001 belong to different days`() {
        val lateDay = engine.fortuneFor(Instant.parse("2026-08-11T15:59:00Z"), zone)
        val nextDay = engine.fortuneFor(Instant.parse("2026-08-11T16:01:00Z"), zone)

        assertEquals("2026-08-11", lateDay.date.toString())
        assertEquals("2026-08-12", nextDay.date.toString())
        assertEquals("丁巳", lateDay.dayGanzhi.displayName)
        assertEquals("戊午", nextDay.dayGanzhi.displayName)
        assertNotEquals(lateDay.rotationIndex, nextDay.rotationIndex)
        assertEquals(YaoPosition.entries[(lateDay.rotationIndex + 1) % 6], nextDay.dayLinePosition)
    }

    @Test
    fun `spring festival boundary keeps the gregorian day sequence intact`() {
        // 2026-02-17 is 丙午年正月初一; the lunar year flips but the day sequence does not.
        val beforeFestival = engine.fortuneFor(Instant.parse("2026-02-16T04:00:00Z"), zone)
        val festivalDay = engine.fortuneFor(Instant.parse("2026-02-17T04:00:00Z"), zone)

        assertTrue(beforeFestival.lunarDateText.startsWith("乙巳年"))
        assertTrue(festivalDay.lunarDateText.startsWith("丙午年 正月初一"))
        assertEquals("辛酉", beforeFestival.dayGanzhi.displayName)
        assertEquals("壬戌", festivalDay.dayGanzhi.displayName)
        assertEquals(
            (beforeFestival.rotationIndex + 1) % 384,
            festivalDay.rotationIndex,
        )
    }

    @Test
    fun `leap month day keeps the same rotation and ganzhi contract`() {
        // 2025-07-25 is 闰六月初一; the leap month is display-only for this feature.
        val leapDay = engine.fortuneFor(Instant.parse("2025-07-25T04:00:00Z"), zone)

        assertTrue(leapDay.lunarDateText.contains("闰"))
        assertEquals("乙未", leapDay.dayGanzhi.displayName)
        assertEquals(JianChu.JIAN, leapDay.jianChu)

        // 2025-08-23 is 七月初一 and a real 甲子 day: stem wood -> 水生木.
        val jiaZiDay = engine.fortuneFor(Instant.parse("2025-08-23T04:00:00Z"), zone)
        assertEquals("甲子", jiaZiDay.dayGanzhi.displayName)
        assertEquals(FiveElement.WOOD, jiaZiDay.dayStemElement)
        assertEquals(FortuneColor.BLACK, jiaZiDay.luckyColor)
        assertEquals(listOf(1, 6), jiaZiDay.luckyNumbers)
    }
}
