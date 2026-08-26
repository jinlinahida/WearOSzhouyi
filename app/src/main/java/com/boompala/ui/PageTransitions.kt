package com.boompala.ui

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

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

internal val DecelEasing = CubicBezierEasing(0.0f, 0.0f, 0.2f, 1.0f)
internal val AccelEasing = CubicBezierEasing(0.4f, 0.0f, 1.0f, 1.0f)

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
                animationSpec = tween(SLIDE_ENTER_DURATION_MS, easing = DecelEasing),
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
                animationSpec = tween(SLIDE_ENTER_DURATION_MS, easing = DecelEasing),
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
                animationSpec = tween(SLIDE_ENTER_DURATION_MS, easing = DecelEasing),
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
    hapticEnabled: Boolean = true,
): Modifier {
    if (!enabled) return this

    val isPressed by interactionSource.collectIsPressedAsState()
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(isPressed) {
        if (isPressed && hapticEnabled) {
            haptic.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
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
