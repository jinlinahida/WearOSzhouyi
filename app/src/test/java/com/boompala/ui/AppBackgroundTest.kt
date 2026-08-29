package com.boompala.ui

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

class AppBackgroundTest {

    @Test
    fun defaultSpotlightColorIsSoftLightBlue() {
        assertEquals(Color(0xFF64B5F6), DefaultSpotlightColor)
    }

    @Test
    fun resolveSpotlightColorMapsScreensCorrectly() {
        // 默认界面：浅蓝色
        assertEquals(DefaultSpotlightColor, resolveSpotlightColor(AppScreen.HOME))
        assertEquals(DefaultSpotlightColor, resolveSpotlightColor(AppScreen.SETTINGS))
        assertEquals(DefaultSpotlightColor, resolveSpotlightColor(AppScreen.ARCHIVES))
        assertEquals(DefaultSpotlightColor, resolveSpotlightColor(AppScreen.WELCOME))
        assertEquals(DefaultSpotlightColor, resolveSpotlightColor(AppScreen.YAO_INPUT))
        assertEquals(DefaultSpotlightColor, resolveSpotlightColor(AppScreen.MEIHUA_TIME))
        assertEquals(DefaultSpotlightColor, resolveSpotlightColor(AppScreen.XIAO_LIU_REN))
        assertEquals(DefaultSpotlightColor, resolveSpotlightColor(AppScreen.BROWSE))

        // 特殊界面 1：今日运势为暖珀流金色
        assertEquals(DailyFortuneSpotlightColor, resolveSpotlightColor(AppScreen.DAILY_FORTUNE))
        assertEquals(Color(0xFFFFB74D), DailyFortuneSpotlightColor)

        // 特殊界面 2：塔罗占卜系列为神秘蓝紫色
        assertEquals(TarotSpotlightColor, resolveSpotlightColor(AppScreen.TAROT_ONE_CARD))
        assertEquals(TarotSpotlightColor, resolveSpotlightColor(AppScreen.TAROT_THREE_CARD))
        assertEquals(TarotSpotlightColor, resolveSpotlightColor(AppScreen.TAROT_HOLY_TRIANGLE))
        assertEquals(TarotSpotlightColor, resolveSpotlightColor(AppScreen.TAROT_CELTIC_CROSS))
        assertEquals(TarotSpotlightColor, resolveSpotlightColor(AppScreen.TAROT_BROWSER))
        assertEquals(TarotSpotlightColor, resolveSpotlightColor(AppScreen.TAROT_CARD_DETAIL))
        assertEquals(Color(0xFF9575CD), TarotSpotlightColor)

        // 特殊界面 3：罗盘为青铜玄碧色
        assertEquals(CompassSpotlightColor, resolveSpotlightColor(AppScreen.COMPASS))
        assertEquals(Color(0xFF4DB6AC), CompassSpotlightColor)

        // 特殊界面 4：排盘结果为苍璧青玉色
        assertEquals(ResultSpotlightColor, resolveSpotlightColor(AppScreen.RESULT))
        assertEquals(ResultSpotlightColor, resolveSpotlightColor(AppScreen.MEIHUA_RESULT))
        assertEquals(Color(0xFF80CBC4), ResultSpotlightColor)

        // 特殊界面 5：脉象推演为翡翠微光青
        assertEquals(PulseSpotlightColor, resolveSpotlightColor(AppScreen.PULSE_MEASURE))
        assertEquals(PulseSpotlightColor, resolveSpotlightColor(AppScreen.PULSE_RESULT))
        assertEquals(Color(0xFF00E5A3), PulseSpotlightColor)
    }
}
