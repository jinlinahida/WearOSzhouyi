package com.boompala.engine.bone

import java.time.LocalDate

/**
 * Reading from Yuan Tiangang Bone Weight Astrology (袁天罡称骨算命).
 */
data class BoneWeightReading(
    val birthDate: LocalDate,
    val birthHour: Int?,
    val lunarDateText: String,
    val yearGanzhi: String,
    val yearWeightQian: Int,
    val monthWeightQian: Int,
    val dayWeightQian: Int,
    val hourWeightQian: Int,
    val totalWeightQian: Int, // e.g. 36 -> 3两6钱
    val poemLines: List<String>,
    val explanationZh: String,
) {
    val totalLiang: Int get() = totalWeightQian / 10
    val remainderQian: Int get() = totalWeightQian % 10

    val formattedWeightZh: String
        get() = "${totalLiang}两${remainderQian}钱"

    val shortSummaryZh: String
        get() = "称骨总重 · ${formattedWeightZh}"
}
