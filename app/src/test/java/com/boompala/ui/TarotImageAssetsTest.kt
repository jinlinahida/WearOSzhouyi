package com.boompala.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class TarotImageAssetsTest {

    @Test
    fun `card back has valid drawable resource`() {
        assertNotEquals(0, TarotImageAssets.cardBackResId)
    }

    @Test
    fun `all 78 tarot cards map to unique and non-zero drawable resources`() {
        val expectedCodes = mutableListOf<String>()

        // 22 Major Arcana
        for (i in 0..21) {
            expectedCodes.add("major_%02d".format(i))
        }

        // 56 Minor Arcana
        val suits = listOf("wands", "cups", "swords", "pentacles")
        for (suit in suits) {
            for (rank in 1..14) {
                expectedCodes.add("${suit}_%02d".format(rank))
            }
        }

        assertEquals(78, expectedCodes.size)

        val resIds = expectedCodes.map { code ->
            val resId = TarotImageAssets.cardDrawableRes(code)
            assertNotEquals("Drawable for $code should not be 0", 0, resId)
            assertNotEquals("Drawable for $code should not fallback to back", TarotImageAssets.cardBackResId, resId)
            resId
        }

        // All 78 card drawables must be distinct
        assertEquals(78, resIds.distinct().size)
    }
}
