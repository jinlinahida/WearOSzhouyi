package com.boompala.engine.ninestar

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class NineStarKiEngineTest {

    @Test
    fun `calculates nine star ki reading correctly`() {
        val reading = NineStarKiEngine.calculate(LocalDate.of(1990, 5, 15))
        assertNotNull(reading.yearStar)
        assertNotNull(reading.monthStar)
        assertTrue(reading.yearStar.nameZh.isNotBlank())
        assertTrue(reading.monthStar.nameZh.isNotBlank())
        assertTrue(reading.yearStar.luckyDirectionsZh.isNotBlank())
        assertTrue(reading.yearStar.personalityZh.isNotBlank())
        assertTrue(reading.energyThemeZh.contains("命"))
        assertTrue(reading.shortSummaryZh.contains("本命"))
    }
}
