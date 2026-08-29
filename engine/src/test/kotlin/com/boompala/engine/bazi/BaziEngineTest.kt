package com.boompala.engine.bazi

import com.boompala.engine.model.EarthlyBranch
import com.boompala.engine.model.FiveElement
import com.boompala.engine.model.HeavenlyStem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class BaziEngineTest {

    @Test
    fun `calculates four pillars accurately for known date and hour`() {
        // 1990-05-15 14:00 (未时)
        val profile = BaziEngine.calculate(
            birthDate = LocalDate.of(1990, 5, 15),
            birthHour = 14,
            gender = BaziGender.MALE,
        )

        assertEquals("庚午", profile.yearPillar.ganzhi.displayName)
        assertEquals("辛巳", profile.monthPillar.ganzhi.displayName)
        assertEquals("庚辰", profile.dayPillar.ganzhi.displayName)
        assertNotNull(profile.hourPillar)
        assertEquals("癸未", profile.hourPillar?.ganzhi?.displayName)

        assertEquals(HeavenlyStem.GENG, profile.dayMaster)
        assertEquals(FiveElement.METAL, profile.dayMasterElement)
        assertEquals("马", profile.shengXiao)
        assertEquals(BaziGender.MALE, profile.gender)
        assertEquals("日主", profile.dayPillar.stemShiShen)
        assertTrue(profile.yearPillar.naYin.isNotBlank())
        assertTrue(profile.shortSummaryZh.contains("乾造"))
        assertTrue(profile.shortSummaryZh.contains("庚金日主"))
        assertEquals("庚午 辛巳 庚辰 癸未", profile.fourPillarsText)

        // Verify WuXing distribution
        assertTrue(profile.wuXingDistribution.totalCount >= 7)
        assertTrue(profile.wuXingDistribution.metalCount > 0)
        assertEquals(FiveElement.METAL, profile.wuXingDistribution.dominantElement)

        // Verify DaYun pillars
        assertTrue(profile.daYunList.isNotEmpty())
        val firstDaYun = profile.daYunList.first()
        assertTrue(firstDaYun.startAge > 0)
        assertTrue(firstDaYun.ganzhi.displayName.isNotBlank())
        assertTrue(firstDaYun.stemShiShen.isNotBlank())
    }

    @Test
    fun `supports unknown hour leaving hour pillar null`() {
        val profile = BaziEngine.calculate(
            birthDate = LocalDate.of(1990, 5, 15),
            birthHour = null,
            gender = BaziGender.FEMALE,
        )

        assertEquals("庚午", profile.yearPillar.ganzhi.displayName)
        assertEquals("辛巳", profile.monthPillar.ganzhi.displayName)
        assertEquals("庚辰", profile.dayPillar.ganzhi.displayName)
        assertNull(profile.hourPillar)

        assertEquals(HeavenlyStem.GENG, profile.dayMaster)
        assertEquals(FiveElement.METAL, profile.dayMasterElement)
        assertEquals(BaziGender.FEMALE, profile.gender)
        assertEquals("庚午 辛巳 庚辰", profile.fourPillarsText)
        assertTrue(profile.shortSummaryZh.contains("坤造"))
    }

    @Test
    fun `respects late Zi convention for next day`() {
        // 1990-05-15 23:30 (晚子时)
        val nextDaySect1 = BaziEngine.calculate(
            birthDate = LocalDate.of(1990, 5, 15),
            birthHour = 23,
            lateZiCountsAsNextDay = true,
        )
        // With late-Zi as tomorrow, day pillar advances to 辛巳
        assertEquals("辛巳", nextDaySect1.dayPillar.ganzhi.displayName)
        assertEquals("戊子", nextDaySect1.hourPillar?.ganzhi?.displayName)

        val sameDaySect2 = BaziEngine.calculate(
            birthDate = LocalDate.of(1990, 5, 15),
            birthHour = 23,
            lateZiCountsAsNextDay = false,
        )
        // With late-Zi as today, day pillar remains 庚辰, while hour pillar is 戊子
        assertEquals("庚辰", sameDaySect2.dayPillar.ganzhi.displayName)
        assertEquals("戊子", sameDaySect2.hourPillar?.ganzhi?.displayName)
    }

    @Test
    fun `handles solar term transition correctly before and after Lichun`() {
        // 2024 Lichun was around Feb 4, 2024 16:27
        // Jan 20, 2024 is still Gui-Mao year (兔年)
        val beforeLichun = BaziEngine.calculate(
            birthDate = LocalDate.of(2024, 1, 20),
            birthHour = 10,
        )
        assertEquals("癸卯", beforeLichun.yearPillar.ganzhi.displayName)
        assertEquals("兔", beforeLichun.shengXiao)

        // Feb 10, 2024 is Jia-Chen year (龙年)
        val afterLichun = BaziEngine.calculate(
            birthDate = LocalDate.of(2024, 2, 10),
            birthHour = 10,
        )
        assertEquals("甲辰", afterLichun.yearPillar.ganzhi.displayName)
        assertEquals("龙", afterLichun.shengXiao)
    }
}
