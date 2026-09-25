package com.boompala.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class DailyFortuneScreenTest {

    @Test
    fun fortuneDayTabValuesAreDefined() {
        val tabs = FortuneDayTab.entries
        assertEquals(2, tabs.size)
        assertEquals(FortuneDayTab.TODAY, tabs[0])
        assertEquals(FortuneDayTab.TOMORROW, tabs[1])
    }

    @Test
    fun textReplacementForTomorrowWorksAsExpected() {
        val originalDescription = "今日流日天干与日元相合，主诸事顺遂、人缘和美、得道多助"
        val replaced = originalDescription.replace("今日", "明日")
        assertEquals("明日流日天干与日元相合，主诸事顺遂、人缘和美、得道多助", replaced)

        val clashDescription = "今日日支与自身日支相冲，气场略显动荡"
        val replacedClash = clashDescription.replace("今日", "明日")
        assertEquals("明日日支与自身日支相冲，气场略显动荡", replacedClash)
    }
}
