package com.boompala.engine.calendar

import com.boompala.engine.model.DivinationTimeInfo
import com.boompala.engine.model.EarthlyBranch
import com.boompala.engine.model.Ganzhi
import com.boompala.engine.model.HeavenlyStem
import com.nlf.calendar.EightChar
import com.nlf.calendar.Solar
import java.time.Instant
import java.time.ZoneId

/**
 * Four-pillar adapter backed by 6tail/lunar-java.
 *
 * The library is used only through its public API. It supplies exact
 * solar-term boundaries for year/month and exposes both established late-Zi
 * day conventions. `lateZiCountsAsNextDay` defaults to the convention used by
 * this project: a late Zi hour belongs to the following day pillar.
 */
class SixTailGanzhiCalendar(
    private val lateZiCountsAsNextDay: Boolean = true,
) : GanzhiCalendar {

    override fun divinationTimeInfo(
        instant: Instant,
        zoneId: ZoneId,
    ): DivinationTimeInfo {
        val gregorianDateTime = instant.atZone(zoneId)
        val local = gregorianDateTime.toLocalDateTime()
        val lunar = Solar.fromYmdHms(
            local.year,
            local.monthValue,
            local.dayOfMonth,
            local.hour,
            local.minute,
            local.second,
        ).getLunar()
        val eightChar = EightChar.fromLunar(lunar).apply {
            // 6tail: sect 1 = late-Zi day counts as tomorrow,
            // sect 2 = late-Zi day remains today.
            setSect(if (lateZiCountsAsNextDay) 1 else 2)
        }

        val year = parseGanzhi(eightChar.getYear())
        val month = parseGanzhi(eightChar.getMonth())
        val day = parseGanzhi(eightChar.getDay())
        val hourBranch = parseBranch(lunar.getTimeZhi())
        val hourStem = hourStem(day.heavenlyStem, hourBranch)

        return DivinationTimeInfo(
            gregorianDateTime = gregorianDateTime,
            lunarDate = buildString {
                append(lunar.getYearInGanZhi())
                append("年 ")
                if (lunar.getMonth() < 0) append("闰")
                append(lunar.getMonthInChinese())
                append("月")
                append(lunar.getDayInChinese())
                append(" ")
                append(lunar.getTimeZhi())
                append("时")
            },
            lunarYearGanzhi = parseGanzhi(lunar.getYearInGanZhi()),
            lunarMonth = kotlin.math.abs(lunar.getMonth()),
            lunarDay = lunar.getDay(),
            yearGanzhi = year,
            monthGanzhi = month,
            dayGanzhi = day,
            hourGanzhi = Ganzhi(hourStem, hourBranch),
        )
    }

    private fun parseGanzhi(value: String): Ganzhi {
        require(value.length == 2) { "Expected a two-character Ganzhi value, got: $value" }
        return Ganzhi(
            heavenlyStem = parseStem(value[0].toString()),
            earthlyBranch = parseBranch(value[1].toString()),
        )
    }

    private fun parseStem(value: String): HeavenlyStem =
        HeavenlyStem.entries.firstOrNull { it.displayName == value }
            ?: error("Unknown heavenly stem returned by lunar-java: $value")

    private fun parseBranch(value: String): EarthlyBranch =
        EarthlyBranch.entries.firstOrNull { it.displayName == value }
            ?: error("Unknown earthly branch returned by lunar-java: $value")

    /**
     * Hour stem formula: 甲/己日甲子起, then advance one stem per hour branch.
     */
    private fun hourStem(dayStem: HeavenlyStem, hourBranch: EarthlyBranch): HeavenlyStem {
        val startStemIndex = (dayStem.index % 5) * 2
        return HeavenlyStem.entries[(startStemIndex + hourBranch.index) % HeavenlyStem.entries.size]
    }
}
