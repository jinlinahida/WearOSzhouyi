package com.boompala.engine.bazi

import com.boompala.engine.model.EarthlyBranch
import com.boompala.engine.model.FiveElement
import com.boompala.engine.model.Ganzhi
import com.boompala.engine.model.HeavenlyStem
import java.time.LocalDate

/**
 * Gender distinction for Bazi calculation (乾造 / 坤造).
 */
enum class BaziGender(
    val code: String,
    val displayNameZh: String,
    val titleZh: String,
    val titleEn: String,
) {
    MALE("male", "男", "乾造", "Male"),
    FEMALE("female", "女", "坤造", "Female");

    companion object {
        fun fromCode(code: String?): BaziGender =
            entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: MALE
    }
}

/**
 * A single pillar of four pillars (年/月/日/时柱).
 */
data class BaziPillar(
    val ganzhi: Ganzhi,
    val naYin: String,
    val hiddenStems: List<HeavenlyStem>,
    val stemShiShen: String,
    val branchShiShen: List<String>,
    val diShi: String,
    val xun: String,
    val xunKong: String,
)

/**
 * A single 10-year step in DaYun (大运).
 */
data class DaYunPillar(
    val index: Int,
    val startAge: Int,
    val endAge: Int,
    val startYear: Int,
    val endYear: Int,
    val ganzhi: Ganzhi,
    val stemShiShen: String,
)

/**
 * Distribution of Five Elements across the BaZi chart (stems and branches).
 */
data class WuXingDistribution(
    val metalCount: Int, // 金
    val woodCount: Int,  // 木
    val waterCount: Int, // 水
    val fireCount: Int,  // 火
    val earthCount: Int, // 土
) {
    val totalCount: Int get() = metalCount + woodCount + waterCount + fireCount + earthCount

    fun countOf(element: FiveElement): Int = when (element) {
        FiveElement.METAL -> metalCount
        FiveElement.WOOD -> woodCount
        FiveElement.WATER -> waterCount
        FiveElement.FIRE -> fireCount
        FiveElement.EARTH -> earthCount
    }

    val dominantElement: FiveElement
        get() {
            val list = listOf(
                FiveElement.WOOD to woodCount,
                FiveElement.FIRE to fireCount,
                FiveElement.EARTH to earthCount,
                FiveElement.METAL to metalCount,
                FiveElement.WATER to waterCount,
            )
            return list.maxByOrNull { it.second }?.first ?: FiveElement.WOOD
        }
}

/**
 * Complete immutable Bazi profile derived from birth date, hour and gender.
 */
data class BaziProfile(
    val gender: BaziGender,
    val birthDate: LocalDate,
    val birthHour: Int?,
    val yearPillar: BaziPillar,
    val monthPillar: BaziPillar,
    val dayPillar: BaziPillar,
    val hourPillar: BaziPillar?,
    val dayMaster: HeavenlyStem,
    val dayMasterElement: FiveElement,
    val shengXiao: String,
    val taiYuan: String,
    val mingGong: String,
    val dayXunKong: List<EarthlyBranch>,
    val yearXunKong: List<EarthlyBranch>,
    val daYunList: List<DaYunPillar> = emptyList(),
    val wuXingDistribution: WuXingDistribution = WuXingDistribution(0, 0, 0, 0, 0),
) {
    /**
     * Concise summary text, e.g. "乾造 · 丙火日主 · 庚午年".
     */
    val shortSummaryZh: String
        get() = "${gender.titleZh} · ${dayMaster.displayName}${dayMasterElement.displayName}日主 · ${yearPillar.ganzhi.displayName}年"

    /**
     * Concise four-pillars summary, e.g. "庚午 戊子 丙寅 己丑".
     */
    val fourPillarsText: String
        get() = buildString {
            append(yearPillar.ganzhi.displayName)
            append(" ")
            append(monthPillar.ganzhi.displayName)
            append(" ")
            append(dayPillar.ganzhi.displayName)
            if (hourPillar != null) {
                append(" ")
                append(hourPillar.ganzhi.displayName)
            }
        }
}
