package com.boompala.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppSettingsTest {

    @Test
    fun `defaults are auto standard with animations, rotary, haptics, chinese and default home order`() {
        assertEquals(
            AppSettings(
                screenMode = ScreenMode.AUTO,
                contentSize = ContentSize.STANDARD,
                animationsEnabled = true,
                rotaryScrollingEnabled = true,
                hapticFeedbackEnabled = true,
                language = AppLanguage.CHINESE,
                homeOrder = HomeFeature.DEFAULT_ORDER,
                hiddenHomeFeatures = emptySet(),
            ),
            AppSettings.DEFAULT,
        )
    }

    @Test
    fun `screen mode resolves auto and manual overrides`() {
        assertEquals(
            ScreenShape.ROUND,
            AppSettings(screenMode = ScreenMode.AUTO).resolvedScreenShape(isRoundDevice = true),
        )
        assertEquals(
            ScreenShape.SQUARE,
            AppSettings(screenMode = ScreenMode.AUTO).resolvedScreenShape(isRoundDevice = false),
        )
        assertEquals(
            ScreenShape.ROUND,
            AppSettings(screenMode = ScreenMode.ROUND).resolvedScreenShape(isRoundDevice = false),
        )
        assertEquals(
            ScreenShape.SQUARE,
            AppSettings(screenMode = ScreenMode.SQUARE).resolvedScreenShape(isRoundDevice = true),
        )
    }

    @Test
    fun `home features order and visibility work as expected`() {
        val settings = AppSettings.DEFAULT.copy(
            homeOrder = listOf(HomeFeature.TAROT_ONE, HomeFeature.SIX_YAO),
            hiddenHomeFeatures = setOf(HomeFeature.SIX_YAO),
        )
        val effective = settings.effectiveHomeOrder()
        assertEquals(HomeFeature.TAROT_ONE, effective.first())
        assertEquals(HomeFeature.SIX_YAO, effective[1])
        assertEquals(HomeFeature.DEFAULT_ORDER.size, effective.size)

        val visible = settings.visibleHomeFeatures()
        assertFalse(visible.contains(HomeFeature.SIX_YAO))
        assertTrue(visible.contains(HomeFeature.TAROT_ONE))
    }

    @Test
    fun `home feature fromId resolves correctly with fallback`() {
        assertEquals(HomeFeature.SIX_YAO, HomeFeature.fromId("six_yao"))
        assertEquals(HomeFeature.DAILY_FORTUNE, HomeFeature.fromId("daily_fortune"))
        assertEquals(null, HomeFeature.fromId("non_existent"))
    }
}
