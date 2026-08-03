package com.boompala.engine.model

import java.time.Instant
import java.time.ZoneId

/**
 * The four outcomes produced by one manual six-yao toss.
 *
 * Their numeric values follow the conventional 6/7/8/9 notation:
 * 6 = old yin, 7 = young yang, 8 = young yin, 9 = old yang.
 * The calculation rules that consume these values are intentionally kept
 * outside this model until the source comparison is complete.
 */
enum class YaoState(
    val numericValue: Int,
    val isYang: Boolean,
    val isChanging: Boolean,
    val displayName: String,
) {
    YOUNG_YANG(7, isYang = true, isChanging = false, displayName = "少阳"),
    YOUNG_YIN(8, isYang = false, isChanging = false, displayName = "少阴"),
    OLD_YANG(9, isYang = true, isChanging = true, displayName = "老阳"),
    OLD_YIN(6, isYang = false, isChanging = true, displayName = "老阴"),

    ;

    companion object {
        fun fromNumericValue(value: Int): YaoState =
            entries.firstOrNull { it.numericValue == value }
                ?: throw IllegalArgumentException("Unsupported six-yao value: $value")
    }
}

enum class YaoPolarity(
    val isYang: Boolean,
    val displayName: String,
) {
    YANG(isYang = true, displayName = "阳"),
    YIN(isYang = false, displayName = "阴"),
}

enum class FiveElement(
    val displayName: String,
) {
    WOOD("木"),
    FIRE("火"),
    EARTH("土"),
    METAL("金"),
    WATER("水"),
}

enum class HeavenlyStem(
    val index: Int,
    val displayName: String,
    val element: FiveElement,
) {
    JIA(0, "甲", FiveElement.WOOD),
    YI(1, "乙", FiveElement.WOOD),
    BING(2, "丙", FiveElement.FIRE),
    DING(3, "丁", FiveElement.FIRE),
    WU(4, "戊", FiveElement.EARTH),
    JI(5, "己", FiveElement.EARTH),
    GENG(6, "庚", FiveElement.METAL),
    XIN(7, "辛", FiveElement.METAL),
    REN(8, "壬", FiveElement.WATER),
    GUI(9, "癸", FiveElement.WATER),
}

enum class EarthlyBranch(
    val index: Int,
    val displayName: String,
    val element: FiveElement,
) {
    ZI(0, "子", FiveElement.WATER),
    CHOU(1, "丑", FiveElement.EARTH),
    YIN(2, "寅", FiveElement.WOOD),
    MAO(3, "卯", FiveElement.WOOD),
    CHEN(4, "辰", FiveElement.EARTH),
    SI(5, "巳", FiveElement.FIRE),
    WU(6, "午", FiveElement.FIRE),
    WEI(7, "未", FiveElement.EARTH),
    SHEN(8, "申", FiveElement.METAL),
    YOU(9, "酉", FiveElement.METAL),
    XU(10, "戌", FiveElement.EARTH),
    HAI(11, "亥", FiveElement.WATER),
}

data class Ganzhi(
    val heavenlyStem: HeavenlyStem,
    val earthlyBranch: EarthlyBranch,
) {
    init {
        require(heavenlyStem.index % 2 == earthlyBranch.index % 2) {
            "Heavenly stem and earthly branch must form a valid sexagenary pair."
        }
    }

    val displayName: String
        get() = heavenlyStem.displayName + earthlyBranch.displayName
}

enum class SixRelation(
    val displayName: String,
) {
    PARENTS("父母"),
    SIBLINGS("兄弟"),
    OFFSPRING("子孙"),
    WEALTH("妻财"),
    OFFICER_GHOST("官鬼"),
}

enum class SixSpirit(
    val displayName: String,
) {
    AZURE_DRAGON("青龙"),
    VERMILION_BIRD("朱雀"),
    HOOKED_WORM("勾陈"),
    SERPENT("螣蛇"),
    WHITE_TIGER("白虎"),
    BLACK_TORTOISE("玄武"),
}

enum class Palace(
    val displayName: String,
    val element: FiveElement,
) {
    QIAN("乾", FiveElement.METAL),
    DUI("兑", FiveElement.METAL),
    LI("离", FiveElement.FIRE),
    ZHEN("震", FiveElement.WOOD),
    XUN("巽", FiveElement.WOOD),
    KAN("坎", FiveElement.WATER),
    GEN("艮", FiveElement.EARTH),
    KUN("坤", FiveElement.EARTH),
}

enum class PalaceStage(
    val displayName: String,
) {
    PURE("八纯卦"),
    FIRST("一世卦"),
    SECOND("二世卦"),
    THIRD("三世卦"),
    FOURTH("四世卦"),
    FIFTH("五世卦"),
    WANDERING_SOUL("游魂卦"),
    RETURNING_SOUL("归魂卦"),
}

/**
 * A stable position from the bottom line (初爻) to the top line (上爻).
 */
enum class YaoPosition(
    val indexFromBottom: Int,
    val displayName: String,
) {
    FIRST(0, "初爻"),
    SECOND(1, "二爻"),
    THIRD(2, "三爻"),
    FOURTH(3, "四爻"),
    FIFTH(4, "五爻"),
    TOP(5, "上爻"),
}

data class YaoLineInput(
    val position: YaoPosition,
    val state: YaoState,
)

/**
 * User-provided input for one casting.
 *
 * `linesFromBottom` is deliberately explicit so callers cannot accidentally
 * reverse the traditional 初爻 -> 上爻 input order.
 */
data class HexagramInput(
    val linesFromBottom: List<YaoLineInput>,
    val castAt: Instant,
    val zoneId: ZoneId,
) {
    init {
        require(linesFromBottom.size == YaoPosition.entries.size) {
            "A six-yao casting must contain exactly six lines."
        }
        require(linesFromBottom.map { it.position } == YaoPosition.entries) {
            "Lines must be ordered from 初爻 to 上爻."
        }
    }
}

/**
 * The six line polarities, always stored from 初爻 to 上爻.
 */
data class HexagramPattern(
    val linesFromBottom: List<YaoPolarity>,
) {
    init {
        require(linesFromBottom.size == YaoPosition.entries.size) {
            "A hexagram pattern must contain exactly six lines."
        }
    }

    val codeFromBottom: String
        get() = linesFromBottom.joinToString(separator = "") { polarity ->
            if (polarity == YaoPolarity.YANG) "1" else "0"
        }
}

data class Hexagram(
    val pattern: HexagramPattern,
    val name: String,
    val palace: Palace,
    val element: FiveElement,
    val palaceStage: PalaceStage,
    val yaoFromBottom: List<Yao>,
) {
    init {
        require(yaoFromBottom.map { it.position } == YaoPosition.entries) {
            "Hexagram lines must be ordered from 初爻 to 上爻."
        }
        require(yaoFromBottom.map { it.yinYang } == pattern.linesFromBottom) {
            "Hexagram line polarities must match its pattern."
        }
    }

    val shiPosition: YaoPosition
        get() = yaoFromBottom.single { it.isShi }.position

    val yingPosition: YaoPosition
        get() = yaoFromBottom.single { it.isYing }.position
}

data class Yao(
    val position: YaoPosition,
    val yinYang: YaoPolarity,
    val moving: Boolean,
    val heavenlyStem: HeavenlyStem,
    val earthlyBranch: EarthlyBranch,
    val element: FiveElement,
    val sixRelation: SixRelation,
    val sixSpirit: SixSpirit,
    val isShi: Boolean,
    val isYing: Boolean,
    val isVoid: Boolean,
    val lineText: String? = null,
)

/**
 * Complete engine output. Its fields are intentionally independent of UI
 * presentation so history/favorites can persist the same contract later.
 */
data class DivinationResult(
    val timeInfo: DivinationTimeInfo,
    val voidBranches: List<EarthlyBranch>,
    val original: Hexagram,
    val changed: Hexagram?,
) {
    init {
        require(voidBranches.size == 2) {
            "A sexagenary day has exactly two void branches."
        }
    }

    val yaoFromBottom: List<Yao>
        get() = original.yaoFromBottom

    val castAt: Instant
        get() = timeInfo.gregorianDateTime.toInstant()

    val zoneId: ZoneId
        get() = timeInfo.gregorianDateTime.zone

    val yearGanzhi: Ganzhi
        get() = timeInfo.yearGanzhi

    val monthGanzhi: Ganzhi
        get() = timeInfo.monthGanzhi

    val dayGanzhi: Ganzhi
        get() = timeInfo.dayGanzhi

    val hourGanzhi: Ganzhi
        get() = timeInfo.hourGanzhi

    val changingPositions: List<YaoPosition>
        get() = yaoFromBottom.filter { it.moving }.map { it.position }

    val movingLineTexts: List<String>
        get() = yaoFromBottom.mapNotNull { yao ->
            if (yao.moving) yao.lineText else null
        }
}
