package com.boompala.engine.model

import com.boompala.engine.BasicHexagramEngine
import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class DirectHexagramInputTest {
    @Test
    fun `polarity differences derive moving lines through existing engine input`() {
        val original = listOf(
            YaoPolarity.YIN,
            YaoPolarity.YANG,
            YaoPolarity.YIN,
            YaoPolarity.YANG,
            YaoPolarity.YIN,
            YaoPolarity.YANG,
        )
        val changed = listOf(
            YaoPolarity.YANG,
            YaoPolarity.YANG,
            YaoPolarity.YIN,
            YaoPolarity.YIN,
            YaoPolarity.YIN,
            YaoPolarity.YANG,
        )

        val engineInput = DirectHexagramInput(
            originalLinesFromBottom = original.toInputs(),
            changedLinesFromBottom = changed.toInputs(),
            castAt = Instant.parse("2026-07-30T14:00:00Z"),
            zoneId = ZoneId.of("Asia/Shanghai"),
        ).toEngineInput()

        assertEquals(
            listOf(
                YaoState.OLD_YIN,
                YaoState.YOUNG_YANG,
                YaoState.YOUNG_YIN,
                YaoState.OLD_YANG,
                YaoState.YOUNG_YIN,
                YaoState.YOUNG_YANG,
            ),
            engineInput.linesFromBottom.map { it.state },
        )
        val basicResult = BasicHexagramEngine.derive(engineInput)
        assertEquals("010101", basicResult.original.codeFromBottom)
        assertEquals("110001", basicResult.changed?.codeFromBottom)
        assertEquals(
            listOf(YaoPosition.FIRST, YaoPosition.FOURTH),
            basicResult.changingPositions,
        )
    }

    private fun List<YaoPolarity>.toInputs(): List<YaoPolarityInput> =
        mapIndexed { index, polarity ->
            YaoPolarityInput(YaoPosition.entries[index], polarity)
        }
}
