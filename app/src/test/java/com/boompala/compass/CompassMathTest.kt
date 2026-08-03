package com.boompala.compass

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class CompassMathTest {
    @Test fun compassUiUpdateLimiterCapsSensorDrivenRecompositionsAt15Hz() {
        val limiter = CompassUiUpdateLimiter()

        val emitted = (0..60).count { frame ->
            limiter.shouldPublish(frame * 16_666_667L)
        }

        assertEquals(16, emitted)
    }

    @Test fun cardinalAndEightDirections() {
        assertEquals("北", CompassMath.reading(0f).eightDirection)
        assertEquals("东", CompassMath.reading(90f).eightDirection)
        assertEquals("南", CompassMath.reading(180f).eightDirection)
        assertEquals("西", CompassMath.reading(270f).eightDirection)
        assertEquals("东北", CompassMath.reading(45f).eightDirection)
        assertEquals("坎", CompassMath.reading(0f).trigram.name)
        assertEquals("震", CompassMath.reading(90f).trigram.name)
        assertEquals("离", CompassMath.reading(180f).trigram.name)
        assertEquals("兑", CompassMath.reading(270f).trigram.name)
    }

    @Test fun sittingMountainIsOppositeFacingMountain() {
        assertEquals("子", CompassMath.reading(0f).mountain.name)
        assertEquals("午", CompassMath.reading(0f).sittingMountain.name)
        assertEquals("酉", CompassMath.reading(90f).sittingMountain.name)
    }

    @Test fun yinYangUsesThreeDragonConvention() {
        assertEquals(YinYang.YANG, CompassMath.reading(345f).mountain.yinYang)
        assertEquals(YinYang.YIN, CompassMath.reading(0f).mountain.yinYang)
        assertEquals(YinYang.YIN, CompassMath.reading(15f).mountain.yinYang)
    }

    @Test fun yuanYunCoversAllNinePeriodsAndWraps() {
        (1..9).forEach { period ->
            val year = 1864 + (period - 1) * 20
            assertEquals(period, YuanYunData.periodFor(LocalDate.of(year, 2, 4)).number)
        }
        assertEquals(9, YuanYunData.periodFor(LocalDate.of(2026, 7, 31)).number)
        assertEquals(1, YuanYunData.periodFor(LocalDate.of(2044, 2, 4)).number)
        assertEquals(9, YuanYunData.periodFor(LocalDate.of(2044, 2, 3)).number)
    }

    @Test fun starStrengthFollowsCurrentNextPreviousRule() {
        assertEquals("旺气", YuanYunData.status(9, 9))
        assertEquals("生气", YuanYunData.status(1, 9))
        assertEquals("退气", YuanYunData.status(8, 9))
    }

    @Test fun normalizeAndWrapSmoothly() {
        assertEquals(359f, CompassMath.normalize(-1f), 0.001f)
        assertEquals(0f, CompassMath.normalize(360f), 0.001f)
        val next = CompassMath.smooth(359f, 0f, 0.5f)
        assertEquals(359.5f, next, 0.001f)
        assertTrue(CompassMath.angularDistance(359f, 1f) < 3f)
    }

    @Test fun allMountainBoundariesAreUniqueAndContinuous() {
        assertEquals(24, CompassMath.mountains.size)
        assertEquals("子", CompassMath.reading(0f).mountain.name)
        assertEquals("癸", CompassMath.reading(7.5f).mountain.name)
        assertEquals("丑", CompassMath.reading(22.5f).mountain.name)
        assertEquals("壬", CompassMath.reading(352.49f).mountain.name)
        assertEquals("子", CompassMath.reading(352.5f).mountain.name)
        CompassMath.mountains.drop(1).forEach { mountain ->
            assertEquals(mountain.name, CompassMath.reading(mountain.startDegrees).mountain.name)
        }
    }

    @Test fun mountainCentersFollowTraditionalOrder() {
        CompassMath.mountains.forEachIndexed { index, mountain -> assertEquals(index * 15, mountain.centerDegrees) }
    }

    @Test fun lockResumeAccuracyAvailabilityAndLifecycle() {
        var session = CompassSession().onVisible().onSensor(42f, 1)
        assertTrue(session.listening)
        assertEquals(1, session.accuracy)
        session = session.lock().onSensor(90f, 3)
        assertEquals(42f, session.displayedHeading!!, 0.001f)
        session = session.resumeLive()
        assertEquals(90f, session.displayedHeading!!, 0.001f)
        session = session.onHidden()
        assertTrue(!session.listening)
        session = session.onVisible()
        assertTrue(session.listening)
        session = session.unavailable()
        assertTrue(!session.sensorAvailable)
        assertEquals(null, session.displayedHeading)
    }
}
