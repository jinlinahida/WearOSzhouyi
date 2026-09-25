package com.boompala.ui

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import com.boompala.settings.HapticIntensity

internal enum class NavigationDirection {
    FORWARD,
    BACKWARD,
    LATERAL,
}

// Wear OS Motion Timings and Curves (Faithful to WYS App Market Reference)
private const val SLIDE_ENTER_DURATION_MS = 280
private const val SLIDE_EXIT_DURATION_MS = 260
private const val FADE_DURATION_MS = 200
private const val PARALLAX_OFFSET_FRACTION = 0.20f

internal val AccelEasing = CubicBezierEasing(0.4f, 0.0f, 1.0f, 1.0f)

// Material Motion 规范的 emphasized-decelerate：起手柔和、收尾更长，避免满速冲出。
internal val EmphasizedDecelEasing = CubicBezierEasing(0.05f, 0.0f, 0.1f, 1.0f)

// 全局触觉开关与强度，由 BoompalaApp 根据设置提供；默认开启以保证独立调用点不受影响。
internal val LocalHapticFeedbackEnabled = staticCompositionLocalOf { true }
internal val LocalHapticIntensity = staticCompositionLocalOf { HapticIntensity.STANDARD }

/**
 * 应用级触觉反馈：直驱 [android.os.Vibrator]。
 *
 * 针对 Wear OS 手表硬件与 ROM 特性的适配要点：
 * 1. 优先采用 VibratorManager.defaultVibrator 并兜底 Context.getSystemService(Vibrator::class.java)，
 *    确保在各品牌 Wear OS 系统（Galaxy Watch, TicWatch, OPPO Watch, Pixel Watch 等）上稳定获取硬件；
 * 2. 移除盲目调用的 v.cancel()，避免在 Binder 异步调度中与新发出的短脉冲产生竞态导致震动被中途截断；
 * 3. 显式配置 VibrationAttributes(USAGE_TOUCH)，确保系统触觉策略将其视为前台 UI 交互反馈并顺利放行；
 * 4. 优先使用 EFFECT_CLICK（标准触觉点击），并在 HAL 不支持或失效时自动回退为显式毫秒级 OneShot 波形，
 *    彻底解决微弱的 EFFECT_TICK 在手腕上无法感知或被系统静默丢弃的问题；
 * 5. 提供翻牌（cardFlip）、起卦落定（coinToss，区分动爻双脉冲与静爻单脉冲）等专属触感。
 */
internal object AppHaptics {
    @Volatile
    private var vibrator: android.os.Vibrator? = null
    @Volatile
    private var resolved = false

    private fun getVibrator(context: android.content.Context): android.os.Vibrator? {
        if (!resolved) {
            val appContext = context.applicationContext ?: context
            val v = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                appContext.getSystemService(android.os.VibratorManager::class.java)?.defaultVibrator
                    ?: appContext.getSystemService(android.os.Vibrator::class.java)
            } else {
                @Suppress("DEPRECATION")
                appContext.getSystemService(android.content.Context.VIBRATOR_SERVICE) as? android.os.Vibrator
            }
            vibrator = v
            resolved = true
        }
        return vibrator
    }

    private fun vibrateEffect(v: android.os.Vibrator, effect: android.os.VibrationEffect) {
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                Api30HapticsHelper.vibrateWithTouchAttributes(v, effect)
            } else {
                v.vibrate(effect)
            }
        } catch (_: Throwable) {
            try {
                v.vibrate(effect)
            } catch (_: Throwable) {
                // 少数极度精简系统静默降级
            }
        }
    }

    @androidx.annotation.RequiresApi(android.os.Build.VERSION_CODES.R)
    private object Api30HapticsHelper {
        private val touchAttributes: android.os.VibrationAttributes by lazy {
            android.os.VibrationAttributes.Builder()
                .setUsage(android.os.VibrationAttributes.USAGE_TOUCH)
                .build()
        }

        fun vibrateWithTouchAttributes(v: android.os.Vibrator, effect: android.os.VibrationEffect) {
            v.vibrate(effect, touchAttributes)
        }
    }

    /**
     * 针对智能手表高端 X 轴线性马达（LRA）的底层触觉分发器：
     * 1. 优先调用系统预定义硬件微脉冲（[VibrationEffect.createPredefined]）：
     *    在 Wear OS 3/4/5（Galaxy Watch、Pixel Watch、TicWatch 等）上，系统驱动芯片（如 TI/Cirrus Logic）
     *    会直接合成包含“起振加速”与“反向主动电制动（Active Braking）”的专属谐振波形，彻底消除方波余震与外壳嗡鸣；
     * 2. 在极端精简系统或不支持 Predefined 的场景下，平滑回退为毫秒级超短微脉冲（4~8ms，振幅收敛至 80~190），
     *    避免强行灌入 220+ 满功率方波导致表壳共鸣与手腕发麻；
     * 3. 始终挂载 [android.os.VibrationAttributes.USAGE_TOUCH]，确保系统将其视为高优先级前台交互反馈。
     */
    private fun performHaptic(
        context: android.content.Context,
        predefinedEffectId: Int,
        fallbackDurationMs: Long,
        fallbackAmplitude: Int,
        enabled: Boolean = true,
    ) {
        if (!enabled) return
        val v = getVibrator(context) ?: return
        if (!v.hasVibrator()) return

        val effect = try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                try {
                    android.os.VibrationEffect.createPredefined(predefinedEffectId)
                } catch (_: Throwable) {
                    android.os.VibrationEffect.createOneShot(fallbackDurationMs, fallbackAmplitude)
                }
            } else {
                android.os.VibrationEffect.createOneShot(fallbackDurationMs, fallbackAmplitude)
            }
        } catch (_: Throwable) {
            return
        }
        vibrateEffect(v, effect)
    }

    /**
     * Level 1 · 基础按键与卡片点击反馈：
     * 模拟高级机械腕表微动开关手感，极度干脆清爽、零拖尾余震。
     * - LIGHT：原厂微点 Tick；
     * - STANDARD：标准清脆 Click；
     * - STRONG：扎实微触 Heavy Click。
     */
    fun click(
        context: android.content.Context,
        intensity: HapticIntensity = HapticIntensity.STANDARD,
        enabled: Boolean = true,
    ) {
        if (!enabled) return
        val (predefinedId, duration, amplitude) = when (intensity) {
            HapticIntensity.LIGHT -> Triple(android.os.VibrationEffect.EFFECT_TICK, 4L, 95)
            HapticIntensity.STANDARD -> Triple(android.os.VibrationEffect.EFFECT_CLICK, 6L, 135)
            HapticIntensity.STRONG -> Triple(android.os.VibrationEffect.EFFECT_HEAVY_CLICK, 8L, 185)
        }
        performHaptic(context, predefinedId, duration, amplitude, enabled)
    }

    /**
     * Level 2 · 仪式阻尼反馈：用于塔罗翻牌、六爻静爻落定。
     * 提供温润微顿挫阻尼感。
     */
    fun cardFlip(
        context: android.content.Context,
        intensity: HapticIntensity = HapticIntensity.STANDARD,
        enabled: Boolean = true,
    ) {
        if (!enabled) return
        val (predefinedId, duration, amplitude) = when (intensity) {
            HapticIntensity.LIGHT -> Triple(android.os.VibrationEffect.EFFECT_CLICK, 6L, 115)
            HapticIntensity.STANDARD -> Triple(android.os.VibrationEffect.EFFECT_HEAVY_CLICK, 8L, 155)
            HapticIntensity.STRONG -> Triple(android.os.VibrationEffect.EFFECT_HEAVY_CLICK, 12L, 205)
        }
        performHaptic(context, predefinedId, duration, amplitude, enabled)
    }

    /**
     * Level 3 · 变爻揭晓 / 动爻专属反馈：
     * - 静爻（少阳 7 / 少阴 8）：单枚金属铜钱清脆落定（Level 1 Click）；
     * - 动爻（老阳 9 / 老阴 6）：两枚铜钱交错清脆轻撞（优先 Double Click 硬件微脉冲），金属节拍分明。
     */
    fun coinToss(
        context: android.content.Context,
        isChanging: Boolean,
        intensity: HapticIntensity = HapticIntensity.STANDARD,
        enabled: Boolean = true,
    ) {
        if (!enabled) return
        val v = getVibrator(context) ?: return
        if (!v.hasVibrator()) return

        val effect = try {
            if (isChanging) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    try {
                        android.os.VibrationEffect.createPredefined(android.os.VibrationEffect.EFFECT_DOUBLE_CLICK)
                    } catch (_: Throwable) {
                        createCoinDoublePulse(intensity)
                    }
                } else {
                    createCoinDoublePulse(intensity)
                }
            } else {
                val (predefinedId, duration, amplitude) = when (intensity) {
                    HapticIntensity.LIGHT -> Triple(android.os.VibrationEffect.EFFECT_TICK, 4L, 95)
                    HapticIntensity.STANDARD -> Triple(android.os.VibrationEffect.EFFECT_CLICK, 6L, 135)
                    HapticIntensity.STRONG -> Triple(android.os.VibrationEffect.EFFECT_HEAVY_CLICK, 8L, 185)
                }
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    try {
                        android.os.VibrationEffect.createPredefined(predefinedId)
                    } catch (_: Throwable) {
                        android.os.VibrationEffect.createOneShot(duration, amplitude)
                    }
                } else {
                    android.os.VibrationEffect.createOneShot(duration, amplitude)
                }
            }
        } catch (_: Throwable) {
            return
        }
        vibrateEffect(v, effect)
    }

    private fun createCoinDoublePulse(intensity: HapticIntensity): android.os.VibrationEffect {
        return when (intensity) {
            HapticIntensity.LIGHT -> android.os.VibrationEffect.createWaveform(
                longArrayOf(0, 5, 24, 6),
                intArrayOf(0, 100, 0, 120),
                -1,
            )
            HapticIntensity.STANDARD -> android.os.VibrationEffect.createWaveform(
                longArrayOf(0, 6, 24, 8),
                intArrayOf(0, 130, 0, 155),
                -1,
            )
            HapticIntensity.STRONG -> android.os.VibrationEffect.createWaveform(
                longArrayOf(0, 8, 24, 11),
                intArrayOf(0, 160, 0, 195),
                -1,
            )
        }
    }

    /**
     * Level 4 · 脉冲微搏动：用于切脉时脉搏波峰微震反馈。
     * 极轻柔、收敛的微波形，模拟指下微弱起伏。
     */
    fun pulseBeat(
        context: android.content.Context,
        intensity: HapticIntensity = HapticIntensity.STANDARD,
        enabled: Boolean = true,
    ) {
        if (!enabled) return
        val v = getVibrator(context) ?: return
        if (!v.hasVibrator()) return

        val (duration, amplitude) = when (intensity) {
            HapticIntensity.LIGHT -> 3L to 70
            HapticIntensity.STANDARD -> 5L to 100
            HapticIntensity.STRONG -> 7L to 140
        }
        val effect = try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q && intensity == HapticIntensity.LIGHT) {
                try {
                    android.os.VibrationEffect.createPredefined(android.os.VibrationEffect.EFFECT_TICK)
                } catch (_: Throwable) {
                    android.os.VibrationEffect.createOneShot(duration, amplitude)
                }
            } else {
                android.os.VibrationEffect.createOneShot(duration, amplitude)
            }
        } catch (_: Throwable) {
            return
        }
        vibrateEffect(v, effect)
    }

    /**
     * Level 5 · 腕上木鱼专属敲击反馈：
     * 紧凑实木叩击微触感，彻底消除低频余震拖影，还原实木木槌的干脆反作用力。
     */
    fun muyuTap(
        context: android.content.Context,
        intensity: HapticIntensity = HapticIntensity.STANDARD,
        enabled: Boolean = true,
    ) {
        if (!enabled) return
        val (predefinedId, duration, amplitude) = when (intensity) {
            HapticIntensity.LIGHT -> Triple(android.os.VibrationEffect.EFFECT_TICK, 4L, 105)
            HapticIntensity.STANDARD -> Triple(android.os.VibrationEffect.EFFECT_CLICK, 6L, 145)
            HapticIntensity.STRONG -> Triple(android.os.VibrationEffect.EFFECT_HEAVY_CLICK, 8L, 195)
        }
        performHaptic(context, predefinedId, duration, amplitude, enabled)
    }

    /**
     * Level 6 · 页面返回与手势 Dismiss 触感：
     * 针对手表端物理返回键与 SwipeToDismissBox 滑动关闭落定设计。
     * 采用清爽轻盈的 Tick 级微触感，传达“退后、解绑、归位”的轻盈质感。
     */
    fun back(
        context: android.content.Context,
        intensity: HapticIntensity = HapticIntensity.STANDARD,
        enabled: Boolean = true,
    ) {
        if (!enabled) return
        val (predefinedId, duration, amplitude) = when (intensity) {
            HapticIntensity.LIGHT -> Triple(android.os.VibrationEffect.EFFECT_TICK, 3L, 75)
            HapticIntensity.STANDARD -> Triple(android.os.VibrationEffect.EFFECT_TICK, 4L, 110)
            HapticIntensity.STRONG -> Triple(android.os.VibrationEffect.EFFECT_CLICK, 6L, 145)
        }
        performHaptic(context, predefinedId, duration, amplitude, enabled)
    }

    /**
     * 设置项预览试听触感：直接执行对应 Level 1 点击。
     */
    fun preview(context: android.content.Context, intensity: HapticIntensity) {
        click(context, intensity = intensity, enabled = true)
    }
}

// Press Feedback Physics from Reference App (InteractiveHighlight Spec)
// dampingRatio = 0.5f, stiffness = 300f
private const val PRESS_SPRING_DAMPING = 0.5f
private const val PRESS_SPRING_STIFFNESS = 300f
private const val PRESS_SCALE_TARGET = 0.96f

/**
 * Returns the hierarchical depth of an [AppScreen].
 * Lower depth is closer to root/home; higher depth is deeper in detail/result.
 */
internal fun AppScreen.hierarchyDepth(): Int = when (this) {
    AppScreen.HOME -> 0
    AppScreen.WELCOME -> 0

    // Level 1: Primary feature entry screens
    AppScreen.YAO_INPUT,
    AppScreen.MEIHUA_TIME,
    AppScreen.XIAO_LIU_REN,
    AppScreen.DAILY_FORTUNE,
    AppScreen.TAROT_ONE_CARD,
    AppScreen.TAROT_THREE_CARD,
    AppScreen.TAROT_HOLY_TRIANGLE,
    AppScreen.TAROT_CELTIC_CROSS,
    AppScreen.DESTINY_CHART_MENU,
    AppScreen.PULSE_MEASURE,
    AppScreen.MUYU,
    AppScreen.COMPASS,
    AppScreen.ARCHIVES,
    AppScreen.BROWSE,
    AppScreen.SETTINGS -> 1

    // Level 2: Sub-browsers, results, tags, about
    AppScreen.RESULT,
    AppScreen.MEIHUA_RESULT,
    AppScreen.PULSE_RESULT,
    AppScreen.ARCHIVE_DETAIL,
    AppScreen.ARCHIVE_TAG,
    AppScreen.HEXAGRAM_BROWSER,
    AppScreen.KNOWLEDGE_LIST,
    AppScreen.TAROT_BROWSER,
    AppScreen.BAZI_DETAIL,
    AppScreen.WESTERN_CHART_DETAIL,
    AppScreen.NUMEROLOGY_DETAIL,
    AppScreen.BONE_WEIGHT_DETAIL,
    AppScreen.NINE_STAR_DETAIL,
    AppScreen.ABOUT -> 2

    // Level 3: Individual item detail pages
    AppScreen.HEXAGRAM_DETAIL,
    AppScreen.KNOWLEDGE_DETAIL,
    AppScreen.TAROT_CARD_DETAIL -> 3
}

/**
 * Calculates whether transitioning from [from] to [to] represents a forward drill-down,
 * a backward pop, or a lateral transition.
 */
internal fun calculateNavigationDirection(
    from: AppScreen,
    to: AppScreen,
    welcomeReturnScreen: AppScreen = AppScreen.HOME,
    archiveReturnScreen: AppScreen = AppScreen.HOME,
): NavigationDirection {
    if (from == to) return NavigationDirection.LATERAL

    // Direct parent/child checks based on backDestination
    if (to.backDestination() == from) {
        return NavigationDirection.FORWARD
    }
    if (from.backDestination() == to) {
        return NavigationDirection.BACKWARD
    }

    // Special cases: WELCOME entered from ABOUT -> back to ABOUT
    if (from == AppScreen.ABOUT && to == AppScreen.WELCOME) {
        return NavigationDirection.FORWARD
    }
    if (from == AppScreen.WELCOME && to == welcomeReturnScreen && welcomeReturnScreen != AppScreen.HOME) {
        return NavigationDirection.BACKWARD
    }

    // Special cases: ARCHIVE_TAG
    if (to == AppScreen.ARCHIVE_TAG) {
        return NavigationDirection.FORWARD
    }
    if (from == AppScreen.ARCHIVE_TAG && to == archiveReturnScreen) {
        return NavigationDirection.BACKWARD
    }

    // Compare depth
    val fromDepth = from.hierarchyDepth()
    val toDepth = to.hierarchyDepth()

    return when {
        toDepth > fromDepth -> NavigationDirection.FORWARD
        toDepth < fromDepth -> NavigationDirection.BACKWARD
        else -> NavigationDirection.FORWARD
    }
}

/**
 * Spatial Continuity Motion System for Wear OS.
 *
 * FORWARD (Push / Drill-Down):
 *  - Target page enters smoothly from right (+100% X) with standard deceleration + fade.
 *  - Origin page recedes subtly to left (-20% X parallax) with gentle fade.
 *  - targetContentZIndex = 1f (incoming page slides directly over the origin page).
 *
 * BACKWARD (Pop / Return):
 *  - Origin page exits to right (+100% X) with standard acceleration + fade.
 *  - Target page restores from left (-20% X parallax) back to center.
 *  - targetContentZIndex = 0f (exiting top page stays on top until completely offscreen).
 *
 * LATERAL (Sibling / Tag):
 *  - Subtle horizontal slide (±15% X) + fade.
 */
internal fun pageTransitionSpec(
    direction: NavigationDirection,
    animationsEnabled: Boolean,
): ContentTransform {
    if (!animationsEnabled) {
        return EnterTransition.None togetherWith ExitTransition.None
    }

    return when (direction) {
        NavigationDirection.FORWARD -> {
            val enter = fadeIn(
                animationSpec = tween(FADE_DURATION_MS, easing = LinearOutSlowInEasing),
            ) + slideInHorizontally(
                initialOffsetX = { fullWidth -> fullWidth },
                animationSpec = tween(SLIDE_ENTER_DURATION_MS, easing = EmphasizedDecelEasing),
            )

            val exit = fadeOut(
                animationSpec = tween(FADE_DURATION_MS, easing = FastOutLinearInEasing),
            ) + slideOutHorizontally(
                targetOffsetX = { fullWidth -> -(fullWidth * PARALLAX_OFFSET_FRACTION).toInt() },
                animationSpec = tween(SLIDE_EXIT_DURATION_MS, easing = AccelEasing),
            )

            enter.togetherWith(exit).apply {
                targetContentZIndex = 1f
            }
        }

        NavigationDirection.BACKWARD -> {
            val enter = fadeIn(
                animationSpec = tween(FADE_DURATION_MS, easing = LinearOutSlowInEasing),
            ) + slideInHorizontally(
                initialOffsetX = { fullWidth -> -(fullWidth * PARALLAX_OFFSET_FRACTION).toInt() },
                animationSpec = tween(SLIDE_ENTER_DURATION_MS, easing = EmphasizedDecelEasing),
            )

            val exit = fadeOut(
                animationSpec = tween(FADE_DURATION_MS, easing = FastOutLinearInEasing),
            ) + slideOutHorizontally(
                targetOffsetX = { fullWidth -> fullWidth },
                animationSpec = tween(SLIDE_EXIT_DURATION_MS, easing = AccelEasing),
            )

            enter.togetherWith(exit).apply {
                targetContentZIndex = 0f
            }
        }

        NavigationDirection.LATERAL -> {
            val enter = fadeIn(
                animationSpec = tween(FADE_DURATION_MS, easing = LinearOutSlowInEasing),
            ) + slideInHorizontally(
                initialOffsetX = { (it * 0.15f).toInt() },
                animationSpec = tween(SLIDE_ENTER_DURATION_MS, easing = EmphasizedDecelEasing),
            )

            val exit = fadeOut(
                animationSpec = tween(FADE_DURATION_MS, easing = FastOutLinearInEasing),
            ) + slideOutHorizontally(
                targetOffsetX = { -(it * 0.15f).toInt() },
                animationSpec = tween(SLIDE_EXIT_DURATION_MS, easing = AccelEasing),
            )

            enter.togetherWith(exit).apply {
                targetContentZIndex = 1f
            }
        }
    }
}

/**
 * 统一的"加载 -> 内容"转场规范（仿 WYS App Market 的具名 ContentTransition 实践）。
 * 所有页面内加载态到内容态的 AnimatedContent 都应使用该函数，
 * 并在关闭动画设置下退化为无转场。
 */
internal fun loadingContentTransitionSpec(animationsEnabled: Boolean): ContentTransform {
    if (!animationsEnabled) {
        return EnterTransition.None togetherWith ExitTransition.None
    }
    return (fadeIn(tween(240, easing = LinearOutSlowInEasing)) +
        scaleIn(initialScale = 0.95f, animationSpec = tween(280, easing = FastOutSlowInEasing)))
        .togetherWith(
            fadeOut(tween(160, easing = FastOutLinearInEasing)) +
                scaleOut(targetScale = 0.95f, animationSpec = tween(200, easing = FastOutLinearInEasing)),
        )
}

/**
 * Tactile Press Feedback faithful to Reference App's InteractiveHighlight.
 * - Spring damping = 0.5f, stiffness = 300f
 * - Scale down to 0.96f on press, springs back to 1.0f on release
 * - Triggers haptic feedback on press threshold
 *
 * 调用方必须传入与可点击组件共用的 [interactionSource]（例如同时传给
 * Button/OutlinedButton 的 interactionSource 参数），否则按压状态无法被观察到。
 */
@Composable
fun Modifier.wearPressFeedback(
    interactionSource: MutableInteractionSource,
    enabled: Boolean = true,
    hapticEnabled: Boolean = LocalHapticFeedbackEnabled.current,
    intensity: HapticIntensity = LocalHapticIntensity.current,
): Modifier {
    if (!enabled) return this

    val isPressed by interactionSource.collectIsPressedAsState()
    val context = LocalContext.current

    // 震动用事件流而非按压状态采样：collectIsPressedAsState 按帧合并状态，
    // 快速点击（按下+抬起在同一帧内）永远不会观察到按压，震动被静默丢弃；
    // interactions 流能看到每一次 Press 事件，与帧率无关。
    LaunchedEffect(interactionSource, hapticEnabled, intensity) {
        if (hapticEnabled) {
            interactionSource.interactions.collect { interaction ->
                if (interaction is PressInteraction.Press) {
                    AppHaptics.click(context, intensity = intensity, enabled = hapticEnabled)
                }
            }
        }
    }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) PRESS_SCALE_TARGET else 1.0f,
        animationSpec = spring(
            dampingRatio = PRESS_SPRING_DAMPING,
            stiffness = PRESS_SPRING_STIFFNESS,
            visibilityThreshold = 0.001f,
        ),
        label = "wearPressScale",
    )

    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
}
