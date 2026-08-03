package com.boompala.compass

import java.time.LocalDate

data class NineStar(
    val number: Int,
    val colorName: String,
    val starName: String,
    val element: String,
    val trigram: String,
    val neutralMeaning: String,
) {
    val displayName: String get() = "$number$colorName$starName"
}

data class YuanYunPeriod(
    val number: Int,
    val yuan: String,
    val startYear: Int,
    val endYear: Int,
    val rulingStar: NineStar,
) {
    val displayName: String get() = "$yuan · ${number}运"
}

object YuanYunData {
    val stars = listOf(
        NineStar(1, "白", "贪狼", "水", "坎", "人缘、流动与思考"),
        NineStar(2, "黑", "巨门", "土", "坤", "承载、照护与健康课题"),
        NineStar(3, "碧", "禄存", "木", "震", "行动、竞争与是非课题"),
        NineStar(4, "绿", "文曲", "木", "巽", "学习、传播与文艺"),
        NineStar(5, "黄", "廉贞", "土", "中", "中央、权变与失衡风险"),
        NineStar(6, "白", "武曲", "金", "乾", "秩序、权责与执行"),
        NineStar(7, "赤", "破军", "金", "兑", "交际、表达与破损风险"),
        NineStar(8, "白", "左辅", "土", "艮", "积累、地产与稳定"),
        NineStar(9, "紫", "右弼", "火", "离", "显达、文化与传播"),
    )

    fun periodFor(date: LocalDate): YuanYunPeriod {
        // This feature adopts the common Li Chun boundary, represented at date precision as February 4.
        val effectiveYear = if (date < LocalDate.of(date.year, 2, 4)) date.year - 1 else date.year
        val cycleYear = Math.floorMod(effectiveYear - 1864, 180)
        val number = cycleYear / 20 + 1
        val cycleStart = effectiveYear - cycleYear
        val startYear = cycleStart + (number - 1) * 20
        val yuan = when (number) { in 1..3 -> "上元"; in 4..6 -> "中元"; else -> "下元" }
        return YuanYunPeriod(number, yuan, startYear, startYear + 19, stars[number - 1])
    }

    fun status(starNumber: Int, periodNumber: Int): String = when (Math.floorMod(starNumber - periodNumber, 9)) {
        0 -> "旺气"
        1, 2 -> "生气"
        8 -> "退气"
        3, 4 -> "死气"
        else -> "煞气"
    }

    fun periodSummary(period: YuanYunPeriod): String {
        val next1 = stars[period.number % 9]
        val next2 = stars[(period.number + 1) % 9]
        val previous = stars[(period.number + 7) % 9]
        return "${period.rulingStar.displayName}当运；${next1.number}${next1.colorName}、${next2.number}${next2.colorName}为生气，${previous.number}${previous.colorName}为退气"
    }
}
