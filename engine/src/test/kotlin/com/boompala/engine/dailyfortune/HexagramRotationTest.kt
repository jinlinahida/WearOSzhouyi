package com.boompala.engine.dailyfortune

import com.boompala.engine.model.YaoPosition
import com.nlf.calendar.Solar
import org.junit.Assert.assertEquals
import org.junit.Test

class HexagramRotationTest {

    @Test
    fun `epoch is a real jiazi day according to 6tail`() {
        val epoch = HexagramRotation.EPOCH
        val dayGanzhi = Solar.fromYmd(epoch.year, epoch.monthValue, epoch.dayOfMonth)
            .getLunar()
            .getDayInGanZhi()

        assertEquals("甲子", dayGanzhi)
        assertEquals(0, HexagramRotation.rotationIndexOf(epoch))
    }

    @Test
    fun `384 consecutive days cover every hexagram line pair exactly once`() {
        val epoch = HexagramRotation.EPOCH
        val pairs = (0 until HexagramRotation.ROTATION_LENGTH).map { offset ->
            val index = HexagramRotation.rotationIndexOf(epoch.plusDays(offset.toLong()))
            assertEquals(offset, index)
            HexagramRotation.hexagramCodeOf(index) to HexagramRotation.linePositionOf(index)
        }

        // 64 hexagrams x 6 lines, no duplicates, no gaps.
        assertEquals(384, pairs.size)
        assertEquals(384, pairs.toSet().size)
        assertEquals(64, pairs.map { it.first }.toSet().size)

        // Every hexagram rules exactly six consecutive days, first line -> top line.
        pairs.groupBy { it.first }.forEach { (_, lines) ->
            assertEquals(YaoPosition.entries, lines.map { it.second })
        }
    }

    @Test
    fun `rotation wraps for dates before the epoch`() {
        assertEquals(383, HexagramRotation.rotationIndexOf(HexagramRotation.EPOCH.minusDays(1)))
        assertEquals(0, HexagramRotation.rotationIndexOf(HexagramRotation.EPOCH.plusDays(384)))
        assertEquals(1, HexagramRotation.rotationIndexOf(HexagramRotation.EPOCH.plusDays(385)))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `out of range slot is rejected`() {
        HexagramRotation.hexagramCodeOf(384)
    }
}
