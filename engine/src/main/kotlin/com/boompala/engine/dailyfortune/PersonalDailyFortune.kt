package com.boompala.engine.dailyfortune

import com.boompala.engine.model.FiveElement
import com.boompala.engine.model.HeavenlyStem

/**
 * 流日神煞与干支合冲互动事件。
 *
 * @property title 简练标题，如 "天乙贵人值日"、"天干相合 · 乙庚逢合"、"日支六冲 · 寅申相冲"。
 * @property description 详细解释与行事指引。
 * @property isAuspicious 是否为吉庆/加持向事件（false 为需防范/沉着应对事件）。
 */
data class FortuneEvent(
    val title: String,
    val description: String,
    val isAuspicious: Boolean = true,
)

/**
 * 个人八字与流日相结合的完整专属运势结果。
 *
 * 纯函数计算生成，完全不依赖任何外部状态与网络，严格可复现。
 */
data class PersonalDailyFortune(
    val dayMaster: HeavenlyStem,
    val dayMasterElement: FiveElement,
    val shiShenName: String,
    val themeTitle: String,
    val themeAdvice: String,
    val events: List<FortuneEvent>,
    val balanceColor: FortuneColor,
    val balanceNumbers: List<Int>,
    val hexagramResonance: String,
)
