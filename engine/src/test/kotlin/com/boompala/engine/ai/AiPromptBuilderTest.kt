package com.boompala.engine.ai

import com.boompala.engine.LiuYaoEngine
import com.boompala.engine.bazi.BaziGender
import com.boompala.engine.model.DivinationTimeInfo
import com.boompala.engine.model.EarthlyBranch
import com.boompala.engine.model.Ganzhi
import com.boompala.engine.model.HeavenlyStem
import com.boompala.engine.model.HexagramInput
import com.boompala.engine.model.YaoLineInput
import com.boompala.engine.model.YaoPosition
import com.boompala.engine.model.YaoState
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AiPromptBuilderTest {

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
    )

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

    @Test
    fun `build prompt for moving hexagram contains required structures and guidelines`() {
        // 6 7 8 9 8 7 -> 火水未济 变 山风蛊
        val result = engine.calculate(input(6, 7, 8, 9, 8, 7))
        val prompt = AiPromptBuilder.buildPrompt(
            result = result,
            topic = AiDivinationTopic.CAREER,
            question = "今年下半年换工作是否顺利？",
            userGender = BaziGender.MALE,
        )

        // System prompt checks
        assertTrue("System prompt should enforce 4 sections", prompt.systemPrompt.contains("【核心判断】"))
        assertTrue("System prompt should enforce 4 sections", prompt.systemPrompt.contains("【卦象依据】"))
        assertTrue("System prompt should enforce 4 sections", prompt.systemPrompt.contains("【趋势分析】"))
        assertTrue("System prompt should enforce 4 sections", prompt.systemPrompt.contains("【建议】"))
        assertTrue("System prompt should mention Wear OS limit", prompt.systemPrompt.contains("300～500"))
        assertTrue("System prompt should forbid fabrication", prompt.systemPrompt.contains("严禁虚构"))

        // User prompt checks
        assertTrue(prompt.userPrompt.contains("火水未济"))
        assertTrue(prompt.userPrompt.contains("今年下半年换工作是否顺利？"))
        assertTrue(prompt.userPrompt.contains("丙午年 乙未月 甲子日 甲子时"))
        assertTrue(prompt.userPrompt.contains("乾造")) // Male title
        assertTrue(prompt.userPrompt.contains("事业官运"))

        // Context checks
        assertEquals("火水未济", prompt.context.originalHexagramName)
        assertNotNull(prompt.context.changedHexagramName)
        assertEquals(AiDivinationTopic.CAREER, prompt.context.topic)
        assertEquals(6, prompt.context.lines.size)

        // Check Shi/Ying
        assertTrue(prompt.context.shiYingDescription.contains("世爻"))
        assertTrue(prompt.context.shiYingDescription.contains("应爻"))

        // Check moving lines
        assertTrue(prompt.context.movingLinesSummary.any { it.contains("初爻发动") })
        assertTrue(prompt.context.movingLinesSummary.any { it.contains("四爻发动") })
    }

    @Test
    fun `build prompt for static hexagram correctly handles quiet lines without changed hexagram`() {
        // 7 7 7 7 7 7 -> 乾为天（纯阳六爻安静）
        val result = engine.calculate(input(7, 7, 7, 7, 7, 7))
        val prompt = AiPromptBuilder.buildPrompt(
            result = result,
            topic = AiDivinationTopic.WEALTH,
            question = null,
        )

        assertNull(prompt.context.changedHexagramName)
        assertTrue(prompt.context.movingLinesSummary.any { it.contains("六爻安静（静卦）") })
        assertTrue(prompt.userPrompt.contains("六爻安静之静卦"))
        assertTrue(prompt.context.yongShenDescription.contains("妻财"))
    }

    @Test
    fun `relationship topic distinguishes male and female yong shen`() {
        val result = engine.calculate(input(7, 8, 7, 8, 7, 8)) // 水火既济

        val malePrompt = AiPromptBuilder.buildPrompt(
            result = result,
            topic = AiDivinationTopic.RELATIONSHIP,
            userGender = BaziGender.MALE,
        )
        assertTrue(malePrompt.context.yongShenDescription.contains("妻财"))

        val femalePrompt = AiPromptBuilder.buildPrompt(
            result = result,
            topic = AiDivinationTopic.RELATIONSHIP,
            userGender = BaziGender.FEMALE,
        )
        assertTrue(femalePrompt.context.yongShenDescription.contains("官鬼"))
    }

    @Test
    fun `study and health topics select corresponding yong shen`() {
        val result = engine.calculate(input(8, 8, 8, 8, 8, 8)) // 坤为地

        val studyPrompt = AiPromptBuilder.buildPrompt(
            result = result,
            topic = AiDivinationTopic.STUDY,
        )
        assertTrue(studyPrompt.context.yongShenDescription.contains("父母"))

        val healthPrompt = AiPromptBuilder.buildPrompt(
            result = result,
            topic = AiDivinationTopic.HEALTH,
        )
        assertTrue(healthPrompt.context.yongShenDescription.contains("子孙"))
    }

    @Test
    fun `question sanitization truncates over-length inputs`() {
        val result = engine.calculate(input(7, 7, 7, 7, 7, 7))
        val superLongQuestion = "测试问题".repeat(100) // 400 chars
        val prompt = AiPromptBuilder.buildPrompt(
            result = result,
            question = superLongQuestion,
        )

        assertNotNull(prompt.context.userQuestion)
        assertEquals(200, prompt.context.userQuestion?.length)
    }

    @Test
    fun `blank question is treated as null in context`() {
        val result = engine.calculate(input(7, 7, 7, 7, 7, 7))
        val prompt = AiPromptBuilder.buildPrompt(
            result = result,
            question = "   \n\t  ",
        )

        assertNull(prompt.context.userQuestion)
        assertFalse(prompt.userPrompt.contains("具体问事：   "))
    }
}
