package com.boompala.engine.meihua

import com.boompala.engine.model.DivinationTimeInfo
import com.boompala.engine.model.YaoPolarity
import com.boompala.engine.model.YaoPosition
import com.boompala.engine.rules.Trigram

/** The four fixed values used by this project's time-casting convention. */
data class MeiHuaCastingNumbers(
    val yearBranch: Int,
    val lunarMonth: Int,
    val lunarDay: Int,
    val hourBranch: Int,
) {
    init {
        require(yearBranch in 1..12)
        require(lunarMonth in 1..12)
        require(lunarDay in 1..30)
        require(hourBranch in 1..12)
    }

    val upperSum: Int
        get() = yearBranch + lunarMonth + lunarDay

    val lowerSum: Int
        get() = upperSum + hourBranch
}

data class MeiHuaHexagram(
    val name: String,
    val upperTrigram: Trigram,
    val lowerTrigram: Trigram,
    val linesFromBottom: List<YaoPolarity>,
) {
    init {
        require(linesFromBottom.size == YaoPosition.entries.size)
        require(linesFromBottom.take(3) == lowerTrigram.linesFromBottom)
        require(linesFromBottom.drop(3) == upperTrigram.linesFromBottom)
    }

    val codeFromBottom: String
        get() = linesFromBottom.joinToString("") {
            if (it == YaoPolarity.YANG) "1" else "0"
        }
}

/**
 * Mei Hua output deliberately contains only its own time-casting fields. It
 * never carries a LiuYaoResult or six-yao NaJia data.
 */
data class MeiHuaTimeReading(
    val timeInfo: DivinationTimeInfo,
    val numbers: MeiHuaCastingNumbers,
    val upperTrigram: Trigram,
    val lowerTrigram: Trigram,
    val original: MeiHuaHexagram,
    val mutual: MeiHuaHexagram,
    val changed: MeiHuaHexagram,
    val movingPosition: YaoPosition,
    val bodyTrigram: Trigram,
    val useTrigram: Trigram,
)
