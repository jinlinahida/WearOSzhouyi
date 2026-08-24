package com.boompala.engine.tarot

/**
 * Core domain models for Tarot divination.
 */
enum class ArcanaType(val displayName: String, val englishName: String) {
    MAJOR("大阿卡纳", "Major Arcana"),
    MINOR("小阿卡纳", "Minor Arcana"),
}

enum class TarotSuit(
    val displayName: String,
    val englishName: String,
    val element: TarotElement,
) {
    MAJOR("大牌", "Major", TarotElement.NONE),
    WANDS("权杖", "Wands", TarotElement.FIRE),
    CUPS("圣杯", "Cups", TarotElement.WATER),
    SWORDS("宝剑", "Swords", TarotElement.AIR),
    PENTACLES("星币", "Pentacles", TarotElement.EARTH),
}

enum class TarotElement(val displayName: String, val englishName: String) {
    FIRE("火", "Fire"),
    WATER("水", "Water"),
    AIR("风", "Air"),
    EARTH("土", "Earth"),
    NONE("无", "None"),
}

enum class TarotOrientation(val displayName: String, val englishName: String) {
    UPRIGHT("正位", "Upright"),
    REVERSED("逆位", "Reversed"),
}

enum class DeckType(val displayName: String, val cardCount: Int) {
    FULL_78("完整牌组 (78张)", 78),
    MAJOR_22("仅大阿卡纳 (22张)", 22),
    MINOR_56("仅小阿卡纳 (56张)", 56),
}

/**
 * Immutable definition of a single Tarot card.
 */
data class TarotCard(
    val id: Int,
    val code: String,
    val nameEn: String,
    val nameZh: String,
    val arcana: ArcanaType,
    val suit: TarotSuit,
    val rank: Int,
    val rankName: String,
    val element: TarotElement,
    val keywordsEn: List<String>,
    val keywordsZh: List<String>,
    val uprightMeanings: List<String>,
    val reversedMeanings: List<String>,
    val fortuneTelling: List<String>,
    val uprightMeaningsZh: List<String> = uprightMeanings,
    val reversedMeaningsZh: List<String> = reversedMeanings,
    val fortuneTellingZh: List<String> = fortuneTelling,
    val uprightMeaningsEn: List<String> = emptyList(),
    val reversedMeaningsEn: List<String> = emptyList(),
    val fortuneTellingEn: List<String> = emptyList(),
)

/**
 * A slot/position inside a Tarot spread.
 */
data class TarotSlot(
    val index: Int,
    val name: String,
    val description: String,
)

/**
 * A predefined Tarot spread layout.
 */
data class TarotSpread(
    val id: String,
    val name: String,
    val description: String,
    val cardCount: Int,
    val slots: List<TarotSlot>,
) {
    init {
        require(cardCount == slots.size) {
            "Spread $id specifies $cardCount cards but has ${slots.size} slots."
        }
    }

    companion object {
        val ONE_CARD = TarotSpread(
            id = "one_card",
            name = "单张牌指引",
            description = "适用于今日运势、单点困惑、是非判断或当下核心能量洞察。",
            cardCount = 1,
            slots = listOf(
                TarotSlot(0, "当下启示", "反映目前事件的核心状态、当下的能量与关键建议。"),
            ),
        )

        val TIME_FLOW = TarotSpread(
            id = "time_flow",
            name = "时间流牌阵",
            description = "经典三牌阵，梳理事件在时间维度上的脉络与发展趋势。",
            cardCount = 3,
            slots = listOf(
                TarotSlot(0, "过去", "导致当前状况的起因、过去的经验或潜藏根源。"),
                TarotSlot(1, "现在", "当前的实际处境、正在面对的挑战与当下能量。"),
                TarotSlot(2, "未来", "若顺应当前趋势发展，最可能出现的结果与走向。"),
            ),
        )

        val HOLY_TRIANGLE = TarotSpread(
            id = "holy_triangle",
            name = "圣三角牌阵",
            description = "深入剖析具体问题的因果逻辑，寻找解决阻碍的关键对策。",
            cardCount = 3,
            slots = listOf(
                TarotSlot(0, "现状", "问题的当前实际表现与显性状态。"),
                TarotSlot(1, "阻碍与根源", "造成困扰的核心阻碍、隐性原因或难点所在。"),
                TarotSlot(2, "对策与建议", "突破困局的有效建议与行动指引。"),
            ),
        )

        val FOUR_ELEMENTS = TarotSpread(
            id = "four_elements",
            name = "四要素牌阵",
            description = "从行动、情感、思维与物质四个维度全方位审视问题。",
            cardCount = 4,
            slots = listOf(
                TarotSlot(0, "火 / 行动意志", "行动力、热情、动机与推进节奏。"),
                TarotSlot(1, "水 / 情感内心", "内心感受、情绪状态、人际关系与直觉。"),
                TarotSlot(2, "风 / 思维沟通", "逻辑分析、沟通交流、想法与理性判断。"),
                TarotSlot(3, "土 / 现实物质", "资源基础、财务状况、健康与落地结果。"),
            ),
        )

        val HEXAGRAM = TarotSpread(
            id = "hexagram",
            name = "六芒星牌阵",
            description = "经典七牌综合分析阵，兼顾内外环境、主客观意愿与发展走势。",
            cardCount = 7,
            slots = listOf(
                TarotSlot(0, "过去起因", "事情发生的最初原因与背景。"),
                TarotSlot(1, "现在处境", "当下的实际状态。"),
                TarotSlot(2, "未来趋势", "未来的自然演化方向。"),
                TarotSlot(3, "辅助对策", "可运用的支持力量或解决方法。"),
                TarotSlot(4, "周围环境", "外界环境、相关人员的态度与外部影响。"),
                TarotSlot(5, "内心愿望", "当事人内心的期望或隐忧。"),
                TarotSlot(6, "最终结果", "综合各方因素后的最终推演结果。"),
            ),
        )

        val CELTIC_CROSS = TarotSpread(
            id = "celtic_cross",
            name = "凯尔特十字",
            description = "西方塔罗最经典严谨的大型十牌牌阵，全景透视复杂事态与深层潜意识。",
            cardCount = 10,
            slots = listOf(
                TarotSlot(0, "核心现状", "问卜者目前的处境与核心议题。"),
                TarotSlot(1, "阻碍与助力", "与核心现状产生交互的阻碍或促进力量。"),
                TarotSlot(2, "潜意识根源", "深层动机、潜意识或事件深层的基石。"),
                TarotSlot(3, "过去影响", "刚刚过去且仍在发挥作用的事件。"),
                TarotSlot(4, "显意识目标", "最佳可能目标、理性思考与显性追求。"),
                TarotSlot(5, "近期未来", "短期内即将发生或显现的情况。"),
                TarotSlot(6, "自身态度", "当事人的心态、定位与自我认知。"),
                TarotSlot(7, "外界环境", "家庭、社会或他人对事态的影响与态度。"),
                TarotSlot(8, "希望与恐惧", "当事人内心深处的期盼或担忧。"),
                TarotSlot(9, "最终结果", "若持续当前轨迹的最终长远结论。"),
            ),
        )

        val ALL_SPREADS: List<TarotSpread> = listOf(
            ONE_CARD,
            TIME_FLOW,
            HOLY_TRIANGLE,
            FOUR_ELEMENTS,
            HEXAGRAM,
            CELTIC_CROSS,
        )

        fun findById(id: String): TarotSpread? = ALL_SPREADS.find { it.id == id }
    }
}

/**
 * A single card drawn into a specific slot with an orientation.
 */
data class DrawnTarotCard(
    val slot: TarotSlot,
    val card: TarotCard,
    val orientation: TarotOrientation,
)

/**
 * Complete result of a Tarot reading session.
 */
data class TarotReading(
    val spread: TarotSpread,
    val deckType: DeckType,
    val drawnCards: List<DrawnTarotCard>,
    val castAt: Long,
)
