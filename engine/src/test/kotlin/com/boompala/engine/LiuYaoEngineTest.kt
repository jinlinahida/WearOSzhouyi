package com.boompala.engine

import com.boompala.engine.data.LineTextRepository
import com.boompala.engine.model.DivinationTimeInfo
import com.boompala.engine.model.EarthlyBranch
import com.boompala.engine.model.Ganzhi
import com.boompala.engine.model.HeavenlyStem
import com.boompala.engine.model.HexagramInput
import com.boompala.engine.model.Palace
import com.boompala.engine.model.PalaceStage
import com.boompala.engine.model.SixRelation
import com.boompala.engine.model.SixSpirit
import com.boompala.engine.model.YaoLineInput
import com.boompala.engine.model.YaoPolarity
import com.boompala.engine.model.YaoPosition
import com.boompala.engine.model.YaoState
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LiuYaoEngineTest {
    private val castAt = Instant.parse("2026-07-30T11:00:00Z")
    private val zoneId = ZoneId.of("Asia/Shanghai")
    private val timeInfo = DivinationTimeInfo(
        gregorianDateTime = ZonedDateTime.ofInstant(castAt, zoneId),
        lunarDate = "丙午年 六月十七 申时",
        lunarYearGanzhi = Ganzhi(HeavenlyStem.BING, EarthlyBranch.WU),
        lunarMonth = 6,
        lunarDay = 17,
        yearGanzhi = Ganzhi(HeavenlyStem.BING, EarthlyBranch.WU),
        monthGanzhi = Ganzhi(HeavenlyStem.YI, EarthlyBranch.WEI),
        dayGanzhi = Ganzhi(HeavenlyStem.JIA, EarthlyBranch.ZI),
        hourGanzhi = Ganzhi(HeavenlyStem.JIA, EarthlyBranch.ZI),
    )
    private val engine = LiuYaoEngine(
        calendar = { _, _ -> timeInfo },
        lineTexts = LineTextRepository { code, position ->
            "$code:${position.displayName}爻辞"
        },
    )

    @Test
    fun `fixed 6 7 8 9 8 7 case produces complete traditional chart`() {
        val result = engine.calculate(input(6, 7, 8, 9, 8, 7))

        assertEquals("火水未济", result.original.name)
        assertEquals("010101", result.original.pattern.codeFromBottom)
        assertEquals(
            listOf(
                YaoPolarity.YIN,
                YaoPolarity.YANG,
                YaoPolarity.YIN,
                YaoPolarity.YANG,
                YaoPolarity.YIN,
                YaoPolarity.YANG,
            ),
            result.original.yaoFromBottom.map { it.yinYang },
        )
        assertEquals(YaoPosition.entries, result.original.yaoFromBottom.map { it.position })
        assertEquals(Palace.LI, result.original.palace)
        assertEquals(PalaceStage.THIRD, result.original.palaceStage)
        assertEquals(YaoPosition.THIRD, result.original.shiPosition)
        assertEquals(YaoPosition.TOP, result.original.yingPosition)
        assertEquals(
            listOf("戊寅", "戊辰", "戊午", "己酉", "己未", "己巳"),
            result.original.yaoFromBottom.map {
                it.heavenlyStem.displayName + it.earthlyBranch.displayName
            },
        )
        assertEquals(
            listOf(
                SixRelation.PARENTS,
                SixRelation.OFFSPRING,
                SixRelation.SIBLINGS,
                SixRelation.WEALTH,
                SixRelation.OFFSPRING,
                SixRelation.SIBLINGS,
            ),
            result.original.yaoFromBottom.map { it.sixRelation },
        )
        assertEquals(SixSpirit.entries, result.original.yaoFromBottom.map { it.sixSpirit })
        assertEquals(
            listOf(YaoPosition.FIRST, YaoPosition.FOURTH),
            result.changingPositions,
        )

        val changed = assertNotNull(result.changed).let { requireNotNull(result.changed) }
        assertEquals("山泽损", changed.name)
        assertEquals("110001", changed.pattern.codeFromBottom)
        assertEquals(
            listOf(
                YaoPolarity.YANG,
                YaoPolarity.YANG,
                YaoPolarity.YIN,
                YaoPolarity.YIN,
                YaoPolarity.YIN,
                YaoPolarity.YANG,
            ),
            changed.yaoFromBottom.map { it.yinYang },
        )
        assertEquals(YaoPosition.entries, changed.yaoFromBottom.map { it.position })
        assertEquals(Palace.GEN, changed.palace)
        assertEquals(YaoPosition.THIRD, changed.shiPosition)
        assertEquals(YaoPosition.TOP, changed.yingPosition)
        assertEquals(
            listOf("丁巳", "丁卯", "丁丑", "丙戌", "丙子", "丙寅"),
            changed.yaoFromBottom.map {
                it.heavenlyStem.displayName + it.earthlyBranch.displayName
            },
        )
        assertEquals(
            listOf(
                SixRelation.PARENTS,
                SixRelation.OFFICER_GHOST,
                SixRelation.SIBLINGS,
                SixRelation.SIBLINGS,
                SixRelation.WEALTH,
                SixRelation.OFFICER_GHOST,
            ),
            changed.yaoFromBottom.map { it.sixRelation },
        )
        assertEquals(SixSpirit.entries, changed.yaoFromBottom.map { it.sixSpirit })
        assertTrue(changed.yaoFromBottom[3].isVoid)
        assertFalse(changed.yaoFromBottom[0].moving)

        assertEquals(listOf(EarthlyBranch.XU, EarthlyBranch.HAI), result.voidBranches)
        assertEquals("丙午", result.yearGanzhi.displayName)
        assertEquals("乙未", result.monthGanzhi.displayName)
        assertEquals("甲子", result.dayGanzhi.displayName)
        assertEquals("甲子", result.hourGanzhi.displayName)
        assertEquals("010101:初爻爻辞", result.original.yaoFromBottom[0].lineText)
        assertEquals("010101:四爻爻辞", result.original.yaoFromBottom[3].lineText)
    }

    private fun input(vararg numericValues: Int): HexagramInput =
        HexagramInput(
            linesFromBottom = numericValues.mapIndexed { index, value ->
                YaoLineInput(
                    position = YaoPosition.entries[index],
                    state = YaoState.fromNumericValue(value),
                )
            },
            castAt = castAt,
            zoneId = zoneId,
        )
}
