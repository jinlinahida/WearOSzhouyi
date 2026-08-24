package com.boompala.engine.dailyfortune

import com.boompala.engine.model.YaoPosition
import com.boompala.engine.rules.HexagramCatalog
import java.time.LocalDate

/**
 * The deterministic 384-day hexagram rotation used by the daily fortune.
 *
 * [EPOCH] is a real 甲子 day (verified through 6tail
 * `Solar.fromYmd(1984, 1, 31).getLunar().getDayInGanZhi()` == "甲子" and
 * locked by tests). Each day advances one step through the King Wen order,
 * spending six consecutive days per hexagram; the ruling line walks from 初爻
 * to 上爻 within those six days. The full cycle therefore covers 64 × 6 = 384
 * distinct (hexagram, line) pairs with no randomness.
 */
internal object HexagramRotation {
    val EPOCH: LocalDate = LocalDate.of(1984, 1, 31)

    const val ROTATION_LENGTH = 384

    private const val DAYS_PER_HEXAGRAM = 6

    /** Rotation slot in 0..383 for the given local date. */
    fun rotationIndexOf(date: LocalDate): Int {
        val dayIndex = date.toEpochDay() - EPOCH.toEpochDay()
        return Math.floorMod(dayIndex, ROTATION_LENGTH)
    }

    /** King Wen order hexagram code (bottom-to-top bits) ruling the slot. */
    fun hexagramCodeOf(rotationIndex: Int): String {
        require(rotationIndex in 0 until ROTATION_LENGTH) {
            "Rotation index must be in 0 until $ROTATION_LENGTH, got $rotationIndex."
        }
        return HexagramCatalog.zhouOrderCodes[rotationIndex / DAYS_PER_HEXAGRAM]
    }

    /** Ruling line of the slot, advancing 初爻 -> 上爻 over the six days. */
    fun linePositionOf(rotationIndex: Int): YaoPosition {
        require(rotationIndex in 0 until ROTATION_LENGTH) {
            "Rotation index must be in 0 until $ROTATION_LENGTH, got $rotationIndex."
        }
        return YaoPosition.entries[rotationIndex % DAYS_PER_HEXAGRAM]
    }
}
