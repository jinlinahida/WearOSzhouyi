package com.boompala.engine.ninestar

import com.boompala.engine.model.FiveElement
import com.nlf.calendar.NineStar
import java.time.LocalDate

/**
 * Detailed profile for a single Nine Star (一白水星至九紫火星).
 */
data class NineStarProfile(
    val index: Int, // 1..9
    val nameZh: String, // e.g. "一白水星"
    val trigramZh: String, // e.g. "坎"
    val element: FiveElement,
    val colorZh: String,
    val natureZh: String,
    val luckyDirectionsZh: String,
    val personalityZh: String,
)

/**
 * Reading from Nine Star Ki Astrology (九星气学命盘).
 */
data class NineStarKiReading(
    val birthDate: LocalDate,
    val yearStar: NineStarProfile,
    val monthStar: NineStarProfile,
    val solarTermText: String,
    val energyThemeZh: String,
) {
    val shortSummaryZh: String
        get() = "本命 ${yearStar.nameZh} · 月命 ${monthStar.nameZh}"
}
