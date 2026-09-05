package com.boompala.engine.rules

import com.boompala.engine.LiuYaoEngine
import com.boompala.engine.data.LineTextRepository
import com.boompala.engine.model.DivinationTimeInfo
import com.boompala.engine.model.EarthlyBranch
import com.boompala.engine.model.Ganzhi
import com.boompala.engine.model.HeavenlyStem
import com.boompala.engine.model.HexagramInput
import com.boompala.engine.model.YaoLineInput
import com.boompala.engine.model.YaoPosition
import com.boompala.engine.model.YaoState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

class YongShenEvaluatorTest {

    private val castAt = Instant.parse("2026-07-30T11:00:00Z")
    private val zoneId = ZoneId.of("Asia/Shanghai")

    @Test
    fun testEvaluateShiYao() {
        // 乾为天：世在六爻(戌土)
        val timeInfo = DivinationTimeInfo(
            gregorianDateTime = ZonedDateTime.ofInstant(castAt, zoneId),
            lunarDate = "丙午年 九月初一 申时",
            lunarYearGanzhi = Ganzhi(HeavenlyStem.BING, EarthlyBranch.WU),
            lunarMonth = 9,
            lunarDay = 1,
            yearGanzhi = Ganzhi(HeavenlyStem.BING, EarthlyBranch.WU),
            monthGanzhi = Ganzhi(HeavenlyStem.WU, EarthlyBranch.XU), // 月建为戌
            dayGanzhi = Ganzhi(HeavenlyStem.JIA, EarthlyBranch.ZI), // 日辰为子
            hourGanzhi = Ganzhi(HeavenlyStem.REN, EarthlyBranch.SHEN),
        )
        val engine = LiuYaoEngine(
            calendar = { _, _ -> timeInfo },
            lineTexts = LineTextRepository { _, _ -> "" },
        )

        val input = HexagramInput(
            linesFromBottom = YaoPosition.entries.map {
                YaoLineInput(position = it, state = YaoState.YOUNG_YANG)
            },
            castAt = castAt,
            zoneId = zoneId,
        )
        val result = engine.calculate(input)
        val evaluation = YongShenEvaluator.evaluate(result, YongShenCategory.SHI_YAO)

        assertNotNull(evaluation)
        assertEquals(YaoPosition.TOP, evaluation!!.targetPosition)
        assertTrue(evaluation.monthEffect.contains("临月建"))
    }

    @Test
    fun testEvaluateFuShenUsesFuBranchNotFeiBranch() {
        // 天风姤 (011111): 乾宫一世卦，缺“妻财”(寅木)，伏在二爻亥水子孙之下(飞神为亥水)
        val timeInfo = DivinationTimeInfo(
            gregorianDateTime = ZonedDateTime.ofInstant(castAt, zoneId),
            lunarDate = "丙午年 正月初一 申时",
            lunarYearGanzhi = Ganzhi(HeavenlyStem.BING, EarthlyBranch.WU),
            lunarMonth = 1,
            lunarDay = 1,
            yearGanzhi = Ganzhi(HeavenlyStem.BING, EarthlyBranch.WU),
            monthGanzhi = Ganzhi(HeavenlyStem.GENG, EarthlyBranch.YIN), // 寅月：伏神寅木临月建！
            dayGanzhi = Ganzhi(HeavenlyStem.JIA, EarthlyBranch.ZI),
            hourGanzhi = Ganzhi(HeavenlyStem.REN, EarthlyBranch.SHEN),
        )
        val engine = LiuYaoEngine(
            calendar = { _, _ -> timeInfo },
            lineTexts = LineTextRepository { _, _ -> "" },
        )

        val input = HexagramInput(
            linesFromBottom = listOf(
                YaoLineInput(YaoPosition.FIRST, YaoState.YOUNG_YIN),
                YaoLineInput(YaoPosition.SECOND, YaoState.YOUNG_YANG),
                YaoLineInput(YaoPosition.THIRD, YaoState.YOUNG_YANG),
                YaoLineInput(YaoPosition.FOURTH, YaoState.YOUNG_YANG),
                YaoLineInput(YaoPosition.FIFTH, YaoState.YOUNG_YANG),
                YaoLineInput(YaoPosition.TOP, YaoState.YOUNG_YANG),
            ),
            castAt = castAt,
            zoneId = zoneId,
        )
        val result = engine.calculate(input)
        val evaluation = YongShenEvaluator.evaluate(result, YongShenCategory.WEALTH)

        assertNotNull(evaluation)
        assertTrue(evaluation!!.isFuShen)
        assertEquals(YaoPosition.SECOND, evaluation.targetPosition)
        // 关键断言：如果使用伏神地支(寅木)，则在寅月应是“临月建(大旺)”；若错误使用了飞神地支(亥水)，则不是临月建
        assertTrue(evaluation.monthEffect.contains("临月建"))
        assertTrue(evaluation.summary.contains("伏神"))
    }
}
