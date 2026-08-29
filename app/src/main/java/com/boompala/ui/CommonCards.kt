package com.boompala.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonColors
import androidx.wear.compose.material3.ButtonDefaults
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.basicMarquee
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.boompala.settings.HapticIntensity

/**
 * 手表端平滑走马灯修饰符：
 * - 当文本未超出容器可用宽度时：完全静止，零动画渲染开销；
 * - 当文本超出容器宽度时：平滑向左循环滚动；
 * - 页面进入或首尾循环时停留 1.2 秒，阅读体验从容稳定；
 * - 当动画开关关闭时，平滑降级为普通修饰符。
 */
fun Modifier.wearMarquee(
    animationsEnabled: Boolean = true,
    initialDelayMillis: Int = 1200,
    repeatDelayMillis: Int = 1200,
    iterations: Int = Int.MAX_VALUE,
): Modifier {
    return if (animationsEnabled) {
        this.basicMarquee(
            iterations = iterations,
            initialDelayMillis = initialDelayMillis,
            repeatDelayMillis = repeatDelayMillis,
        )
    } else {
        this
    }
}

/**
 * 直接复用自 Re-WearBili 项目的卡片设计规范常量。
 */
val CardBorderColor: Color = Color(54, 54, 54, 255)
val CardBorderWidth: Dp = 0.4f.dp
val CardBackgroundColor: Color = Color(38, 38, 38, 77)
val CardShape: Shape = RoundedCornerShape(10.dp)
val CardHighlightColor: Color = Color(231, 86, 136, 255)

/**
 * 带有按压缩放与触觉反馈的点击修饰符（直接对齐 Re-WearBili 的 clickVfx 实现）。
 * - 按下时缩小至 0.9f，松手 150ms 平滑回弹；
 * - 移除系统原生水波纹涟漪，提供清晰紧凑的手表按压质感；
 * - 联动 boompala 触觉震动体系。
 */
@Composable
fun Modifier.clickVfx(
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    isEnabled: Boolean = true,
    animationsEnabled: Boolean = true,
    hapticEnabled: Boolean = LocalHapticFeedbackEnabled.current,
    intensity: HapticIntensity = LocalHapticIntensity.current,
    onClick: () -> Unit,
): Modifier = composed {
    val context = LocalContext.current
    if (isEnabled) {
        if (!animationsEnabled) {
            clickable(
                indication = null,
                interactionSource = interactionSource,
                onClick = {
                    AppHaptics.click(context, intensity = intensity, enabled = hapticEnabled)
                    onClick()
                },
            )
        } else {
            val isPressed by interactionSource.collectIsPressedAsState()
            val sizePercent by animateFloatAsState(
                targetValue = if (isPressed) 0.9f else 1f,
                animationSpec = tween(durationMillis = 150),
                label = "cardClickVfxScale",
            )
            LaunchedEffect(interactionSource, hapticEnabled, intensity) {
                if (hapticEnabled) {
                    interactionSource.interactions.collect { interaction ->
                        if (interaction is PressInteraction.Press) {
                            AppHaptics.click(context, intensity = intensity, enabled = hapticEnabled)
                        }
                    }
                }
            }
            scale(sizePercent).clickable(
                indication = null,
                interactionSource = interactionSource,
                onClick = onClick,
            )
        }
    } else {
        Modifier
    }
}

/**
 * 支持单击与长按的 clickVfx 重载版本。
 */
@Composable
fun Modifier.clickVfx(
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    enabled: Boolean = true,
    animationsEnabled: Boolean = true,
    hapticEnabled: Boolean = LocalHapticFeedbackEnabled.current,
    intensity: HapticIntensity = LocalHapticIntensity.current,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {},
): Modifier = composed {
    val context = LocalContext.current
    if (enabled) {
        if (!animationsEnabled) {
            pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        AppHaptics.click(context, intensity = intensity, enabled = hapticEnabled)
                        onClick()
                    },
                    onLongPress = {
                        AppHaptics.click(context, intensity = HapticIntensity.STRONG, enabled = hapticEnabled)
                        onLongClick()
                    },
                )
            }
        } else {
            val isPressed by interactionSource.collectIsPressedAsState()
            val sizePercent by animateFloatAsState(
                targetValue = if (isPressed) 0.9f else 1f,
                animationSpec = tween(durationMillis = 150),
                label = "cardClickVfxScaleWithLongClick",
            )
            LaunchedEffect(interactionSource, hapticEnabled, intensity) {
                if (hapticEnabled) {
                    interactionSource.interactions.collect { interaction ->
                        if (interaction is PressInteraction.Press) {
                            AppHaptics.click(context, intensity = intensity, enabled = hapticEnabled)
                        }
                    }
                }
            }
            scale(sizePercent).pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onClick() },
                    onLongPress = { onLongClick() },
                    onPress = {
                        val press = PressInteraction.Press(it)
                        interactionSource.emit(press)
                        tryAwaitRelease()
                        interactionSource.emit(PressInteraction.Release(press))
                    },
                )
            }
        }
    } else {
        Modifier
    }
}

/**
 * 通用 Card 组件，完全对齐 Re-WearBili 的 Card 效果。
 * - 0.4dp 对角线微光渐变边框 (Brush.linearGradient, Offset.Zero -> Offset.Infinite)
 * - 30% alpha 半透明暗灰背景
 * - clickVfx 按压缩放微交互
 */
@Composable
fun Card(
    modifier: Modifier = Modifier,
    isClickEnabled: Boolean = true,
    shape: Shape = CardShape,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    isGradient: Boolean = true,
    borderColor: Color = CardBorderColor,
    borderWidth: Dp = CardBorderWidth,
    backgroundColor: Color = CardBackgroundColor,
    innerPaddingValues: PaddingValues = PaddingValues(
        start = 8.dp,
        end = 8.dp,
        top = 10.dp,
        bottom = 10.dp,
    ),
    outerPaddingValues: PaddingValues = PaddingValues(vertical = 4.dp),
    fillMaxSize: Boolean = true,
    contentAlignment: Alignment = Alignment.TopStart,
    content: @Composable BoxScope.() -> Unit,
) {
    val secondColor = if (isGradient) Color.Transparent else borderColor
    Box(
        modifier = modifier
            .padding(outerPaddingValues)
            .then(
                if (isClickEnabled && (onClick != null || onLongClick != null)) {
                    Modifier.clickVfx(
                        enabled = true,
                        onClick = { onClick?.invoke() },
                        onLongClick = { onLongClick?.invoke() },
                    )
                } else {
                    Modifier
                },
            )
            .clip(shape)
            .border(
                width = borderWidth,
                shape = shape,
                brush = Brush.linearGradient(
                    listOf(
                        borderColor,
                        secondColor,
                    ),
                    start = Offset.Zero,
                    end = Offset.Infinite,
                ),
            )
            .background(color = backgroundColor)
            .padding(innerPaddingValues)
            .then(if (fillMaxSize) Modifier.fillMaxWidth() else Modifier),
        contentAlignment = contentAlignment,
    ) {
        content()
    }
}

/**
 * 带有高亮切换动效的 Card 组件（对齐 Re-WearBili 的高亮状态重载）。
 */
@Composable
fun Card(
    modifier: Modifier = Modifier,
    isClickEnabled: Boolean = true,
    shape: Shape = CardShape,
    onClick: (() -> Unit)? = null,
    innerPaddingValues: PaddingValues = PaddingValues(
        start = 8.dp,
        end = 8.dp,
        top = 10.dp,
        bottom = 10.dp,
    ),
    isHighlighted: Boolean = false,
    highlightColor: Color = CardHighlightColor,
    fillMaxSize: Boolean = true,
    contentAlignment: Alignment = Alignment.TopStart,
    outerPaddingValues: PaddingValues = PaddingValues(vertical = 4.dp),
    content: @Composable BoxScope.() -> Unit,
) {
    val secondColor by animateColorAsState(
        targetValue = if (isHighlighted) highlightColor else Color.Transparent,
        label = "cardSecondColor",
    )
    val cardBorderColor by animateColorAsState(
        targetValue = if (isHighlighted) highlightColor else CardBorderColor,
        animationSpec = tween(),
        label = "cardBorderColor",
    )
    val cardBackgroundColor by animateColorAsState(
        targetValue = if (isHighlighted) highlightColor.copy(alpha = 0.1f) else CardBackgroundColor,
        animationSpec = tween(),
        label = "cardBackgroundColor",
    )
    val width by animateDpAsState(
        targetValue = if (isHighlighted) 2.dp else CardBorderWidth,
        label = "cardBorderWidth",
    )

    Box(
        modifier = modifier
            .padding(outerPaddingValues)
            .then(
                if (isClickEnabled && onClick != null) {
                    Modifier.clickVfx(isEnabled = true) { onClick() }
                } else {
                    Modifier
                },
            )
            .clip(shape)
            .border(
                width = width,
                shape = shape,
                brush = Brush.linearGradient(
                    listOf(
                        cardBorderColor,
                        secondColor,
                    ),
                    start = Offset.Zero,
                    end = Offset.Infinite,
                ),
            )
            .background(color = cardBackgroundColor)
            .padding(innerPaddingValues)
            .then(if (fillMaxSize) Modifier.fillMaxWidth() else Modifier),
        contentAlignment = contentAlignment,
    ) {
        content()
    }
}

/**
 * 升级版 ResultCard：直接复用 Re-WearBili 的对角微光渐变边框和半透明暗色背景，
 * 保持对已有 15 个业务界面的完全向后兼容，同时赋予通透轻盈的高级质感。
 */
@Composable
fun ResultCard(
    modifier: Modifier = Modifier,
    shape: Shape = CardShape,
    borderColor: Color = CardBorderColor,
    borderWidth: Dp = CardBorderWidth,
    backgroundColor: Color = CardBackgroundColor,
    isGradient: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    val metrics = LocalUiMetrics.current
    val secondColor = if (isGradient) Color.Transparent else borderColor
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .border(
                width = borderWidth,
                shape = shape,
                brush = Brush.linearGradient(
                    listOf(
                        borderColor,
                        secondColor,
                    ),
                    start = Offset.Zero,
                    end = Offset.Infinite,
                ),
            )
            .background(backgroundColor)
            .padding(
                horizontal = metrics.horizontalPadding / 2,
                vertical = metrics.cardVerticalPadding,
            ),
        verticalArrangement = Arrangement.spacedBy(metrics.itemSpacing / 2),
        content = content,
    )
}

@Composable
fun DetailField(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    marquee: Boolean = false,
    animationsEnabled: Boolean = true,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            maxLines = if (marquee) 1 else Int.MAX_VALUE,
            softWrap = !marquee,
            overflow = if (marquee) TextOverflow.Ellipsis else TextOverflow.Clip,
            modifier = if (marquee) Modifier.wearMarquee(animationsEnabled) else Modifier,
        )
    }
}

/**
 * Re-WearBili 风格按钮的默认属性配置。
 * - 0.4dp 对角线渐变微光高光边框 (Brush.linearGradient, Offset.Zero -> Offset.Infinite)
 * - 30% alpha 半透明深灰暗色底色，让背景漫反射环境光自然透出，不再死黑遮挡
 */
object BoompalaButtonDefaults {
    val borderStroke: BorderStroke
        get() = BorderStroke(
            width = CardBorderWidth,
            brush = Brush.linearGradient(
                colors = listOf(CardBorderColor, Color.Transparent),
                start = Offset.Zero,
                end = Offset.Infinite,
            ),
        )

    fun highlightedBorderStroke(highlightColor: Color = CardHighlightColor): BorderStroke =
        BorderStroke(
            width = 1.dp,
            brush = Brush.linearGradient(
                colors = listOf(highlightColor, Color.Transparent),
                start = Offset.Zero,
                end = Offset.Infinite,
            ),
        )

    @Composable
    fun buttonColors(
        containerColor: Color = CardBackgroundColor,
        contentColor: Color = Color.White,
        disabledContainerColor: Color = CardBackgroundColor.copy(alpha = 0.2f),
        disabledContentColor: Color = Color.White.copy(alpha = 0.38f),
    ): ButtonColors = ButtonDefaults.buttonColors(
        containerColor = containerColor,
        contentColor = contentColor,
        disabledContainerColor = disabledContainerColor,
        disabledContentColor = disabledContentColor,
    )

    @Composable
    fun outlinedButtonColors(
        containerColor: Color = Color(38, 38, 38, 38),
        contentColor: Color = Color.White,
        disabledContainerColor: Color = Color.Transparent,
        disabledContentColor: Color = Color.White.copy(alpha = 0.38f),
    ): ButtonColors = ButtonDefaults.buttonColors(
        containerColor = containerColor,
        contentColor = contentColor,
        disabledContainerColor = disabledContainerColor,
        disabledContentColor = disabledContentColor,
    )

    val compactContentPadding: PaddingValues = PaddingValues(
        horizontal = 8.dp,
        vertical = 4.dp,
    )
}

/**
 * 通用卡片按钮，完全复用 Re-WearBili 按钮视觉与交互效果：
 * - 边缘微光高光渐变细线
 * - 半透明暗色底色让背景环境光自然透出
 * - 支持 RowScope 水平居中排版与自定义修饰符
 */
@Composable
fun BoompalaCardButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.shape,
    colors: ButtonColors = BoompalaButtonDefaults.buttonColors(),
    border: BorderStroke? = BoompalaButtonDefaults.borderStroke,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable RowScope.() -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = shape,
        colors = colors,
        border = border,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
        content = content,
    )
}

/**
 * 具有选择与未选择状态的卡片胶囊按钮：
 * - 未选择态：0.4dp 对角线微光渐变边框，30% alpha 暗灰半透底色，次级白色文本；
 * - 已选择态：1.2dp 主题高亮色全环绕边框，注入主题发光半透光晕底色，高亮白色加粗文本；
 * - 切换状态时支持 200ms 色彩平滑流动过渡动画。
 */
@Composable
fun SelectableCardButton(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.shape,
    highlightColor: Color = Color(0xFF64B5F6),
    contentPadding: PaddingValues = BoompalaButtonDefaults.compactContentPadding,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable RowScope.() -> Unit,
) {
    val animatedBorderColor by animateColorAsState(
        targetValue = if (selected) highlightColor else CardBorderColor,
        animationSpec = tween(durationMillis = 200),
        label = "selectableBorderColor",
    )
    val animatedContainerColor by animateColorAsState(
        targetValue = if (selected) highlightColor.copy(alpha = 0.18f) else CardBackgroundColor,
        animationSpec = tween(durationMillis = 200),
        label = "selectableContainerColor",
    )
    val animatedBorderWidth by animateDpAsState(
        targetValue = if (selected) 1.2.dp else CardBorderWidth,
        animationSpec = tween(durationMillis = 200),
        label = "selectableBorderWidth",
    )

    val borderStroke = remember(selected, animatedBorderColor, animatedBorderWidth) {
        if (selected) {
            BorderStroke(width = animatedBorderWidth, color = animatedBorderColor)
        } else {
            BorderStroke(
                width = animatedBorderWidth,
                brush = Brush.linearGradient(
                    colors = listOf(animatedBorderColor, Color.Transparent),
                    start = Offset.Zero,
                    end = Offset.Infinite,
                ),
            )
        }
    }

    val buttonColors = ButtonDefaults.buttonColors(
        containerColor = animatedContainerColor,
        contentColor = Color.White,
        disabledContainerColor = animatedContainerColor.copy(alpha = 0.2f),
        disabledContentColor = Color.White.copy(alpha = 0.38f),
    )

    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = shape,
        colors = buttonColors,
        border = borderStroke,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
        content = content,
    )
}
