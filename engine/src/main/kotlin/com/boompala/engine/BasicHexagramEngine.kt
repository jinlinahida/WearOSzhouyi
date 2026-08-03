package com.boompala.engine

import com.boompala.engine.model.HexagramInput
import com.boompala.engine.model.HexagramPattern
import com.boompala.engine.model.YaoPolarity
import com.boompala.engine.model.YaoPosition

data class BasicHexagramResult(
    val original: HexagramPattern,
    val changed: HexagramPattern?,
    val changingPositions: List<YaoPosition>,
) {
    val hasChangingLines: Boolean
        get() = changingPositions.isNotEmpty()
}

/**
 * Derives only the unambiguous line-level result from manual six-yao input.
 *
 * Naming, palace assignment, Najia, six relatives, six spirits, and calendar
 * calculations remain outside this small primitive; `LiuYaoEngine` composes
 * the confirmed rule implementations around it.
 */
object BasicHexagramEngine {
    fun derive(input: HexagramInput): BasicHexagramResult {
        val originalLines = input.linesFromBottom.map { line ->
            line.state.toPolarity()
        }
        val changingPositions = input.linesFromBottom
            .filter { it.state.isChanging }
            .map { it.position }

        val changedLines = input.linesFromBottom.map { line ->
            if (line.state.isChanging) {
                line.state.toPolarity().opposite()
            } else {
                line.state.toPolarity()
            }
        }

        return BasicHexagramResult(
            original = HexagramPattern(originalLines),
            changed = changedLines.takeUnless { changingPositions.isEmpty() }
                ?.let(::HexagramPattern),
            changingPositions = changingPositions,
        )
    }

    private fun com.boompala.engine.model.YaoState.toPolarity(): YaoPolarity =
        if (isYang) YaoPolarity.YANG else YaoPolarity.YIN

    private fun YaoPolarity.opposite(): YaoPolarity =
        if (this == YaoPolarity.YANG) YaoPolarity.YIN else YaoPolarity.YANG
}
