package com.boompala.engine.astrology

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class WesternAstrologyEngineTest {

    @Test
    fun `calculates sun sign and element correctly for known dates`() {
        // May 15, 1990 is Taurus (金牛座, Earth)
        val reading1 = WesternAstrologyEngine.calculate(
            birthDate = LocalDate.of(1990, 5, 15),
            birthHour = 14,
        )
        assertEquals(ZodiacSign.TAURUS, reading1.sun.sign)
        assertEquals(ZodiacElement.EARTH, reading1.sun.sign.element)
        assertEquals(CelestialBody.SUN, reading1.sun.body)

        // July 28, 1995 is Leo (狮子座, Fire)
        val reading2 = WesternAstrologyEngine.calculate(
            birthDate = LocalDate.of(1995, 7, 28),
            birthHour = 10,
        )
        assertEquals(ZodiacSign.LEO, reading2.sun.sign)
        assertEquals(ZodiacElement.FIRE, reading2.sun.sign.element)

        // October 28, 2000 is Scorpio (天蝎座, Water)
        val reading3 = WesternAstrologyEngine.calculate(
            birthDate = LocalDate.of(2000, 10, 28),
            birthHour = 18,
        )
        assertEquals(ZodiacSign.SCORPIO, reading3.sun.sign)
        assertEquals(ZodiacElement.WATER, reading3.sun.sign.element)

        // February 5, 1992 is Aquarius (水瓶座, Air)
        val reading4 = WesternAstrologyEngine.calculate(
            birthDate = LocalDate.of(1992, 2, 5),
            birthHour = 8,
        )
        assertEquals(ZodiacSign.AQUARIUS, reading4.sun.sign)
        assertEquals(ZodiacElement.AIR, reading4.sun.sign.element)
    }

    @Test
    fun `evaluates four elements balance and big three summary`() {
        val reading = WesternAstrologyEngine.calculate(
            birthDate = LocalDate.of(1990, 5, 15),
            birthHour = 14,
        )

        val balance = reading.elementBalance
        assertEquals(8, balance.totalCount)
        assertEquals(8, balance.fireCount + balance.earthCount + balance.airCount + balance.waterCount)
        assertTrue(balance.balanceSummaryZh.isNotBlank())

        assertNotNull(reading.ascendant)
        assertTrue(reading.bigThreeSummary.contains("日"))
        assertTrue(reading.bigThreeSummary.contains("月"))
        assertTrue(reading.bigThreeSummary.contains("升"))
        assertTrue(reading.planets.size >= 8)
    }

    @Test
    fun `supports unknown hour leaving ascendant null`() {
        val reading = WesternAstrologyEngine.calculate(
            birthDate = LocalDate.of(1990, 5, 15),
            birthHour = null,
        )
        assertEquals(ZodiacSign.TAURUS, reading.sun.sign)
        assertNull(reading.ascendant)
        assertEquals(7, reading.elementBalance.totalCount)
        assertTrue(!reading.bigThreeSummary.contains("升"))
    }
}
