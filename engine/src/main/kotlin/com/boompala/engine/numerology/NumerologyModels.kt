package com.boompala.engine.numerology

import java.time.LocalDate

/**
 * Archetype profile for Life Path Numbers (1..9 and Master Numbers 11, 22, 33).
 */
data class LifePathInfo(
    val number: Int,
    val titleZh: String,
    val titleEn: String,
    val keywordsZh: String,
    val descriptionZh: String,
    val isMasterNumber: Boolean = false,
)

/**
 * A line (Arrow of Pythagoras) in the 3x3 Lo Shu Numerology grid.
 */
data class LoShuLine(
    val nameZh: String,
    val digits: List<Int>,
    val isComplete: Boolean,
    val descriptionZh: String,
)

/**
 * The 3x3 Lo Shu Numerology Grid based on birth date digits.
 */
data class LoShuGrid(
    val digitCounts: Map<Int, Int>,
    val lines: List<LoShuLine>,
) {
    fun countOf(digit: Int): Int = digitCounts[digit] ?: 0
}

/**
 * Complete Pythagorean Numerology reading.
 */
data class NumerologyReading(
    val birthDate: LocalDate,
    val lifePathNumber: Int,
    val lifePathInfo: LifePathInfo,
    val birthdayNumber: Int,
    val attitudeNumber: Int,
    val personalYearNumber: Int,
    val loShuGrid: LoShuGrid,
) {
    val shortSummaryZh: String
        get() = "生命道路数 ${lifePathNumber} · ${lifePathInfo.titleZh}"
}
