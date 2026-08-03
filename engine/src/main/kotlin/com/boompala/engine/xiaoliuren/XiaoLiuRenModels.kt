package com.boompala.engine.xiaoliuren

import com.boompala.engine.model.DivinationTimeInfo

enum class XiaoLiuRenPalace(val displayName: String, val meaning: String, val auspicious: Boolean) {
    DA_AN("大安", "稳定、守成、平安；宜静守与按计划推进。", true),
    LIU_LIAN("留连", "事情容易拖延或反复，宜耐心核实。", false),
    SU_XI("速喜", "进展较快、消息来得急；仍需以事实为准。", true),
    CHI_KOU("赤口", "口舌、冲突或阻碍，宜谨慎沟通。", false),
    XIAO_JI("小吉", "有小幅助力与顺势机会，宜稳步行动。", true),
    KONG_WANG("空亡", "信息不足、落空或暂未成形，宜等待确认。", false),
}

data class XiaoLiuRenReading(
    val timeInfo: DivinationTimeInfo,
    val monthPalace: XiaoLiuRenPalace,
    val dayPalace: XiaoLiuRenPalace,
    val hourPalace: XiaoLiuRenPalace,
) {
    val finalPalace: XiaoLiuRenPalace get() = hourPalace
    val palaceCycle: List<XiaoLiuRenPalace> get() = XiaoLiuRenPalace.entries
}
