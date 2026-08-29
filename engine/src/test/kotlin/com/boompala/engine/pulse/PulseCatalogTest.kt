package com.boompala.engine.pulse

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PulseCatalogTest {

    @Test
    fun testAllTwelveProfilesExistAndComplete() {
        val all = PulseCatalog.allProfiles()
        assertEquals("应包含12个经典脉象", 12, all.size)

        for (cat in PulseCategory.entries) {
            val profile = PulseCatalog.getProfile(cat)
            assertNotNull(profile)
            assertEquals(cat, profile.category)
            assertTrue("特征描述不可为空", profile.featureDescription.isNotBlank())
            assertTrue("典型波形点不可为空", profile.waveformPoints.size >= 10)
            assertTrue("波形点在0到1之间", profile.waveformPoints.all { it in 0f..1f })
            assertTrue("宜做项不可为空", profile.dosList.isNotEmpty())
            assertTrue("忌做项不可为空", profile.dontsList.isNotEmpty())
            assertTrue("兼症调理不可为空", profile.syndromes.isNotEmpty())
            assertTrue("情志建议不可为空", profile.emotionalAdvice.isNotBlank())
            assertTrue("生活起居不可为空", profile.lifestyleAdvice.isNotBlank())
            assertTrue("运动建议不可为空", profile.exerciseAdvice.isNotBlank())
            assertTrue("典籍出处不可为空", profile.classicLiterature.isNotBlank())
            assertTrue("医理解释不可为空", profile.theoreticalReason.isNotBlank())
        }
    }

    @Test
    fun testMeridianInfluence24Hours() {
        for (h in 0..23) {
            val meridian = PulseCatalog.getMeridianInfluence(h)
            assertNotNull(meridian)
            assertTrue(meridian.earthlyBranch.endsWith("时"))
            assertTrue(meridian.meridianName.isNotBlank())
            assertTrue(meridian.healthGuidance.isNotBlank())
        }
        // Test midnight (23:00) -> 子时
        assertEquals("子时", PulseCatalog.getMeridianInfluence(23).earthlyBranch)
        // Test noon (12:00) -> 午时
        assertEquals("午时", PulseCatalog.getMeridianInfluence(12).earthlyBranch)
        // Test 16:00 -> 申时
        assertEquals("申时", PulseCatalog.getMeridianInfluence(16).earthlyBranch)
    }
}
