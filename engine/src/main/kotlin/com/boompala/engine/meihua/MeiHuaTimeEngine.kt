package com.boompala.engine.meihua

import com.boompala.engine.calendar.GanzhiCalendar
import com.boompala.engine.model.DivinationTimeInfo
import com.boompala.engine.model.YaoPolarity
import com.boompala.engine.model.YaoPosition
import com.boompala.engine.rules.Trigram
import java.time.Instant
import java.time.ZoneId

/** Pure time-casting orchestration; Compose and LiuYaoEngine are not involved. */
class MeiHuaTimeEngine(
    private val calendar: GanzhiCalendar,
) {
    fun calculate(castAt: Instant, zoneId: ZoneId): MeiHuaTimeReading =
        calculate(calendar.divinationTimeInfo(castAt, zoneId))

    fun calculate(timeInfo: DivinationTimeInfo): MeiHuaTimeReading {
        val numbers = MeiHuaCastingNumbers(
            yearBranch = timeInfo.lunarYearGanzhi.earthlyBranch.ordinalNumber(),
            lunarMonth = timeInfo.lunarMonth,
            lunarDay = timeInfo.lunarDay,
            hourBranch = timeInfo.hourGanzhi.earthlyBranch.ordinalNumber(),
        )
        val upper = TrigramRules.fromCastingSum(numbers.upperSum)
        val lower = TrigramRules.fromCastingSum(numbers.lowerSum)
        val moving = YaoPosition.entries[positiveRemainder(numbers.lowerSum, 6) - 1]
        val original = MutualHexagramCalculator.hexagram(upper, lower)
        val changedLines = original.linesFromBottom.toMutableList().apply {
            val index = moving.indexFromBottom
            this[index] = if (this[index] == YaoPolarity.YANG) YaoPolarity.YIN else YaoPolarity.YANG
        }
        val changed = MutualHexagramCalculator.hexagram(
            TrigramRules.fromLines(changedLines.drop(3)),
            TrigramRules.fromLines(changedLines.take(3)),
        )
        val useIsUpper = moving.indexFromBottom >= 3

        return MeiHuaTimeReading(
            timeInfo = timeInfo,
            numbers = numbers,
            upperTrigram = upper,
            lowerTrigram = lower,
            original = original,
            mutual = MutualHexagramCalculator.calculate(original.linesFromBottom),
            changed = changed,
            movingPosition = moving,
            bodyTrigram = if (useIsUpper) lower else upper,
            useTrigram = if (useIsUpper) upper else lower,
        )
    }
}

private fun com.boompala.engine.model.EarthlyBranch.ordinalNumber(): Int = index + 1
