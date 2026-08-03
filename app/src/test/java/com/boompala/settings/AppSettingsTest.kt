package com.boompala.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class AppSettingsTest {

    @Test
    fun `defaults are auto standard with animations and rotary enabled`() {
        assertEquals(
            AppSettings(
                screenMode = ScreenMode.AUTO,
                contentSize = ContentSize.STANDARD,
                animationsEnabled = true,
                rotaryScrollingEnabled = true,
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
}
