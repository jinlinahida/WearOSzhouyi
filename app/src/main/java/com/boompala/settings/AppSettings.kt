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

data class AppSettings(
    val screenMode: ScreenMode = ScreenMode.AUTO,
    val contentSize: ContentSize = ContentSize.STANDARD,
    val animationsEnabled: Boolean = true,
    val rotaryScrollingEnabled: Boolean = true,
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
}

enum class ScreenShape {
    ROUND,
    SQUARE,
}
