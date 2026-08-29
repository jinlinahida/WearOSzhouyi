package com.boompala.engine.pulse

/**
 * 常见中医脉象大类（涵盖临床与古典最核心的 12 种典型脉象）。
 */
enum class PulseCategory(
    val chineseName: String,
    val natureSummary: String,
    val classicPhrase: String,
) {
    PING("平脉", "阳气冲和 · 脏腑安和之象", "和缓从容，节律平稳"),
    HUA("滑脉", "阳中之阴 · 气血流利或湿滞之象", "往来流利，如盘走珠"),
    XIAN("弦脉", "阳气偏刚 · 肝气郁结或张力偏高之象", "端直以长，如按琴弦"),
    CHI("迟脉", "阴寒凝聚 · 阳虚气弱或内寒之象", "脉率偏缓，一息三至"),
    SHU("数脉", "阳盛发热 · 阴虚内热或循环急促之象", "一息六至，脉流急促"),
    HUAN("缓脉", "中土之令 · 脾虚湿盛或从容有度之象", "从容和缓，怠而不急"),
    RU("濡脉", "阴阳双亏 · 气虚夹湿于表之象", "浮而细软，如絮在水"),
    XI("细脉", "阴精亏耗 · 气血两虚不充脉道之象", "细如发丝，指下分明"),
    CHEN("沉脉", "深潜入内 · 里寒水气或气机内收之象", "轻取不应，重按乃得"),
    FU("浮脉", "升越于表 · 外邪在表或虚阳浮越之象", "举之有余，按之稍减"),
    HONG("洪脉", "火热炽盛 · 来盛去衰汹涌澎湃之象", "来盛去衰，滔滔满指"),
    JIE_DAI("结代脉", "心气虚怯 · 气血不续脉律偶止之象", "律动歇止，止有定数"),
}

/**
 * 脉象测算输出的数据质控指标（置信度、覆盖率与有效性判定）。
 */
data class PulseDataQuality(
    val validBeatCount: Int = 0,
    val coveragePercent: Double = 100.0,
    val meanConfidence: Double = 3.0,
    val isReliable: Boolean = true,
    val failureReason: String? = null,
)

/**
 * 脉象测算输出的客观物理与波形特征指标。
 */
data class PulseFeatureMetrics(
    val heartRateBpm: Double,
    val regularityPercent: Double,
    val rmssdMs: Double = 32.0,
    val pnn50Percent: Double = 0.0,
    val h1: Double = 1.0,
    val h2: Double = 0.38,
    val h3: Double = 0.50,
    val kValue: Double = 0.35,
    val h3Ratio: Double = 0.50,
    val h2Ratio: Double = 0.38,
    val isRawPpg: Boolean = false,
    val quality: PulseDataQuality = PulseDataQuality(),
)

/**
 * 辨证兼夹体征与食疗推荐明细。
 */
data class SyndromeDetail(
    val title: String,
    val symptoms: String,
    val dietaryRecommendations: String,
)

/**
 * 中医脉象调摄与典籍指引（完全契合 8 层垂直结果卡片体系）。
 */
data class PulseRemedyProfile(
    val category: PulseCategory,
    val featureDescription: String,
    val waveformPoints: List<Float>,
    val dosList: List<String>,
    val dontsList: List<String>,
    val syndromes: List<SyndromeDetail>,
    val emotionalAdvice: String,
    val lifestyleAdvice: String,
    val exerciseAdvice: String,
    val classicLiterature: String,
    val theoreticalReason: String,
)

/**
 * 子午流注十二经络时辰气血流注信息。
 */
data class MeridianInfluence(
    val earthlyBranch: String,
    val timeRangeText: String,
    val meridianName: String,
    val organName: String,
    val physiologicalRole: String,
    val healthGuidance: String,
)

/**
 * 完整脉象推演综合诊断结果。
 */
data class PulseDiagnosisResult(
    val category: PulseCategory,
    val metrics: PulseFeatureMetrics,
    val profile: PulseRemedyProfile,
    val meridianInfo: MeridianInfluence,
    val timestampMillis: Long,
)
