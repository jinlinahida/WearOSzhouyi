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

    private val touchAttributes: android.os.VibrationAttributes by lazy {
        android.os.VibrationAttributes.Builder()
            .setUsage(android.os.VibrationAttributes.USAGE_TOUCH)
            .build()
    }

    private fun getVibrator(context: android.content.Context): android.os.Vibrator? {
        if (!resolved) {
            val appContext = context.applicationContext ?: context
            val manager = appContext.getSystemService(android.os.VibratorManager::class.java)
            val v = manager?.defaultVibrator ?: appContext.getSystemService(android.os.Vibrator::class.java)
            vibrator = v
            resolved = true
        }
        return vibrator
    }

    private fun vibrateEffect(v: android.os.Vibrator, effect: android.os.VibrationEffect) {
        try {
            v.vibrate(effect, touchAttributes)
        } catch (_: Throwable) {
            try {
                v.vibrate(effect)
            } catch (_: Throwable) {
                // 少数极度精简系统静默降级
            }
        }
    }

    /**
     * Level 1 · 基础按键点击反馈：
     * 彻底摒弃不可靠的系统黑盒预定义常量（如导致 Galaxy Watch 哑火的 EFFECT_HEAVY_CLICK），
     * 全线采用显式参数化波形（OneShot），保证在所有手表硬件上 100% 触发且手感绝对一致。
     * - LIGHT (弱): 20ms / 振幅 170（轻柔微触）
     * - STANDARD (标准，新默认): 35ms / 振幅 230（清脆扎实，手腕有清晰机械下沉感，彻底告别偏弱，且绝不哑火）
     * - STRONG (强劲): 55ms / 振幅 255（充沛有力，走动时依然清晰）
     */
    fun click(
        context: android.content.Context,
        intensity: HapticIntensity = HapticIntensity.STANDARD,
        enabled: Boolean = true,
    ) {
        if (!enabled) return
        val v = getVibrator(context) ?: return
        if (!v.hasVibrator()) return

        val (duration, amplitude) = when (intensity) {
            HapticIntensity.LIGHT -> 20L to 170
            HapticIntensity.STANDARD -> 35L to 230
            HapticIntensity.STRONG -> 55L to 255
        }
        val effect = try {
            android.os.VibrationEffect.createOneShot(duration, amplitude)
        } catch (_: Throwable) {
            return
        }
        vibrateEffect(v, effect)
    }

    /**
     * Level 2 · 仪式阻尼反馈：用于塔罗翻牌、六爻静爻铜钱落定。
     * - LIGHT: 25ms / 振幅 180
     * - STANDARD: 45ms / 振幅 240
     * - STRONG: 65ms / 振幅 255
     */
    fun cardFlip(
        context: android.content.Context,
        intensity: HapticIntensity = HapticIntensity.STANDARD,
        enabled: Boolean = true,
    ) {
        if (!enabled) return
        val v = getVibrator(context) ?: return
        if (!v.hasVibrator()) return

        val (duration, amplitude) = when (intensity) {
            HapticIntensity.LIGHT -> 25L to 180
            HapticIntensity.STANDARD -> 45L to 240
            HapticIntensity.STRONG -> 65L to 255
        }
        val effect = try {
            android.os.VibrationEffect.createOneShot(duration, amplitude)
        } catch (_: Throwable) {
            return
        }
        vibrateEffect(v, effect)
    }

    /**
     * Level 3 · 变爻揭晓 / 动爻专属反馈：
     * - 静爻（少阳 7 / 少阴 8）：Level 2 仪式落定单脉冲；
     * - 动爻（老阳 9 / 老阴 6）：显式节奏双脉冲（震 - 停 - 强震），
     *   彻底摒弃容易失效的系统 EFFECT_DOUBLE_CLICK，确保在任何手表上双震节拍分明。
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
                when (intensity) {
                    HapticIntensity.LIGHT -> android.os.VibrationEffect.createWaveform(
                        longArrayOf(0, 20, 35, 30),
                        intArrayOf(0, 170, 0, 190),
                        -1,
                    )
                    HapticIntensity.STANDARD -> android.os.VibrationEffect.createWaveform(
                        longArrayOf(0, 30, 40, 50),
                        intArrayOf(0, 210, 0, 245),
                        -1,
                    )
                    HapticIntensity.STRONG -> android.os.VibrationEffect.createWaveform(
                        longArrayOf(0, 40, 40, 70),
                        intArrayOf(0, 255, 0, 255),
                        -1,
                    )
                }
            } else {
                val (duration, amplitude) = when (intensity) {
                    HapticIntensity.LIGHT -> 25L to 180
                    HapticIntensity.STANDARD -> 40L to 235
                    HapticIntensity.STRONG -> 60L to 255
                }
                android.os.VibrationEffect.createOneShot(duration, amplitude)
            }
        } catch (_: Throwable) {
            return
        }
        vibrateEffect(v, effect)
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
    AppScreen.COMPASS,
    AppScreen.ARCHIVES,
    AppScreen.BROWSE,
    AppScreen.SETTINGS -> 1

    // Level 2: Sub-browsers, results, tags, about
    AppScreen.RESULT,
    AppScreen.MEIHUA_RESULT,
    AppScreen.ARCHIVE_DETAIL,
    AppScreen.ARCHIVE_TAG,
    AppScreen.HEXAGRAM_BROWSER,
    AppScreen.KNOWLEDGE_LIST,
    AppScreen.TAROT_BROWSER,
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
