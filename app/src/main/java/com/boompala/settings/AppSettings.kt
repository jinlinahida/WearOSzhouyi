package com.boompala.settings

enum class ScreenMode(
    val displayName: String,
) {
    AUTO("自动检测"),
    ROUND("圆屏"),
    SQUARE("方屏"),
}

enum class ContentSize(
    val displayName: String,
    val fontScale: Float,
) {
    SMALL("小", 0.90f),
    STANDARD("标准", 1.0f),
    LARGE("大", 1.10f),
}

enum class HapticIntensity(
    val displayName: String,
) {
    LIGHT("弱"),
    STANDARD("标准"),
    STRONG("强劲"),
}

enum class AppLanguage(
    val code: String,
    val displayName: String,
    val englishName: String,
) {
    CHINESE("zh", "简体中文", "Simplified Chinese"),
    ENGLISH("en", "English", "English"),
}

enum class HomeFeature(
    val id: String,
    val defaultTitleZh: String,
    val defaultTitleEn: String,
) {
    SIX_YAO("six_yao", "六爻排盘", "Six Yao"),
    MEI_HUA("mei_hua", "时间起卦", "Mei Hua Time"),
    TAROT_ONE("tarot_one", "单牌塔罗", "Tarot Single Card"),
    TAROT_THREE("tarot_three", "时间流三牌", "Tarot Time Flow"),
    TAROT_HOLY_TRIANGLE("tarot_holy_triangle", "圣三角牌阵", "Holy Triangle"),
    DAILY_FORTUNE("daily_fortune", "今日运势", "Daily Fortune"),
    XIAO_LIU_REN("xiao_liu_ren", "小六壬", "Xiao Liu Ren"),
    COMPASS("compass", "罗盘", "Compass"),
    ARCHIVES("archives", "归档", "Archives"),
    BROWSE("browse", "浏览", "Browse");

    companion object {
        val DEFAULT_ORDER = listOf(
            SIX_YAO,
            MEI_HUA,
            TAROT_ONE,
            TAROT_THREE,
            TAROT_HOLY_TRIANGLE,
            DAILY_FORTUNE,
            XIAO_LIU_REN,
            COMPASS,
            ARCHIVES,
            BROWSE,
        )

        fun fromId(id: String): HomeFeature? = entries.find { it.id == id }
    }
}

data class AppSettings(
    val screenMode: ScreenMode = ScreenMode.AUTO,
    val contentSize: ContentSize = ContentSize.STANDARD,
    val animationsEnabled: Boolean = true,
    val rotaryScrollingEnabled: Boolean = true,
    val hapticFeedbackEnabled: Boolean = true,
    val hapticIntensity: HapticIntensity = HapticIntensity.STANDARD,
    val language: AppLanguage = AppLanguage.CHINESE,
    val homeOrder: List<HomeFeature> = HomeFeature.DEFAULT_ORDER,
    val hiddenHomeFeatures: Set<HomeFeature> = emptySet(),
    val hasCompletedOnboarding: Boolean = false,
) {
    companion object {
        val DEFAULT = AppSettings()
    }

    fun resolvedScreenShape(isRoundDevice: Boolean): ScreenShape =
        when (screenMode) {
            ScreenMode.AUTO -> if (isRoundDevice) ScreenShape.ROUND else ScreenShape.SQUARE
            ScreenMode.ROUND -> ScreenShape.ROUND
            ScreenMode.SQUARE -> ScreenShape.SQUARE
        }

    fun effectiveHomeOrder(): List<HomeFeature> {
        val seen = mutableSetOf<HomeFeature>()
        val result = mutableListOf<HomeFeature>()
        for (item in homeOrder) {
            if (seen.add(item)) {
                result.add(item)
            }
        }
        for (item in HomeFeature.DEFAULT_ORDER) {
            if (seen.add(item)) {
                result.add(item)
            }
        }
        return result
    }

    fun visibleHomeFeatures(): List<HomeFeature> {
        return effectiveHomeOrder().filterNot { hiddenHomeFeatures.contains(it) }
    }
}

enum class ScreenShape {
    ROUND,
    SQUARE,
}
