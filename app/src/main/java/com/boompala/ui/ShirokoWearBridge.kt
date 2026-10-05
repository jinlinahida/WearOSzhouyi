package com.boompala.ui

import androidx.compose.runtime.Composable
import com.boompala.settings.AppSettings
import com.boompala.settings.ContentSize
import com.boompala.settings.HapticIntensity
import com.boompala.settings.ScreenShape
import io.github.jinlinahida.shirokowear.ui.ShirokoWearContentScale
import io.github.jinlinahida.shirokowear.ui.ShirokoWearHapticIntensity
import io.github.jinlinahida.shirokowear.ui.ShirokoWearScreenShape
import io.github.jinlinahida.shirokowear.ui.ShirokoWearTheme

/**
 * boompala 设置 → ShirokoWear UI 令牌的映射。
 *
 * 这一层必须存在：库组件从 CompositionLocal 读动画/表冠/触觉开关与字号档位。
 * 若不把本 App 的设置灌进去，库会退回默认值——用户明明关了震动却仍然震，
 * 属于静默行为分歧，比任何编译错误都难查。
 */
@Composable
internal fun ShirokoWearBridge(
    settings: AppSettings,
    screenShape: ScreenShape,
    content: @Composable () -> Unit,
) {
    ShirokoWearTheme(
        contentScale = settings.contentSize.toShirokoContentScale(),
        screenShape = screenShape.toShirokoScreenShape(),
        animationsEnabled = settings.animationsEnabled,
        rotaryScrollingEnabled = settings.rotaryScrollingEnabled,
        hapticFeedbackEnabled = settings.hapticFeedbackEnabled,
        hapticIntensity = settings.hapticIntensity.toShirokoHapticIntensity(),
        content = content,
    )
}

internal fun ContentSize.toShirokoContentScale(): ShirokoWearContentScale = when (this) {
    ContentSize.SMALL -> ShirokoWearContentScale.SMALL
    ContentSize.STANDARD -> ShirokoWearContentScale.STANDARD
    ContentSize.LARGE -> ShirokoWearContentScale.LARGE
}

internal fun ScreenShape.toShirokoScreenShape(): ShirokoWearScreenShape = when (this) {
    ScreenShape.ROUND -> ShirokoWearScreenShape.ROUND
    ScreenShape.SQUARE -> ShirokoWearScreenShape.SQUARE
}

internal fun HapticIntensity.toShirokoHapticIntensity(): ShirokoWearHapticIntensity = when (this) {
    HapticIntensity.LIGHT -> ShirokoWearHapticIntensity.LIGHT
    HapticIntensity.STANDARD -> ShirokoWearHapticIntensity.STANDARD
    HapticIntensity.STRONG -> ShirokoWearHapticIntensity.STRONG
}
