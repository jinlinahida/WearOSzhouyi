package com.boompala.engine.dailyfortune

import com.boompala.engine.calendar.DailyAlmanacSource
import com.boompala.engine.calendar.GanzhiCalendar
import com.boompala.engine.data.EmptyHexagramInterpretationRepository
import com.boompala.engine.data.EmptyLineTextRepository
import com.boompala.engine.data.HexagramInterpretationRepository
import com.boompala.engine.data.LineTextRepository
import com.boompala.engine.rules.HexagramCatalog
import java.time.Instant
import java.time.ZoneId

/**
 * Pure daily-fortune assembly. Stateless; every output is a deterministic
 * function of the device-zone Gregorian date with no randomness involved.
 *
 * The day boundary follows the local calendar date: the reading is computed
 * from the local date of [instant] and all calendar queries are sampled at
 * 12:00 (午时) of that date, so any moment within the same natural day yields
 * the identical reading while 23:59 and the next 00:01 belong to different
 * days.
 */
class DailyFortuneEngine(
    private val calendar: GanzhiCalendar,
    private val almanac: DailyAlmanacSource,
    private val lineTextRepository: LineTextRepository = EmptyLineTextRepository,
    private val interpretationRepository: HexagramInterpretationRepository = EmptyHexagramInterpretationRepository,
) {

    fun fortuneFor(instant: Instant, zoneId: ZoneId): DailyFortuneReading {
        val localDate = instant.atZone(zoneId).toLocalDate()
        val noon = localDate.atTime(12, 0).atZone(zoneId).toInstant()
        val timeInfo = calendar.divinationTimeInfo(noon, zoneId)
        val almanacDay = almanac.almanacDay(localDate)

        val rotationIndex = HexagramRotation.rotationIndexOf(localDate)
        val hexagramCode = HexagramRotation.hexagramCodeOf(rotationIndex)
        val linePosition = HexagramRotation.linePositionOf(rotationIndex)
        val interpretation = interpretationRepository.interpretationFor(hexagramCode)
        val dayStemElement = timeInfo.dayGanzhi.heavenlyStem.element

        return DailyFortuneReading(
            date = localDate,
            lunarDateText = timeInfo.lunarDate,
            dayGanzhi = timeInfo.dayGanzhi,
            dayStemElement = dayStemElement,
            dayHexagramCode = hexagramCode,
            dayHexagramName = HexagramCatalog.nameFor(hexagramCode),
            rotationIndex = rotationIndex,
            hexagramSummary = interpretation?.coreMeaning,
            hexagramAdvice = interpretation?.advice,
            dayLinePosition = linePosition,
            dayLineText = lineTextRepository.lineText(hexagramCode, linePosition),
            luckyColor = DailyFortuneRules.luckyColor(dayStemElement),
            supportColor = DailyFortuneRules.supportColor(dayStemElement),
            avoidColor = DailyFortuneRules.avoidColor(dayStemElement),
            luckyNumbers = DailyFortuneRules.luckyNumbers(dayStemElement),
            directions = almanacDay.directions,
            hours = almanacDay.hours,
            jianChu = JianChuRules.fromDayAndMonth(
                dayBranch = timeInfo.dayGanzhi.earthlyBranch,
                solarTermMonthBranch = timeInfo.monthGanzhi.earthlyBranch,
            ),
        )
    }
}
