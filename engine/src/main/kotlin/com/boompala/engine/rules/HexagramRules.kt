package com.boompala.engine.rules

import com.boompala.engine.model.HexagramPattern
import com.boompala.engine.model.Palace
import com.boompala.engine.model.PalaceStage
import com.boompala.engine.model.YaoPolarity
import com.boompala.engine.model.YaoPosition

internal data class HexagramClassification(
    val name: String,
    val palace: Palace,
    val palaceStage: PalaceStage,
    val shiPosition: YaoPosition,
    val yingPosition: YaoPosition,
)

/**
 * 八宫、寻世和认宫规则 follow bopo/najia at commit
 * 9cf119169d7eb8e48febc05274aebf3f7106d647.
 *
 * The implementation is rewritten in Kotlin and keeps the project's explicit
 * bottom-to-top bit order.
 */
internal object HexagramRules {
    fun classify(pattern: HexagramPattern): HexagramClassification {
        val code = pattern.codeFromBottom
        val lower = Trigram.fromBits(code.substring(0, 3))
        val upper = Trigram.fromBits(code.substring(3, 6))
        val stage = palaceStage(lower.bitsFromBottom, upper.bitsFromBottom)
        val shiNumber = shiLineNumber(lower.bitsFromBottom, upper.bitsFromBottom, stage)
        val yingNumber = if (shiNumber > 3) shiNumber - 3 else shiNumber + 3
        val palace = palace(lower, upper, stage, shiNumber)

        return HexagramClassification(
            name = HexagramCatalog.nameFor(code),
            palace = palace,
            palaceStage = stage,
            shiPosition = YaoPosition.entries[shiNumber - 1],
            yingPosition = YaoPosition.entries[yingNumber - 1],
        )
    }

    private fun palaceStage(
        lower: String,
        upper: String,
    ): PalaceStage {
        if (lower == upper) return PalaceStage.PURE

        val isWanderingSoul =
            upper[1] == lower[1] && upper[0] != lower[0] && upper[2] != lower[2]
        if (isWanderingSoul) return PalaceStage.WANDERING_SOUL

        val isReturningSoul =
            upper[1] != lower[1] && upper[0] == lower[0] && upper[2] == lower[2]
        if (isReturningSoul) return PalaceStage.RETURNING_SOUL

        return when (shiLineNumber(lower, upper, null)) {
            1 -> PalaceStage.FIRST
            2 -> PalaceStage.SECOND
            3 -> PalaceStage.THIRD
            4 -> PalaceStage.FOURTH
            5 -> PalaceStage.FIFTH
            else -> error("Only a pure hexagram can have the sixth line as 世爻.")
        }
    }

    private fun shiLineNumber(
        lower: String,
        upper: String,
        knownStage: PalaceStage?,
    ): Int {
        if (knownStage == PalaceStage.PURE) return 6
        if (knownStage == PalaceStage.WANDERING_SOUL) return 4
        if (knownStage == PalaceStage.RETURNING_SOUL) return 3

        // 天同二世，天变五世。
        if (upper[2] == lower[2]) {
            if (upper[1] != lower[1] && upper[0] != lower[0]) return 2
        } else if (upper[1] == lower[1] && upper[0] == lower[0]) {
            return 5
        }

        // 地同四世，地变一世。
        if (upper[0] == lower[0]) {
            if (upper[1] != lower[1] && upper[2] != lower[2]) return 4
        } else if (upper[1] == lower[1] && upper[2] == lower[2]) {
            return 1
        }

        return 3
    }

    private fun palace(
        lower: Trigram,
        upper: Trigram,
        stage: PalaceStage,
        shiLineNumber: Int,
    ): Palace = when {
        stage == PalaceStage.RETURNING_SOUL -> lower.palace
        shiLineNumber in setOf(1, 2, 3, 6) -> upper.palace
        else -> Trigram.fromBits(lower.bitsFromBottom.inverted()).palace
    }

    private fun String.inverted(): String =
        map { bit -> if (bit == '1') '0' else '1' }.joinToString("")
}

/**
 * Shared eight-trigram foundation. Lines are always stored from bottom to top,
 * so it can be consumed by both the six-yao and Mei Hua engines without a UI
 * projection or a second mapping table.
 */
enum class Trigram(
    val number: Int,
    val displayName: String,
    val bitsFromBottom: String,
    val palace: Palace,
) {
    QIAN(1, "乾", "111", Palace.QIAN),
    DUI(2, "兑", "110", Palace.DUI),
    LI(3, "离", "101", Palace.LI),
    ZHEN(4, "震", "100", Palace.ZHEN),
    XUN(5, "巽", "011", Palace.XUN),
    KAN(6, "坎", "010", Palace.KAN),
    GEN(7, "艮", "001", Palace.GEN),
    KUN(8, "坤", "000", Palace.KUN);

    val linesFromBottom: List<YaoPolarity>
        get() = bitsFromBottom.map { if (it == '1') YaoPolarity.YANG else YaoPolarity.YIN }

    companion object {
        fun fromBits(bitsFromBottom: String): Trigram =
            entries.firstOrNull { it.bitsFromBottom == bitsFromBottom }
                ?: error("Unknown trigram code: $bitsFromBottom")

        fun fromNumber(number: Int): Trigram =
            entries.firstOrNull { it.number == number }
                ?: error("Unknown trigram number: $number")
    }
}

/**
 * 六十四卦名称 table from bopo/najia's MIT-licensed const.py, rewritten as
 * immutable Kotlin data and kept separate from classification logic.
 */
/** Shared sixty-four-hexagram name lookup keyed by the bottom-to-top line code. */
object HexagramCatalog {
    private val namesByCode = mapOf(
        "111111" to "乾为天",
        "011111" to "天风姤",
        "001111" to "天山遁",
        "000111" to "天地否",
        "000011" to "风地观",
        "000001" to "山地剥",
        "000101" to "火地晋",
        "111101" to "火天大有",
        "110110" to "兑为泽",
        "010110" to "泽水困",
        "000110" to "泽地萃",
        "001110" to "泽山咸",
        "001010" to "水山蹇",
        "001000" to "地山谦",
        "001100" to "雷山小过",
        "110100" to "雷泽归妹",
        "101101" to "离为火",
        "001101" to "火山旅",
        "011101" to "火风鼎",
        "010101" to "火水未济",
        "010001" to "山水蒙",
        "010011" to "风水涣",
        "010111" to "天水讼",
        "101111" to "天火同人",
        "100100" to "震为雷",
        "000100" to "雷地豫",
        "010100" to "雷水解",
        "011100" to "雷风恒",
        "011000" to "地风升",
        "011010" to "水风井",
        "011110" to "泽风大过",
        "100110" to "泽雷随",
        "011011" to "巽为风",
        "111011" to "风天小畜",
        "101011" to "风火家人",
        "100011" to "风雷益",
        "100111" to "天雷无妄",
        "100101" to "火雷噬嗑",
        "100001" to "山雷颐",
        "011001" to "山风蛊",
        "010010" to "坎为水",
        "110010" to "水泽节",
        "100010" to "水雷屯",
        "101010" to "水火既济",
        "101110" to "泽火革",
        "101100" to "雷火丰",
        "101000" to "地火明夷",
        "010000" to "地水师",
        "001001" to "艮为山",
        "101001" to "山火贲",
        "111001" to "山天大畜",
        "110001" to "山泽损",
        "110101" to "火泽睽",
        "110111" to "天泽履",
        "110011" to "风泽中孚",
        "001011" to "风山渐",
        "000000" to "坤为地",
        "100000" to "地雷复",
        "110000" to "地泽临",
        "111000" to "地天泰",
        "111100" to "雷天大壮",
        "111110" to "泽天夬",
        "111010" to "水天需",
        "000010" to "水地比",
    )

    fun nameFor(codeFromBottom: String): String =
        namesByCode[codeFromBottom] ?: error("Unknown hexagram code: $codeFromBottom")

    /** Standard King Wen order, represented using the project's bottom-to-top bit contract. */
    val zhouOrderCodes: List<String> by lazy {
        listOf("乾为天", "坤为地", "水雷屯", "山水蒙", "水天需", "天水讼", "地水师", "水地比", "风天小畜", "天泽履", "地天泰", "天地否", "天火同人", "火天大有", "地山谦", "雷地豫", "泽雷随", "山风蛊", "地泽临", "风地观", "火雷噬嗑", "山火贲", "山地剥", "地雷复", "天雷无妄", "山天大畜", "山雷颐", "泽风大过", "坎为水", "离为火", "泽山咸", "雷风恒", "天山遁", "雷天大壮", "火地晋", "地火明夷", "风火家人", "火泽睽", "水山蹇", "雷水解", "山泽损", "风雷益", "泽天夬", "天风姤", "泽地萃", "地风升", "泽水困", "水风井", "泽火革", "火风鼎", "震为雷", "艮为山", "风山渐", "雷泽归妹", "雷火丰", "火山旅", "巽为风", "兑为泽", "风水涣", "水泽节", "风泽中孚", "雷山小过", "水火既济", "火水未济")
            .map { wanted -> namesByCode.entries.first { it.value == wanted }.key }
    }
}
