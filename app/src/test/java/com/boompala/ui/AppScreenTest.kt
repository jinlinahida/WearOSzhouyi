package com.boompala.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppScreenTest {
    @Test
    fun backDestinationFollowsScreenHierarchy() {
        assertEquals(AppScreen.SETTINGS, AppScreen.ABOUT.backDestination())
        assertEquals(AppScreen.HOME, AppScreen.SETTINGS.backDestination())
        assertEquals(AppScreen.YAO_INPUT, AppScreen.RESULT.backDestination())
        assertEquals(AppScreen.MEIHUA_TIME, AppScreen.MEIHUA_RESULT.backDestination())
        assertEquals(AppScreen.HOME, AppScreen.MEIHUA_TIME.backDestination())
        assertEquals(AppScreen.HOME, AppScreen.YAO_INPUT.backDestination())
        assertNull(AppScreen.HOME.backDestination())
    }

    @Test
    fun `shared hexagram display keeps line polarity and motion separate`() {
        val display = HexagramDisplayModel(
            name = "测试卦",
            linesFromBottom = listOf(true, false, true, false, true, false),
            movingPositions = setOf(1),
        )

        assertEquals(YaoLineShape.SOLID, display.lineDisplayAt(0).shape)
        assertEquals(YaoLineShape.BROKEN, display.lineDisplayAt(1).shape)
        assertEquals(true, display.lineDisplayAt(1).isMoving)
        assertEquals(false, display.lineDisplayAt(2).isMoving)
    }

    @Test
    fun `trigram preview uses a three-line projection instead of hexagram model`() {
        val lines = listOf(true, false, true)

        assertEquals(YaoLineShape.SOLID, trigramLineDisplayAt(lines, 0).shape)
        assertEquals(YaoLineShape.BROKEN, trigramLineDisplayAt(lines, 1).shape)
        assertEquals(YaoLineShape.SOLID, trigramLineDisplayAt(lines, 2).shape)
    }
}
