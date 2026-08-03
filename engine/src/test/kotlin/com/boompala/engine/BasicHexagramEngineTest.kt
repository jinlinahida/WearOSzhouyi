package com.boompala.engine

import com.boompala.engine.model.HexagramInput
import com.boompala.engine.model.YaoLineInput
import com.boompala.engine.model.YaoPolarity
import com.boompala.engine.model.YaoPosition
import com.boompala.engine.model.YaoState
import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BasicHexagramEngineTest {
    private val castAt = Instant.parse("2026-07-30T11:00:00Z")
    private val zoneId = ZoneId.of("Asia/Shanghai")

    @Test
    fun `all static lines do not produce a changed pattern`() {
        val result = BasicHexagramEngine.derive(
            input(
                YaoState.YOUNG_YANG,
                YaoState.YOUNG_YIN,
                YaoState.YOUNG_YANG,
                YaoState.YOUNG_YIN,
                YaoState.YOUNG_YANG,
                YaoState.YOUNG_YIN,
            ),
        )

        assertFalse(result.hasChangingLines)
        assertEquals(
            listOf(
                YaoPolarity.YANG,
                YaoPolarity.YIN,
                YaoPolarity.YANG,
                YaoPolarity.YIN,
                YaoPolarity.YANG,
                YaoPolarity.YIN,
            ),
            result.original.linesFromBottom,
        )
        assertNull(result.changed)
    }

    @Test
    fun `changing lines flip polarity and retain their positions`() {
        val result = BasicHexagramEngine.derive(
            input(
                YaoState.OLD_YIN,
                YaoState.YOUNG_YANG,
                YaoState.OLD_YANG,
                YaoState.YOUNG_YIN,
                YaoState.YOUNG_YANG,
                YaoState.OLD_YIN,
            ),
        )

        assertTrue(result.hasChangingLines)
        assertEquals(
            listOf(YaoPosition.FIRST, YaoPosition.THIRD, YaoPosition.TOP),
            result.changingPositions,
        )
        assertEquals(
            listOf(
                YaoPolarity.YIN,
                YaoPolarity.YANG,
                YaoPolarity.YANG,
                YaoPolarity.YIN,
                YaoPolarity.YANG,
                YaoPolarity.YIN,
            ),
            result.original.linesFromBottom,
        )
        assertEquals(
            listOf(
                YaoPolarity.YANG,
                YaoPolarity.YANG,
                YaoPolarity.YIN,
                YaoPolarity.YIN,
                YaoPolarity.YANG,
                YaoPolarity.YANG,
            ),
            result.changed?.linesFromBottom,
        )
    }

    private fun input(vararg states: YaoState): HexagramInput =
        HexagramInput(
            linesFromBottom = states.mapIndexed { index, state ->
                YaoLineInput(YaoPosition.entries[index], state)
            },
            castAt = castAt,
            zoneId = zoneId,
        )
}
