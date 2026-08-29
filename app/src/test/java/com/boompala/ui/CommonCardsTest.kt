package com.boompala.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CommonCardsTest {

    @Test
    fun cardVisualConstantsMatchReWearBiliSpecification() {
        assertEquals(Color(54, 54, 54, 255), CardBorderColor)
        assertEquals(0.4f.dp, CardBorderWidth)
        assertEquals(Color(38, 38, 38, 77), CardBackgroundColor)
        assertEquals(Color(231, 86, 136, 255), CardHighlightColor)
        assertEquals(RoundedCornerShape(10.dp), CardShape)

        // 半透明深灰背景约 30% alpha (77/255)
        val alphaPercent = CardBackgroundColor.alpha
        assertTrue("Alpha should be around 0.3", alphaPercent in 0.28f..0.32f)
    }

    @Test
    fun buttonDefaultsMatchVisualSpecification() {
        val normalStroke = BoompalaButtonDefaults.borderStroke
        assertEquals(CardBorderWidth, normalStroke.width)

        val highlightedStroke = BoompalaButtonDefaults.highlightedBorderStroke()
        assertEquals(1.dp, highlightedStroke.width)
    }
}
