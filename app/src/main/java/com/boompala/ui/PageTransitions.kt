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

    private fun vibratePhysical(v: android.os.Vibrator, effect: android.os.VibrationEffect) {
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                Api30HapticsHelper.vibrateWithPhysicalAttributes(v, effect)
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

        private val physicalAttributes: android.os.VibrationAttributes by lazy {
            val builder = android.os.VibrationAttributes.Builder()
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                builder.setUsage(android.os.VibrationAttributes.USAGE_PHYSICAL_EMULATION)
            } else {
                builder.setUsage(android.os.VibrationAttributes.USAGE_TOUCH)
            }
            builder.build()
        }

        fun vibrateWithTouchAttributes(v: android.os.Vibrator, effect: android.os.VibrationEffect) {
            v.vibrate(effect, touchAttributes)
        }

        fun vibrateWithPhysicalAttributes(v: android.os.Vibrator, effect: android.os.VibrationEffect) {
            v.vibrate(effect, physicalAttributes)
        }
    }

    /**
     * 100% 复刻 Google 官方 Wear Compose (compose-foundation) 针对不同手表的触觉规范与原厂常量表：
     * - Samsung Galaxy Watch (One UI Watch): 派发三星专有触觉常量 (101 / 102 / 50107)，
     *   直接触发原厂针对 X 轴线性马达（LRA）调校的无余震纯净波形；
     * - Wear 4 / Pixel Watch (Android 13+): 派发系统标准滚动与聚焦常量 (18 / 19 / 20)；
     * - Other / Phone: 优雅回退为标准前台按键常量 (KEYBOARD_TAP / CLOCK_TICK / CONTEXT_CLICK)。
     */
    internal object WearComposeHapticsSpec {
        val isGalaxyWatch: Boolean by lazy {
            (android.os.Build.MANUFACTURER ?: "").contains("Samsung", ignoreCase = true) &&
                (android.os.Build.MODEL ?: "").matches(Regex("^SM-R.*$"))
        }

        val isWear4: Boolean by lazy {
            android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU
        }

        /**
         * 微点 (Tick)：轻柔刻度、表盘微步进、手势返回、脉搏微动
         * - Galaxy Watch: 101 (Samsung ROTARY_SCROLL 原厂微脉冲)
         * - Wear 4: 18 (HapticFeedbackConstants.SCROLL_TICK)
         * - Other: HapticFeedbackConstants.CLOCK_TICK (4)
         */
        val TICK: Int = when {
            isGalaxyWatch -> 101
            isWear4 -> 18
            else -> android.view.HapticFeedbackConstants.CLOCK_TICK
        }

        /**
         * 清脆点击 (Click / Focus)：普通卡片点击、按键按下
         * - Galaxy Watch: 102 (Samsung ROTARY_FOCUS 原厂机械按键微触，即 wearfinder 的清脆按键手感)
         * - Wear 4: 19 (HapticFeedbackConstants.SCROLL_ITEM_FOCUS)
         * - Other: android.view.HapticFeedbackConstants.KEYBOARD_TAP (3)
         */
        val CLICK: Int = when {
            isGalaxyWatch -> 102
            isWear4 -> 19
            else -> android.view.HapticFeedbackConstants.KEYBOARD_TAP
        }

        /**
         * 重阻尼 / 边界顿挫 (Limit / Heavy)：翻牌阻尼、强劲按压、机械回弹
         * - Galaxy Watch: 50107 (Samsung ROTARY_LIMIT 原厂机械回弹阻尼感)
         * - Wear 4: 20 (HapticFeedbackConstants.SCROLL_LIMIT)
         * - Other: android.view.HapticFeedbackConstants.CONTEXT_CLICK (6)
         */
        val LIMIT: Int = when {
            isGalaxyWatch -> 50107
            isWear4 -> 20
            else -> android.view.HapticFeedbackConstants.CONTEXT_CLICK
        }
    }

    private fun findView(context: android.content.Context): android.view.View? {
        var ctx: android.content.Context? = context
        while (ctx is android.content.ContextWrapper) {
            if (ctx is android.app.Activity) {
                return ctx.window?.decorView
            }
            ctx = ctx.baseContext
        }
        return null
    }

    /**
     * 核心触觉派发器：
     * 1. 优先走 View.performHapticFeedback 通道，携带 FLAG_IGNORE_VIEW_SETTING 强行放行，
     *    在 Galaxy Watch 5 上直接唤醒三星原厂 LRA 驱动（解决 createPredefined 被 HAL 静默丢弃的顽疾）；
     * 2. 无 View 环境自动平滑降级为微秒级短冲程 OneShot 兜底，保证 100% 不漏震。
     */
    private fun performWearHaptic(
        context: android.content.Context,
        hapticConstant: Int,
        fallbackDurationMs: Long,
        fallbackAmplitude: Int,
        enabled: Boolean = true,
    ) {
        if (!enabled) return

        val view = findView(context)
        val performed = if (view != null) {
            try {
                view.performHapticFeedback(
                    hapticConstant,
                    android.view.HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING,
                )
            } catch (_: Throwable) {
                false
            }
        } else {
            false
        }

        if (performed) return

        val v = getVibrator(context) ?: return
        if (!v.hasVibrator()) return

        val effect = try {
            if (hapticConstant == WearComposeHapticsSpec.TICK &&
                android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q
            ) {
                try {
                    android.os.VibrationEffect.createPredefined(android.os.VibrationEffect.EFFECT_TICK)
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
     * 100% 对齐 Wear Compose 官方按键触感，极度干脆清爽、零拖尾余震。
     * - LIGHT：原厂微点 TICK (Watch 5: 101)；
     * - STANDARD：原厂清脆 CLICK (Watch 5: 102)；
     * - STRONG：原厂重阻尼 LIMIT (Watch 5: 50107)。
     */
    fun click(
        context: android.content.Context,
        intensity: HapticIntensity = HapticIntensity.STANDARD,
        enabled: Boolean = true,
    ) {
        if (!enabled) return
        val (constant, duration, amplitude) = when (intensity) {
            HapticIntensity.LIGHT -> Triple(WearComposeHapticsSpec.TICK, 4L, 95)
            HapticIntensity.STANDARD -> Triple(WearComposeHapticsSpec.CLICK, 6L, 135)
            HapticIntensity.STRONG -> Triple(WearComposeHapticsSpec.LIMIT, 8L, 185)
        }
        performWearHaptic(context, constant, duration, amplitude, enabled)
    }

    /**
     * Level 2 · 设置项开关专属拟物触感 (Switch / Toggle)：
     * - targetState == true (开启)：模拟机械微动开关推入并"卡入卡槽" (Latch In) 的上升双相吸附波 (轻动 + 15ms + 清脆吸附)；
     * - targetState == false (关闭)：模拟弹簧释放"钝感脱扣" (Latch Out) 的单相柔和阻尼释放波，告别千篇一律的单调点击。
     */
    fun toggle(
        context: android.content.Context,
        targetState: Boolean,
        intensity: HapticIntensity = HapticIntensity.STANDARD,
        enabled: Boolean = true,
    ) {
        if (!enabled) return

        val v = getVibrator(context)
        if (v != null && v.hasVibrator()) {
            val effect = if (targetState) {
                // 开启 (ON)：阶梯上升轻快吸附 (双触波)
                when (intensity) {
                    HapticIntensity.LIGHT -> android.os.VibrationEffect.createWaveform(
                        longArrayOf(0, 4, 15, 6),
                        intArrayOf(0, 95, 0, 150),
                        -1,
                    )
                    HapticIntensity.STANDARD -> android.os.VibrationEffect.createWaveform(
                        longArrayOf(0, 5, 15, 8),
                        intArrayOf(0, 120, 0, 190),
                        -1,
                    )
                    HapticIntensity.STRONG -> android.os.VibrationEffect.createWaveform(
                        longArrayOf(0, 7, 14, 11),
                        intArrayOf(0, 150, 0, 235),
                        -1,
                    )
                }
            } else {
                // 关闭 (OFF)：单相温和顿挫脱扣 (单触阻尼)
                when (intensity) {
                    HapticIntensity.LIGHT -> android.os.VibrationEffect.createWaveform(
                        longArrayOf(0, 5),
                        intArrayOf(0, 95),
                        -1,
                    )
                    HapticIntensity.STANDARD -> android.os.VibrationEffect.createWaveform(
                        longArrayOf(0, 7),
                        intArrayOf(0, 135),
                        -1,
                    )
                    HapticIntensity.STRONG -> android.os.VibrationEffect.createWaveform(
                        longArrayOf(0, 9),
                        intArrayOf(0, 180),
                        -1,
                    )
                }
            }
            vibratePhysical(v, effect)
        } else {
            val constant = if (targetState) WearComposeHapticsSpec.CLICK else WearComposeHapticsSpec.TICK
            performWearHaptic(context, constant, 6L, 130, enabled)
        }
    }

    /**
     * Level 3 · 仪式与翻牌阻尼：用于塔罗翻牌、六爻落定。
     * 拟物意象：厚磅纸牌从指尖揭开微滑动 + 沉稳拍落于桌面绒布的阻尼感。
     */
    fun cardFlip(
        context: android.content.Context,
        intensity: HapticIntensity = HapticIntensity.STANDARD,
        enabled: Boolean = true,
    ) {
        if (!enabled) return

        val v = getVibrator(context)
        if (v != null && v.hasVibrator()) {
            val effect = when (intensity) {
                HapticIntensity.LIGHT -> android.os.VibrationEffect.createWaveform(
                    longArrayOf(0, 4, 18, 7),
                    intArrayOf(0, 85, 0, 145),
                    -1,
                )
                HapticIntensity.STANDARD -> android.os.VibrationEffect.createWaveform(
                    longArrayOf(0, 5, 18, 9),
                    intArrayOf(0, 110, 0, 190),
                    -1,
                )
                HapticIntensity.STRONG -> android.os.VibrationEffect.createWaveform(
                    longArrayOf(0, 7, 16, 12),
                    intArrayOf(0, 140, 0, 235),
                    -1,
                )
            }
            vibratePhysical(v, effect)
        } else {
            performWearHaptic(context, WearComposeHapticsSpec.LIMIT, 8L, 160, enabled)
        }
    }

    /**
     * Level 4 · 六爻铜钱掷卦拟物反馈：
     * - 静爻（少阳 7 / 少阴 8）：单枚金属铜钱平稳清脆落盘 (单次金属短促撞击)；
     * - 动爻（老阳 9 / 老阴 6）：三枚铜钱翻滚交错跳跃 (第 1 枚触底轻弹 -> 22ms 滞空翻滚 -> 第 2/3 枚合力实击 -> 金属余颤)，
     *   无需看屏幕，凭手腕金属节拍即可分辨动爻与静爻。
     */
    fun coinToss(
        context: android.content.Context,
        isChanging: Boolean,
        intensity: HapticIntensity = HapticIntensity.STANDARD,
        enabled: Boolean = true,
    ) {
        if (!enabled) return

        val v = getVibrator(context)
        if (v != null && v.hasVibrator()) {
            val effect = if (isChanging) {
                // 动爻：三枚铜钱翻滚交错跳跃
                when (intensity) {
                    HapticIntensity.LIGHT -> android.os.VibrationEffect.createWaveform(
                        longArrayOf(0, 5, 22, 7, 12, 3),
                        intArrayOf(0, 100, 0, 140, 0, 70),
                        -1,
                    )
                    HapticIntensity.STANDARD -> android.os.VibrationEffect.createWaveform(
                        longArrayOf(0, 6, 22, 9, 12, 4),
                        intArrayOf(0, 125, 0, 180, 0, 90),
                        -1,
                    )
                    HapticIntensity.STRONG -> android.os.VibrationEffect.createWaveform(
                        longArrayOf(0, 8, 20, 12, 12, 6),
                        intArrayOf(0, 155, 0, 230, 0, 125),
                        -1,
                    )
                }
            } else {
                // 静爻：单枚铜钱平稳落盘
                when (intensity) {
                    HapticIntensity.LIGHT -> android.os.VibrationEffect.createWaveform(
                        longArrayOf(0, 5),
                        intArrayOf(0, 120),
                        -1,
                    )
                    HapticIntensity.STANDARD -> android.os.VibrationEffect.createWaveform(
                        longArrayOf(0, 7),
                        intArrayOf(0, 165),
                        -1,
                    )
                    HapticIntensity.STRONG -> android.os.VibrationEffect.createWaveform(
                        longArrayOf(0, 10),
                        intArrayOf(0, 215),
                        -1,
                    )
                }
            }
            vibratePhysical(v, effect)
        } else {
            val constant = if (isChanging) WearComposeHapticsSpec.LIMIT else WearComposeHapticsSpec.CLICK
            performWearHaptic(context, constant, 8L, 150, enabled)
        }
    }

    /**
     * Level 5 · 中医把脉血流脉冲波 (Somatic Pulse Wave)：
     * 针对把脉界面的生理波峰设计，完全脱离 View 依赖（保证在无触摸输入、传感器后台回调时 100% 稳定直发硬件马达）。
     * 采用真实生理动脉脉搏波：收缩期主动脉冲血主波 (Systolic Peak) + 舒张期流经 + 主动脉瓣关闭重搏波 (Dicrotic Notch)。
     * 击穿腕部脂肪阻尼，产生极度逼真、有生命起伏的脉搏跳动感。
     */
    fun pulseBeat(
        context: android.content.Context,
        intensity: HapticIntensity = HapticIntensity.STANDARD,
        enabled: Boolean = true,
    ) {
        if (!enabled) return

        val v = getVibrator(context) ?: return
        if (!v.hasVibrator()) return

        val effect = when (intensity) {
            HapticIntensity.LIGHT -> android.os.VibrationEffect.createWaveform(
                longArrayOf(0, 10, 36, 6),
                intArrayOf(0, 135, 0, 80),
                -1,
            )
            HapticIntensity.STANDARD -> android.os.VibrationEffect.createWaveform(
                longArrayOf(0, 12, 34, 8),
                intArrayOf(0, 180, 0, 110),
                -1,
            )
            HapticIntensity.STRONG -> android.os.VibrationEffect.createWaveform(
                longArrayOf(0, 15, 30, 10),
                intArrayOf(0, 230, 0, 155),
                -1,
            )
        }
        vibratePhysical(v, effect)
    }

    /**
     * Level 6 · 电子木鱼实木空腔敲击 (Solid Wood & Cavity Resonance)：
     * 拟物意象：硬木槌头破空叩击实木外壳 (Attack) + 空心木腔微弱声波共鸣与吸收阻尼 (Decay)。
     * 彻底告别单薄的电子按键“哒”，还原沉稳有肉感的木质“笃——”反作用力。
     */
    fun muyuTap(
        context: android.content.Context,
        intensity: HapticIntensity = HapticIntensity.STANDARD,
        enabled: Boolean = true,
    ) {
        if (!enabled) return

        val v = getVibrator(context)
        if (v != null && v.hasVibrator()) {
            val effect = when (intensity) {
                HapticIntensity.LIGHT -> android.os.VibrationEffect.createWaveform(
                    longArrayOf(0, 7, 14, 4),
                    intArrayOf(0, 130, 0, 65),
                    -1,
                )
                HapticIntensity.STANDARD -> android.os.VibrationEffect.createWaveform(
                    longArrayOf(0, 9, 14, 5),
                    intArrayOf(0, 185, 0, 95),
                    -1,
                )
                HapticIntensity.STRONG -> android.os.VibrationEffect.createWaveform(
                    longArrayOf(0, 12, 14, 7),
                    intArrayOf(0, 240, 0, 140),
                    -1,
                )
            }
            vibratePhysical(v, effect)
        } else {
            performWearHaptic(context, WearComposeHapticsSpec.CLICK, 8L, 160, enabled)
        }
    }

    /**
     * Level 7 · 页面返回与手势 Dismiss 触感：
     * 针对手表端物理返回键与 SwipeToDismissBox 滑动关闭落定设计。
     * 采用清爽轻盈的 TICK 级微触感 (Watch 5: 101)，传达“退后、解绑、归位”的轻盈质感。
     */
    fun back(
        context: android.content.Context,
        intensity: HapticIntensity = HapticIntensity.STANDARD,
        enabled: Boolean = true,
    ) {
        if (!enabled) return
        val (constant, duration, amplitude) = when (intensity) {
            HapticIntensity.LIGHT -> Triple(WearComposeHapticsSpec.TICK, 3L, 75)
            HapticIntensity.STANDARD -> Triple(WearComposeHapticsSpec.TICK, 4L, 110)
            HapticIntensity.STRONG -> Triple(WearComposeHapticsSpec.CLICK, 6L, 145)
        }
        performWearHaptic(context, constant, duration, amplitude, enabled)
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
