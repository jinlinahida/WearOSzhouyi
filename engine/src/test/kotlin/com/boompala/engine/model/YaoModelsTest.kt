package com.boompala.engine.model

import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class YaoModelsTest {
    private val castAt = Instant.parse("2026-07-30T11:00:00Z")
    private val zoneId = ZoneId.of("Asia/Shanghai")

    @Test
    fun `states preserve conventional numeric values and flags`() {
        assertEquals(7, YaoState.YOUNG_YANG.numericValue)
        assertEquals(8, YaoState.YOUNG_YIN.numericValue)
        assertEquals(9, YaoState.OLD_YANG.numericValue)
        assertEquals(6, YaoState.OLD_YIN.numericValue)
        assertEquals(false, YaoState.YOUNG_YANG.isChanging)
        assertEquals(true, YaoState.OLD_YIN.isChanging)
    }

    @Test
    fun `input accepts exactly six lines from bottom to top`() {
        val input = HexagramInput(
            linesFromBottom = YaoPosition.entries.map { position ->
                YaoLineInput(position, YaoState.YOUNG_YANG)
            },
            castAt = castAt,
            zoneId = zoneId,
        )

        assertEquals(YaoPosition.FIRST, input.linesFromBottom.first().position)
        assertEquals(YaoPosition.TOP, input.linesFromBottom.last().position)
        assertEquals(zoneId, input.zoneId)
    }

    @Test
    fun `input rejects reversed positions`() {
        val reversed = YaoPosition.entries.reversed().map { position ->
            YaoLineInput(position, YaoState.YOUNG_YANG)
        }

        assertThrows(IllegalArgumentException::class.java) {
            HexagramInput(reversed, castAt, zoneId)
        }
    }

    @Test
    fun `pattern code is stored from bottom to top`() {
        val pattern = HexagramPattern(
            listOf(
                YaoPolarity.YIN,
                YaoPolarity.YANG,
                YaoPolarity.YIN,
                YaoPolarity.YANG,
                YaoPolarity.YIN,
                YaoPolarity.YANG,
            ),
        )

        assertEquals("010101", pattern.codeFromBottom)
    }

    @Test
    fun `hexagram owns lines calculated for its own palace`() {
        val pattern = HexagramPattern(List(6) { YaoPolarity.YANG })
        val original = hexagram(pattern, Palace.LI, SixRelation.SIBLINGS)
        val changed = hexagram(pattern, Palace.GEN, SixRelation.PARENTS)
        val result = DivinationResult(
            timeInfo = DivinationTimeInfo(
                gregorianDateTime = ZonedDateTime.ofInstant(castAt, zoneId),
                lunarDate = "丙午年 六月十七 申时",
                lunarYearGanzhi = Ganzhi(HeavenlyStem.BING, EarthlyBranch.WU),
                lunarMonth = 6,
                lunarDay = 17,
                yearGanzhi = Ganzhi(HeavenlyStem.BING, EarthlyBranch.WU),
                monthGanzhi = Ganzhi(HeavenlyStem.YI, EarthlyBranch.WEI),
                dayGanzhi = Ganzhi(HeavenlyStem.YI, EarthlyBranch.YOU),
                hourGanzhi = Ganzhi(HeavenlyStem.BING, EarthlyBranch.XU),
            ),
            voidBranches = listOf(EarthlyBranch.WU, EarthlyBranch.WEI),
            original = original,
            changed = changed,
        )

        assertEquals(SixRelation.SIBLINGS, result.original.yaoFromBottom.first().sixRelation)
        assertEquals(SixRelation.PARENTS, result.changed?.yaoFromBottom?.first()?.sixRelation)
        assertTrue(result.changingPositions.isEmpty())
    }

    private fun hexagram(
        pattern: HexagramPattern,
        palace: Palace,
        relation: SixRelation,
    ): Hexagram = Hexagram(
        pattern = pattern,
        name = "测试卦",
        palace = palace,
        element = palace.element,
        palaceStage = PalaceStage.FIRST,
        yaoFromBottom = YaoPosition.entries.mapIndexed { index, position ->
            Yao(
                position = position,
                yinYang = pattern.linesFromBottom[index],
                moving = false,
                heavenlyStem = HeavenlyStem.JIA,
                earthlyBranch = EarthlyBranch.ZI,
                element = EarthlyBranch.ZI.element,
                sixRelation = relation,
                sixSpirit = SixSpirit.entries[index],
                isShi = index == 0,
                isYing = index == 3,
                isVoid = false,
            )
        },
    )
}
