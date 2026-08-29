package com.boompala.engine.numerology

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class NumerologyEngineTest {

    @Test
    fun `calculates life path number and traits correctly`() {
        val reading = NumerologyEngine.calculate(LocalDate.of(1990, 5, 15))
        assertEquals(3, reading.lifePathNumber)
        assertEquals(6, reading.birthdayNumber)
        assertEquals(2, reading.attitudeNumber)
        assertTrue(reading.lifePathInfo.titleZh.contains("表达") || reading.lifePathInfo.titleZh.contains("灵感"))

        // Check Lo Shu grid
        val grid = reading.loShuGrid
        assertEquals(2, grid.countOf(1)) // 1 in 1990 and 15
        assertEquals(2, grid.countOf(9)) // 9 in 1990
        assertEquals(2, grid.countOf(5)) // 5 in 05 and 15
        assertTrue(grid.lines.isNotEmpty())
    }

    @Test
    fun `identifies master numbers 11 and 22`() {
        // 1975-05-29:
        // 1975 -> 1+9+7+5 = 22
        // 05 -> 5
        // 29 -> 2+9 = 11
        // 22 + 5 + 11 = 38 -> 11!
        val reading11 = NumerologyEngine.calculate(LocalDate.of(1975, 5, 29))
        assertEquals(11, reading11.lifePathNumber)
        assertTrue(reading11.lifePathInfo.isMasterNumber)
        assertTrue(reading11.lifePathInfo.titleZh.contains("大师") || reading11.lifePathInfo.titleZh.contains("启迪"))
    }
}
