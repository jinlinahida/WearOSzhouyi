package com.boompala.engine.bazi

import com.boompala.engine.model.EarthlyBranch
import com.boompala.engine.model.Ganzhi
import com.boompala.engine.model.HeavenlyStem
import com.nlf.calendar.EightChar
import com.nlf.calendar.Solar
import java.time.LocalDate

/**
 * Pure engine for deriving [BaziProfile] from birth date, hour and gender.
 * Backed by 6tail/lunar-java.
 */
object BaziEngine {

    /**
     * Calculates the complete Bazi profile.
     *
     * @param birthDate Gregorian birth date.
     * @param birthHour Birth hour (0..23). If null, time is unknown and only the first
     *                  three pillars (year, month, day) are computed; hourPillar is null.
     * @param gender Male (乾造) or Female (坤造).
     * @param lateZiCountsAsNextDay Whether 23:00-23:59 belongs to the following day pillar.
     */
    fun calculate(
        birthDate: LocalDate,
        birthHour: Int?,
        gender: BaziGender = BaziGender.MALE,
        lateZiCountsAsNextDay: Boolean = true,
    ): BaziProfile {
        require(birthHour == null || birthHour in 0..23) {
            "Birth hour must be in 0..23 or null, got: $birthHour"
        }

        val sampleHour = birthHour ?: 12
        val solar = Solar.fromYmdHms(
            birthDate.year,
            birthDate.monthValue,
            birthDate.dayOfMonth,
            sampleHour,
            0,
            0,
        )
        val lunar = solar.getLunar()
        val eightChar = EightChar.fromLunar(lunar).apply {
            setSect(if (lateZiCountsAsNextDay) 1 else 2)
        }

        val yearGanzhi = parseGanzhi(eightChar.getYear())
        val monthGanzhi = parseGanzhi(eightChar.getMonth())
        val dayGanzhi = parseGanzhi(eightChar.getDay())

        val yearPillar = BaziPillar(
            ganzhi = yearGanzhi,
            naYin = eightChar.getYearNaYin(),
            hiddenStems = parseStems(eightChar.getYearHideGan()),
            stemShiShen = eightChar.getYearShiShenGan(),
            branchShiShen = eightChar.getYearShiShenZhi(),
            diShi = eightChar.getYearDiShi(),
            xun = eightChar.getYearXun(),
            xunKong = eightChar.getYearXunKong(),
        )

        val monthPillar = BaziPillar(
            ganzhi = monthGanzhi,
            naYin = eightChar.getMonthNaYin(),
            hiddenStems = parseStems(eightChar.getMonthHideGan()),
            stemShiShen = eightChar.getMonthShiShenGan(),
            branchShiShen = eightChar.getMonthShiShenZhi(),
            diShi = eightChar.getMonthDiShi(),
            xun = eightChar.getMonthXun(),
            xunKong = eightChar.getMonthXunKong(),
        )

        val dayPillar = BaziPillar(
            ganzhi = dayGanzhi,
            naYin = eightChar.getDayNaYin(),
            hiddenStems = parseStems(eightChar.getDayHideGan()),
            stemShiShen = "日主",
            branchShiShen = eightChar.getDayShiShenZhi(),
            diShi = eightChar.getDayDiShi(),
            xun = eightChar.getDayXun(),
            xunKong = eightChar.getDayXunKong(),
        )

        val hourPillar = if (birthHour != null) {
            val hourGanzhi = parseGanzhi(eightChar.getTime())
            BaziPillar(
                ganzhi = hourGanzhi,
                naYin = eightChar.getTimeNaYin(),
                hiddenStems = parseStems(eightChar.getTimeHideGan()),
                stemShiShen = eightChar.getTimeShiShenGan(),
                branchShiShen = eightChar.getTimeShiShenZhi(),
                diShi = eightChar.getTimeDiShi(),
                xun = eightChar.getTimeXun(),
                xunKong = eightChar.getTimeXunKong(),
            )
        } else {
            null
        }

        val shengXiao = runCatching { lunar.getYearShengXiaoExact() }
            .getOrDefault(lunar.getYearShengXiao())

        val allStems = listOfNotNull(
            yearGanzhi.heavenlyStem,
            monthGanzhi.heavenlyStem,
            dayGanzhi.heavenlyStem,
            hourPillar?.ganzhi?.heavenlyStem,
        )
        val allBranches = listOfNotNull(
            yearGanzhi.earthlyBranch,
            monthGanzhi.earthlyBranch,
            dayGanzhi.earthlyBranch,
            hourPillar?.ganzhi?.earthlyBranch,
        )
        val allElements = allStems.map { it.element } + allBranches.map { it.element }
        val wuXingDistribution = WuXingDistribution(
            metalCount = allElements.count { it == com.boompala.engine.model.FiveElement.METAL },
            woodCount = allElements.count { it == com.boompala.engine.model.FiveElement.WOOD },
            waterCount = allElements.count { it == com.boompala.engine.model.FiveElement.WATER },
            fireCount = allElements.count { it == com.boompala.engine.model.FiveElement.FIRE },
            earthCount = allElements.count { it == com.boompala.engine.model.FiveElement.EARTH },
        )

        val daYunList = runCatching {
            val yun = eightChar.getYun(if (gender == BaziGender.MALE) 1 else 0)
            yun.daYun.mapNotNull { dy ->
                val gzText = dy.ganZhi ?: ""
                if (gzText.length == 2 && dy.index > 0) {
                    val ganzhi = parseGanzhi(gzText)
                    val stemShiShen = com.nlf.calendar.util.LunarUtil.SHI_SHEN[dayGanzhi.heavenlyStem.displayName + ganzhi.heavenlyStem.displayName] ?: ""
                    DaYunPillar(
                        index = dy.index,
                        startAge = dy.startAge,
                        endAge = dy.endAge,
                        startYear = dy.startYear,
                        endYear = dy.endYear,
                        ganzhi = ganzhi,
                        stemShiShen = stemShiShen,
                    )
                } else null
            }
        }.getOrDefault(emptyList())

        return BaziProfile(
            gender = gender,
            birthDate = birthDate,
            birthHour = birthHour,
            yearPillar = yearPillar,
            monthPillar = monthPillar,
            dayPillar = dayPillar,
            hourPillar = hourPillar,
            dayMaster = dayGanzhi.heavenlyStem,
            dayMasterElement = dayGanzhi.heavenlyStem.element,
            shengXiao = shengXiao,
            taiYuan = eightChar.getTaiYuan(),
            mingGong = eightChar.getMingGong(),
            dayXunKong = parseVoidBranches(eightChar.getDayXunKong()),
            yearXunKong = parseVoidBranches(eightChar.getYearXunKong()),
            daYunList = daYunList,
            wuXingDistribution = wuXingDistribution,
        )
    }

    private fun parseGanzhi(value: String): Ganzhi {
        require(value.length == 2) { "Expected a two-character Ganzhi value, got: $value" }
        val stem = HeavenlyStem.entries.firstOrNull { it.displayName == value[0].toString() }
            ?: error("Unknown heavenly stem returned by lunar-java: ${value[0]}")
        val branch = EarthlyBranch.entries.firstOrNull { it.displayName == value[1].toString() }
            ?: error("Unknown earthly branch returned by lunar-java: ${value[1]}")
        return Ganzhi(stem, branch)
    }

    private fun parseStems(stems: List<String>): List<HeavenlyStem> =
        stems.mapNotNull { name -> HeavenlyStem.entries.firstOrNull { it.displayName == name } }

    private fun parseVoidBranches(xunKongText: String): List<EarthlyBranch> =
        xunKongText.mapNotNull { ch ->
            EarthlyBranch.entries.firstOrNull { it.displayName == ch.toString() }
        }
}
