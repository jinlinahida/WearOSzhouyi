package com.boompala.engine.dailyfortune

import com.boompala.engine.bazi.BaziEngine
import com.boompala.engine.bazi.BaziGender
import com.boompala.engine.model.EarthlyBranch
import com.boompala.engine.model.FiveElement
import com.boompala.engine.model.Ganzhi
import com.boompala.engine.model.HeavenlyStem
import com.boompala.engine.model.YaoPosition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class PersonalFortuneEvaluatorTest {

    private fun mockReading(
        dayStem: HeavenlyStem,
        dayBranch: EarthlyBranch,
        hexagramCode: String = "111111", // 乾卦，金
    ): DailyFortuneReading {
        return DailyFortuneReading(
            date = LocalDate.of(2026, 8, 28),
            lunarDateText = "七月十六",
            dayGanzhi = Ganzhi(dayStem, dayBranch),
            dayStemElement = dayStem.element,
            dayHexagramCode = hexagramCode,
            dayHexagramName = "乾为天",
            rotationIndex = 0,
            hexagramSummary = "天行健，君子以自强不息",
            hexagramAdvice = "刚健笃实",
            dayLinePosition = YaoPosition.FIRST,
            dayLineText = "潜龙勿用",
            luckyColor = FortuneColor.YELLOW,
            supportColor = FortuneColor.WHITE,
            avoidColor = FortuneColor.RED,
            luckyNumbers = listOf(5, 10),
            directions = emptyList(),
            hours = emptyList(),
            jianChu = JianChu.JIAN,
        )
    }

    @Test
    fun testShiShenCalculation() {
        // 1990-06-15 为庚午年 壬午月 辛未日，辛金日主
        val profile = BaziEngine.calculate(
            birthDate = LocalDate.of(1990, 6, 15),
            birthHour = 12,
            gender = BaziGender.MALE,
        )
        assertEquals(HeavenlyStem.XIN, profile.dayMaster)

        // 辛遇癸水（我生者，同阴）-> 食神（癸亥日，合法阴阴干支）
        val readingFood = mockReading(HeavenlyStem.GUI, EarthlyBranch.HAI)
        val fortuneFood = PersonalFortuneEvaluator.evaluate(profile, readingFood)
        assertEquals("食神", fortuneFood.shiShenName)
        assertTrue(fortuneFood.themeTitle.contains("才思"))

        // 辛遇壬水（我生者，异阳）-> 伤官（壬子日，合法阳阳干支）
        val readingHarm = mockReading(HeavenlyStem.REN, EarthlyBranch.ZI)
        val fortuneHarm = PersonalFortuneEvaluator.evaluate(profile, readingHarm)
        assertEquals("伤官", fortuneHarm.shiShenName)

        // 辛遇丙火（克我者，异阳）-> 正官，且丙辛相合！（丙申日，合法阳阳干支）
        val readingOfficial = mockReading(HeavenlyStem.BING, EarthlyBranch.SHEN)
        val fortuneOfficial = PersonalFortuneEvaluator.evaluate(profile, readingOfficial)
        assertEquals("正官", fortuneOfficial.shiShenName)
        assertTrue(fortuneOfficial.events.any { it.title.contains("天干相合") })

        // 辛遇丁火（克我者，同阴）-> 七杀（丁卯日，合法阴阴干支）
        val readingKill = mockReading(HeavenlyStem.DING, EarthlyBranch.MAO)
        val fortuneKill = PersonalFortuneEvaluator.evaluate(profile, readingKill)
        assertEquals("七杀", fortuneKill.shiShenName)

        // 辛遇甲木（我克者，异阳）-> 正财（甲戌日，合法阳阳干支）
        val readingWealth = mockReading(HeavenlyStem.JIA, EarthlyBranch.XU)
        val fortuneWealth = PersonalFortuneEvaluator.evaluate(profile, readingWealth)
        assertEquals("正财", fortuneWealth.shiShenName)
    }

    @Test
    fun testBranchInteractionsAndShenSha() {
        // 辛未日主，生肖属马（午年）
        val profile = BaziEngine.calculate(
            birthDate = LocalDate.of(1990, 6, 15),
            birthHour = 12,
            gender = BaziGender.MALE,
        )

        // 今日为甲子日（子午冲），冲年支午马生肖，辛见子为文昌
        val readingZi = mockReading(HeavenlyStem.JIA, EarthlyBranch.ZI)
        val fortuneZi = PersonalFortuneEvaluator.evaluate(profile, readingZi)
        assertTrue(fortuneZi.events.any { it.title.contains("生肖逢冲") })
        assertTrue(fortuneZi.events.any { it.title.contains("文昌") })

        // 今日为乙巳日（巳亥冲），冲日支辛亥自身
        val readingSi = mockReading(HeavenlyStem.YI, EarthlyBranch.SI)
        val fortuneSi = PersonalFortuneEvaluator.evaluate(profile, readingSi)
        assertTrue(fortuneSi.events.any { it.title.contains("日支相冲") })

        // 今日为甲午日：辛见午为天乙贵人，且与年支午相同（本命值日）
        val readingWu = mockReading(HeavenlyStem.JIA, EarthlyBranch.WU)
        val fortuneWu = PersonalFortuneEvaluator.evaluate(profile, readingWu)
        assertTrue(fortuneWu.events.any { it.title.contains("天乙贵人") })
        assertTrue(fortuneWu.events.any { it.title.contains("本命值日") })

        // 今日为甲寅日：寅亥六合（日支六合）
        val readingYin = mockReading(HeavenlyStem.JIA, EarthlyBranch.YIN)
        val fortuneYin = PersonalFortuneEvaluator.evaluate(profile, readingYin)
        assertTrue(fortuneYin.events.any { it.title.contains("日支六合") })
    }

    @Test
    fun testBalanceColorAndHexResonance() {
        val profile = BaziEngine.calculate(
            birthDate = LocalDate.of(1990, 6, 15),
            birthHour = 12,
            gender = BaziGender.MALE,
        )

        val reading = mockReading(HeavenlyStem.BING, EarthlyBranch.WU, hexagramCode = "111111") // 乾卦金
        val fortune = PersonalFortuneEvaluator.evaluate(profile, reading)

        assertNotNull(fortune.balanceColor)
        assertTrue(fortune.balanceNumbers.isNotEmpty())
        assertTrue(fortune.hexagramResonance.isNotEmpty())
    }
}
