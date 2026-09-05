package com.boompala.ui.muyu

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.boompala.R
import com.boompala.ui.AppHaptics
import com.boompala.ui.LocalHapticFeedbackEnabled
import com.boompala.ui.LocalHapticIntensity
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

private data class FloatingMerit(
    val id: Long,
    val text: String,
)

/**
 * 腕上极简电子木鱼界面。
 * 遵循小米手环同款纯粹、克制、无声设计：
 * 1. 绝对静音，无任何音频侵扰；
 * 2. 贴腕线性马达专属硬件级触觉（AppHaptics.muyuTap，还原实木敲击质感）；
 * 3. 屏幕任意区域点击即敲击，支持数码表冠逐格转动敲击；
 * 4. 真实弹簧形变、光晕涟漪与浮动“功德 +1”；
 * 5. 极简手动 / 自动模式切换。
 */
@Composable
fun MuyuScreen(
    initialCount: Long,
    onIncrementCount: (Long) -> Unit,
    rotaryEnabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val hapticEnabled = LocalHapticFeedbackEnabled.current
    val hapticIntensity = LocalHapticIntensity.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    var totalCount by remember { mutableLongStateOf(initialCount) }
    var isAutoMode by remember { mutableStateOf(false) }

    // 木鱼弹性形变动效（Spring Physics）
    val scaleAnim = remember { Animatable(1.0f) }

    // 敲击金色光晕涟漪
    val rippleRadius = remember { Animatable(0f) }
    val rippleAlpha = remember { Animatable(0f) }

    // 向上渐隐飘字队列
    val floatingList = remember { mutableStateListOf<FloatingMerit>() }

    // 核心敲击触发逻辑
    val triggerStrike: () -> Unit = remember(hapticIntensity, hapticEnabled) {
        {
            // 1. 手腕硬件级清脆触感
            AppHaptics.muyuTap(context, intensity = hapticIntensity, enabled = hapticEnabled)

            // 2. 本地计数递增与异步持久化
            totalCount += 1L
            onIncrementCount(1L)

            // 3. 产生漂浮文字
            val meritId = System.nanoTime()
            floatingList.add(FloatingMerit(id = meritId, text = "功德 +1"))

            // 4. 弹性形变动画
            scope.launch {
                scaleAnim.snapTo(0.90f)
                scaleAnim.animateTo(
                    targetValue = 1.0f,
                    animationSpec = spring(
                        dampingRatio = 0.45f,
                        stiffness = 380f,
                    ),
                )
            }

            // 5. 扩散光晕动画
            scope.launch {
                rippleRadius.snapTo(50f)
                rippleAlpha.snapTo(0.55f)
                launch {
                    rippleRadius.animateTo(
                        targetValue = 95f,
                        animationSpec = tween(durationMillis = 360, easing = LinearOutSlowInEasing),
                    )
                }
                launch {
                    rippleAlpha.animateTo(
                        targetValue = 0f,
                        animationSpec = tween(durationMillis = 360, easing = FastOutLinearInEasing),
                    )
                }
            }
        }
    }

    // 自动敲击模式协程循环（1.0 秒节拍）
    LaunchedEffect(isAutoMode) {
        if (isAutoMode) {
            while (isActive && isAutoMode) {
                triggerStrike()
                delay(1000L)
            }
        }
    }

    // 退出界面时自动停止自动敲击
    DisposableEffect(Unit) {
        onDispose {
            isAutoMode = false
        }
    }

    // 表冠旋转交互焦点
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    var rotaryAccumulator by remember { mutableFloatStateOf(0f) }
    val rotaryThreshold = with(density) { 16.dp.toPx() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(focusRequester)
            .focusable()
            .onRotaryScrollEvent { event ->
                if (!rotaryEnabled) return@onRotaryScrollEvent false
                rotaryAccumulator += event.verticalScrollPixels
                if (abs(rotaryAccumulator) >= rotaryThreshold) {
                    rotaryAccumulator = 0f
                    triggerStrike()
                    true
                } else {
                    false
                }
            }
            // 全屏点击即敲击（全屏盲操）
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = triggerStrike,
            ),
        contentAlignment = Alignment.Center,
    ) {
        // 背景层：敲击扩散的光晕涟漪
        val currentRadius = rippleRadius.value
        val currentAlpha = rippleAlpha.value
        if (currentAlpha > 0.01f) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    color = Color(0xFFD4AF37).copy(alpha = currentAlpha),
                    radius = currentRadius.dp.toPx(),
                    center = center,
                    style = Stroke(width = 2.dp.toPx()),
                )
            }
        }

        // 顶层界面元素布局
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 12.dp, horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // 顶部：功德计数
            Text(
                text = stringResource(R.string.muyu_merit),
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFFA8987E),
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = totalCount.toString(),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    letterSpacing = 1.sp,
                ),
                color = Color(0xFFF2E6CE),
            )

            Spacer(modifier = Modifier.weight(1f))

            // 中央：木鱼主图与浮动文字
            Box(
                modifier = Modifier.size(132.dp),
                contentAlignment = Alignment.Center,
            ) {
                // 木鱼图形主体（受弹性缩放驱动）
                Image(
                    painter = painterResource(id = R.drawable.img_muyu),
                    contentDescription = stringResource(R.string.muyu_title),
                    modifier = Modifier
                        .size(124.dp)
                        .scale(scaleAnim.value),
                )

                // 向上浮动的“功德 +1”
                floatingList.forEach { item ->
                    FloatingMeritText(
                        item = item,
                        onDismiss = { floatingList.remove(item) },
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // 底部：极简手动 / 自动模式切换胶囊
            Box(
                modifier = Modifier
                    .background(
                        color = if (isAutoMode) Color(0xFF332717) else Color(0xFF1E1A16),
                        shape = RoundedCornerShape(16.dp),
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) {
                        AppHaptics.click(context, intensity = hapticIntensity, enabled = hapticEnabled)
                        isAutoMode = !isAutoMode
                    }
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (isAutoMode) {
                        "${stringResource(R.string.muyu_auto_mode)} · 1s"
                    } else {
                        stringResource(R.string.muyu_manual_mode)
                    },
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                    color = if (isAutoMode) Color(0xFFFFD54F) else Color(0xFFC4B69F),
                )
            }
        }
    }
}

/**
 * 极简浮动“功德 +1”文字。
 * 出现后在 400ms 内向上飘动 28dp 并渐隐消失。
 */
@Composable
private fun FloatingMeritText(
    item: FloatingMerit,
    onDismiss: () -> Unit,
) {
    val offsetY = remember { Animatable(0f) }
    val alpha = remember { Animatable(1.0f) }

    LaunchedEffect(item.id) {
        launch {
            offsetY.animateTo(
                targetValue = -32f,
                animationSpec = tween(durationMillis = 420, easing = LinearOutSlowInEasing),
            )
        }
        launch {
            delay(100L)
            alpha.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 320, easing = FastOutLinearInEasing),
            )
            onDismiss()
        }
    }

    Text(
        text = item.text,
        color = Color(0xFFFFE082),
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .offset { IntOffset(0, offsetY.value.roundToInt()) }
            .alpha(alpha.value),
    )
}
