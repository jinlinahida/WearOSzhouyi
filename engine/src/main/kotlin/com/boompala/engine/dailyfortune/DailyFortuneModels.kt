package com.boompala.engine.dailyfortune

import com.boompala.engine.model.EarthlyBranch
import com.boompala.engine.model.FiveElement
import com.boompala.engine.model.Ganzhi
import com.boompala.engine.model.YaoPosition
import java.time.LocalDate

/**
 * Eight compass directions aligned with the Later Heaven arrangement used by
 * the compass feature (`docs/compass-data-conventions.md`): 坎 north 0°, then
 * clockwise in 45° steps.
 */
enum class Direction(
    val trigramName: String,
    val directionText: String,
    val centerDegrees: Int,
) {
    KAN("坎", "北", 0),
    GEN("艮", "东北", 45),
    ZHEN("震", "东", 90),
    XUN("巽", "东南", 135),
    LI("离", "南", 180),
    KUN("坤", "西南", 225),
    DUI("兑", "西", 270),
    QIAN("乾", "西北", 315),
}

/**
 * The five ritual colors keyed by element, following the conventional
 * wood-green, fire-red, earth-yellow, metal-white, water-black mapping.
 */
enum class FortuneColor(
    val displayName: String,
    val element: FiveElement,
) {
    GREEN("青绿", FiveElement.WOOD),
    RED("红", FiveElement.FIRE),
    YELLOW("黄", FiveElement.EARTH),
    WHITE("白", FiveElement.METAL),
    BLACK("黑", FiveElement.WATER),
}

/** Deity whose daily direction the almanac reports. */
enum class FortuneDeity(
    val displayName: String,
) {
    XI_SHEN("喜神"),
    YANG_GUI("阳贵人"),
}

/** One deity direction for the day: the deity, its trigram sector, and the traditional wording. */
data class AuspiciousDirection(
    val deity: FortuneDeity,
    val direction: Direction,
    val description: String,
)

/**
 * The twelve day officers (十二建除). Declaration order is the cycle order:
 * the day whose earthly branch equals the solar-term month branch is 建.
 */
enum class JianChu(
    val displayName: String,
    val suitable: List<String>,
    val avoid: List<String>,
) {
    JIAN("建", suitable = listOf("上任", "出行", "谒贵"), avoid = listOf("大动土")),
    CHU("除", suitable = listOf("求医", "扫除", "除旧"), avoid = listOf("嫁娶")),
    MAN("满", suitable = listOf("祈福", "开业", "纳财"), avoid = listOf("动土")),
    PING("平", suitable = listOf("修饰", "小事"), avoid = emptyList()),
    DING("定", suitable = listOf("订盟", "纳采", "签约"), avoid = listOf("词讼")),
    ZHI("执", suitable = listOf("修造", "收纳"), avoid = listOf("移徙", "远行")),
    PO("破", suitable = listOf("破屋", "拆卸"), avoid = listOf("诸事不宜")),
    WEI("危", suitable = listOf("祭祀", "祈福"), avoid = listOf("登高", "乘船")),
    CHENG("成", suitable = listOf("开业", "嫁娶", "移徙"), avoid = listOf("词讼")),
    SHOU("收", suitable = listOf("纳财", "入库", "交易"), avoid = emptyList()),
    KAI("开", suitable = listOf("上任", "开业", "出行"), avoid = listOf("破土")),
    BI("闭", suitable = listOf("安葬", "收藏"), avoid = listOf("开市", "出行")),
}

/** A yellow-path (黄道) hour of the day's twelve shichen. */
data class LuckyHour(
    val branch: EarthlyBranch,
    val periodText: String,
    val deityName: String,
    val isYellowPath: Boolean,
)

/**
 * Complete daily fortune output for one Gregorian day in the device zone.
 *
 * Every field is a pure function of the local date; no randomness is involved.
 * Text fields may be null when the offline repositories are unavailable.
 */
data class DailyFortuneReading(
    val date: LocalDate,
    val lunarDateText: String,
    val dayGanzhi: Ganzhi,
    val dayStemElement: FiveElement,
    val dayHexagramCode: String,
    val dayHexagramName: String,
    val rotationIndex: Int,
    val hexagramSummary: String?,
    val hexagramAdvice: String?,
    val dayLinePosition: YaoPosition,
    val dayLineText: String?,
    val luckyColor: FortuneColor,
    val supportColor: FortuneColor,
    val avoidColor: FortuneColor,
    val luckyNumbers: List<Int>,
    val directions: List<AuspiciousDirection>,
    val hours: List<LuckyHour>,
    val jianChu: JianChu,
)
