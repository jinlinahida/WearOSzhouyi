package com.boompala.engine.liuyao

import com.boompala.engine.model.HexagramInput
import com.boompala.engine.model.YaoLineInput
import com.boompala.engine.model.YaoPosition
import com.boompala.engine.model.YaoState
import com.boompala.engine.rules.Trigram
import java.time.Instant
import java.time.ZoneId

object NumberCastingEngine {

    fun castThreeNumbers(
        numA: Int,
        numB: Int,
        numC: Int,
        castAt: Instant,
        zoneId: ZoneId,
    ): HexagramInput {
        val upperTrigram = Trigram.fromNumber(mod8(numA))
        val lowerTrigram = Trigram.fromNumber(mod8(numB))
        val movingLineIndex = mod6(numC) - 1

        val codeFromBottom = lowerTrigram.bitsFromBottom + upperTrigram.bitsFromBottom

        val lines = YaoPosition.entries.mapIndexed { index, position ->
            val isYang = codeFromBottom[index] == '1'
            val isMoving = index == movingLineIndex
            val state = when {
                isMoving && isYang -> YaoState.OLD_YANG
                isMoving && !isYang -> YaoState.OLD_YIN
                !isMoving && isYang -> YaoState.YOUNG_YANG
                else -> YaoState.YOUNG_YIN
            }
            YaoLineInput(position = position, state = state)
        }

        return HexagramInput(
            linesFromBottom = lines,
            castAt = castAt,
            zoneId = zoneId,
        )
    }

    fun castTwoNumbers(
        numA: Int,
        numB: Int,
        hourBranchNumber: Int,
        castAt: Instant,
        zoneId: ZoneId,
    ): HexagramInput {
        val movingLineNumber = mod6(numA + numB + hourBranchNumber)
        return castThreeNumbers(
            numA = numA,
            numB = numB,
            numC = movingLineNumber,
            castAt = castAt,
            zoneId = zoneId,
        )
    }

    private fun mod8(n: Int): Int {
        val r = n % 8
        return if (r == 0) 8 else if (r < 0) r + 8 else r
    }

    private fun mod6(n: Int): Int {
        val r = n % 6
        return if (r == 0) 6 else if (r < 0) r + 6 else r
    }
}
