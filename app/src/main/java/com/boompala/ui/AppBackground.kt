package com.boompala.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

/**
 * 默认与特殊界面的顶部光照环境色规范。
 */
val DefaultSpotlightColor: Color = Color(0xFF64B5F6) // 默认浅蓝
val DailyFortuneSpotlightColor: Color = Color(0xFFFFB74D) // 今日运势流金
val TarotSpotlightColor: Color = Color(0xFF9575CD) // 塔罗神秘蓝紫
val CompassSpotlightColor: Color = Color(0xFF4DB6AC) // 罗盘青碧
val ResultSpotlightColor: Color = Color(0xFF80CBC4) // 排盘结果苍璧青玉
val PulseSpotlightColor: Color = Color(0xFF00E5A3) // 脉象推演翡翠青
val MuyuSpotlightColor: Color = Color(0xFFD4AF37) // 腕上木鱼温润檀金

/**
 * 根据当前界面计算目标环境光颜色。
 */
internal fun resolveSpotlightColor(screen: AppScreen): Color = when (screen) {
    AppScreen.DAILY_FORTUNE -> DailyFortuneSpotlightColor
    AppScreen.TAROT_ONE_CARD,
    AppScreen.TAROT_THREE_CARD,
    AppScreen.TAROT_HOLY_TRIANGLE,
    AppScreen.TAROT_CELTIC_CROSS,
    AppScreen.TAROT_BROWSER,
    AppScreen.TAROT_CARD_DETAIL -> TarotSpotlightColor
    AppScreen.COMPASS -> CompassSpotlightColor
    AppScreen.RESULT,
    AppScreen.MEIHUA_RESULT -> ResultSpotlightColor
    AppScreen.PULSE_MEASURE,
    AppScreen.PULSE_RESULT -> PulseSpotlightColor
    AppScreen.MUYU -> MuyuSpotlightColor
    else -> DefaultSpotlightColor
}

/**
 * 严格复刻自 Re-WearBili 的顶部环境光背景（Top Spotlight Ambient Background）。
 * - 纯黑底色保证 OLED 屏幕省电特性；
 * - 顶部居中半球状径向渐变，形成柔和自然的漫反射顶光；
 * - 支持呼吸光晕与页面色彩平滑过渡。
 */
@Composable
internal fun TopSpotlightBackground(
    screen: AppScreen,
    modifier: Modifier = Modifier,
    isGenerating: Boolean = false,
    animationsEnabled: Boolean = true,
    backgroundColor: Color = Color.Black,
    ambientAlpha: Float = 0.45f,
    content: @Composable BoxScope.() -> Unit,
) {
    val targetColor = resolveSpotlightColor(screen)
    val animatedColor by if (animationsEnabled) {
        animateColorAsState(
            targetValue = targetColor,
            animationSpec = tween(durationMillis = 600, easing = LinearOutSlowInEasing),
            label = "spotlightColorTransition",
        )
    } else {
        remember(targetColor) { mutableStateOf(targetColor) }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "spotlightBreathing")
    val breathingAlpha by if (animationsEnabled) {
        infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 0.35f,
            animationSpec = infiniteRepeatable(
                animation = tween(easing = EaseInOutCubic, durationMillis = 1200),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "spotlightBreathingAlpha",
        )
    } else {
        remember { mutableFloatStateOf(1f) }
    }

    val currentAlpha = if (isGenerating) ambientAlpha * breathingAlpha else ambientAlpha
    val localDensity = LocalDensity.current
    var circleHeight by remember { mutableStateOf(0.dp) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor),
    ) {
        // 顶部光晕半圆：几何定位严格对齐 Re-WearBili
        Box(
            modifier = Modifier
                .offset(y = circleHeight * -0.5f)
                .fillMaxWidth()
                .aspectRatio(1f)
                .alpha(currentAlpha)
                .background(
                    shape = CircleShape,
                    brush = Brush.radialGradient(
                        listOf(animatedColor, Color.Transparent),
                    ),
                )
                .onSizeChanged {
                    circleHeight = with(localDensity) { it.height.toDp() }
                },
        )
        content()
    }
}
