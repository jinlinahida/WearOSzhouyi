package com.boompala.engine.liuyao

import com.boompala.engine.model.YaoPosition
import com.boompala.engine.model.YaoState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class NumberCastingEngineTest {

    @Test
    fun testCastThreeNumbers() {
        // 上卦 1(乾 111)，下卦 8(坤 000)，动爻 1(初爻)
        // 卦象 codeFromBottom = "000111" (天地否)
        // 动爻在初爻，原为阴爻(0)，变为老阴(OLD_YIN, 6)
        val input = NumberCastingEngine.castThreeNumbers(
            numA = 1,
            numB = 8,
            numC = 1,
            castAt = Instant.now(),
            zoneId = ZoneId.systemDefault(),
        )

        assertEquals(6, input.linesFromBottom.size)
        assertEquals(YaoState.OLD_YIN, input.linesFromBottom[0].state)
        assertEquals(YaoState.YOUNG_YIN, input.linesFromBottom[1].state)
        assertEquals(YaoState.YOUNG_YANG, input.linesFromBottom[3].state)
    }

    @Test
    fun testCastTwoNumbers() {
        // A=5(巽 011), B=3(离 101), hourBranchNumber=1 (子时 1)
        // 动爻 = (5 + 3 + 1) mod 6 = 9 mod 6 = 3 (三爻)
        val input = NumberCastingEngine.castTwoNumbers(
            numA = 5,
            numB = 3,
            hourBranchNumber = 1,
            castAt = Instant.now(),
            zoneId = ZoneId.systemDefault(),
        )

        assertEquals(6, input.linesFromBottom.size)
        assertTrue(input.linesFromBottom[2].state.isChanging)
    }
}
