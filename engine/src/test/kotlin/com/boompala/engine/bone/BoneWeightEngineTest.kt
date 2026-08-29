package com.boompala.engine.bone

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class BoneWeightEngineTest {

    @Test
    fun `calculates bone weight and returns matching poem`() {
        val reading = BoneWeightEngine.calculate(
            birthDate = LocalDate.of(1990, 5, 15),
            birthHour = 14,
        )

        assertTrue(reading.totalWeightQian in 21..72)
        assertEquals(reading.totalWeightQian / 10, reading.totalLiang)
        assertEquals(reading.totalWeightQian % 10, reading.remainderQian)
        assertTrue(reading.formattedWeightZh.contains("两"))
        assertTrue(reading.formattedWeightZh.contains("钱"))
        assertEquals(4, reading.poemLines.size)
        assertTrue(reading.explanationZh.isNotBlank())
        assertTrue(reading.lunarDateText.contains("农历"))
    }
}
