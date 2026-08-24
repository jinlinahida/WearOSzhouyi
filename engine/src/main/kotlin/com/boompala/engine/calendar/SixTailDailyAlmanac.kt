package com.boompala.engine.calendar

import com.boompala.engine.dailyfortune.AuspiciousDirection
import com.boompala.engine.dailyfortune.Direction
import com.boompala.engine.dailyfortune.FortuneDeity
import com.boompala.engine.dailyfortune.LuckyHour
import com.boompala.engine.model.EarthlyBranch
import com.nlf.calendar.LunarTime
import com.nlf.calendar.Solar
import java.time.LocalDate

/**
 * Daily almanac adapter backed by 6tail/lunar-java.
 *
 * The day is sampled at 12:00 (午时) so late-Zi conventions cannot shift the
 * result. Deity directions come from the day-level position getters and are
 * mapped from the library's trigram strings onto the engine [Direction] enum.
 * Lucky hours are the shichen whose celestial deity is one of the six
 * yellow-path (黄道) deities.
 */
class SixTailDailyAlmanac : DailyAlmanacSource {

    override fun almanacDay(dayGregorian: LocalDate): DailyAlmanacInfo {
        val lunar = Solar.fromYmdHms(
            dayGregorian.year,
            dayGregorian.monthValue,
            dayGregorian.dayOfMonth,
            12,
            0,
            0,
        ).getLunar()

        val directions = listOf(
            direction(FortuneDeity.XI_SHEN, lunar.getDayPositionXi(), lunar.getDayPositionXiDesc()),
            direction(
                FortuneDeity.YANG_GUI,
                lunar.getDayPositionYangGui(),
                lunar.getDayPositionYangGuiDesc(),
            ),
        )
        val hours = lunar.times
            .filter { it.tianShen in YELLOW_PATH_DEITIES }
            .map(::luckyHour)

        return DailyAlmanacInfo(directions = directions, hours = hours)
    }

    private fun direction(
        deity: FortuneDeity,
        trigramName: String,
        description: String,
    ): AuspiciousDirection {
        val direction = Direction.entries.firstOrNull { it.trigramName == trigramName }
            ?: error("Unknown deity direction returned by lunar-java: $trigramName")
        return AuspiciousDirection(deity = deity, direction = direction, description = description)
    }

    private fun luckyHour(time: LunarTime): LuckyHour {
        val branch = EarthlyBranch.entries.firstOrNull { it.displayName == time.zhi }
            ?: error("Unknown earthly branch returned by lunar-java: ${time.zhi}")
        return LuckyHour(
            branch = branch,
            periodText = "${time.minHm}-${time.maxHm}",
            deityName = time.tianShen,
            isYellowPath = true,
        )
    }

    private companion object {
        val YELLOW_PATH_DEITIES = setOf("青龙", "明堂", "金匮", "天德", "玉堂", "司命")
    }
}
