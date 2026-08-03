package com.boompala.engine.rules

import com.boompala.engine.model.HexagramPattern
import com.boompala.engine.model.Palace
import com.boompala.engine.model.PalaceStage
import com.boompala.engine.model.YaoPolarity
import com.boompala.engine.model.YaoPosition
import org.junit.Assert.assertEquals
import org.junit.Test

class HexagramRulesTest {
    @Test
    fun `qian palace covers all eight palace stages and shi positions`() {
        val cases = listOf(
            Triple("111111", PalaceStage.PURE, YaoPosition.TOP),
            Triple("011111", PalaceStage.FIRST, YaoPosition.FIRST),
            Triple("001111", PalaceStage.SECOND, YaoPosition.SECOND),
            Triple("000111", PalaceStage.THIRD, YaoPosition.THIRD),
            Triple("000011", PalaceStage.FOURTH, YaoPosition.FOURTH),
            Triple("000001", PalaceStage.FIFTH, YaoPosition.FIFTH),
            Triple("000101", PalaceStage.WANDERING_SOUL, YaoPosition.FOURTH),
            Triple("111101", PalaceStage.RETURNING_SOUL, YaoPosition.THIRD),
        )

        cases.forEach { (code, stage, shi) ->
            val result = HexagramRules.classify(pattern(code))
            assertEquals(code, Palace.QIAN, result.palace)
            assertEquals(code, stage, result.palaceStage)
            assertEquals(code, shi, result.shiPosition)
            val expectedYingIndex = (shi.indexFromBottom + 3) % 6
            assertEquals(code, YaoPosition.entries[expectedYingIndex], result.yingPosition)
        }
    }

    @Test
    fun `all sixty four patterns form eight complete palace sequences`() {
        val classifications = (0 until 64).map { value ->
            val code = value.toString(2).padStart(6, '0')
            HexagramRules.classify(pattern(code))
        }

        Palace.entries.forEach { palace ->
            val palaceHexagrams = classifications.filter { it.palace == palace }
            assertEquals(8, palaceHexagrams.size)
            assertEquals(PalaceStage.entries.toSet(), palaceHexagrams.map { it.palaceStage }.toSet())
        }
    }

    private fun pattern(codeFromBottom: String): HexagramPattern =
        HexagramPattern(
            codeFromBottom.map { bit ->
                if (bit == '1') YaoPolarity.YANG else YaoPolarity.YIN
            },
        )
}
