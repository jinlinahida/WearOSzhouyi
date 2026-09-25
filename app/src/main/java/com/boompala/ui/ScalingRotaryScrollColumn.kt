package com.boompala.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.AutoCenteringParams
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.ScalingLazyColumnDefaults
import androidx.wear.compose.foundation.lazy.ScalingLazyListScope
import androidx.wear.compose.foundation.lazy.ScalingLazyListState
import androidx.wear.compose.foundation.lazy.ScalingParams
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.foundation.requestFocusOnHierarchyActive
import androidx.wear.compose.foundation.rotary.RotaryScrollableDefaults
import androidx.wear.compose.foundation.rotary.rotaryScrollable
import androidx.wear.compose.material3.ScreenScaffold

internal fun resolveScalingParams(animationsEnabled: Boolean): ScalingParams =
    if (animationsEnabled) {
        ScalingLazyColumnDefaults.scalingParams()
    } else {
        ScalingLazyColumnDefaults.scalingParams(
            edgeScale = 1.0f,
            edgeAlpha = 1.0f,
        )
    }

/**
 * Scaling Wear OS list with fish-eye lens transformation for circular watch faces.
 *
 * Features:
 * - Automatically scales items up in the center (1.0x) and scales/fades them down near the curved edges;
 * - Fully integrated with ScreenScaffold to provide native Wear OS scroll indicator;
 * - Seamless rotary crown handling with customizable haptic feedback;
 * - Graceful degradation to 1.0x flat scaling when animations are disabled.
 */
@Composable
fun ScalingRotaryScrollColumn(
    rotaryEnabled: Boolean,
    hapticFeedbackEnabled: Boolean = LocalHapticFeedbackEnabled.current,
    animationsEnabled: Boolean = true,
    modifier: Modifier = Modifier.fillMaxSize(),
    state: ScalingLazyListState = rememberScalingLazyListState(),
    contentPadding: PaddingValues = PaddingValues(),
    itemSpacing: Dp = 8.dp,
    autoCentering: AutoCenteringParams? = AutoCenteringParams(itemIndex = 0),
    scalingParams: ScalingParams = resolveScalingParams(animationsEnabled),
    content: ScalingLazyListScope.() -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    val rotaryBehavior = if (rotaryEnabled) {
        RotaryScrollableDefaults.behavior(
            scrollableState = state,
            hapticFeedbackEnabled = hapticFeedbackEnabled,
        )
    } else {
        null
    }
    val rotaryModifier = remember(rotaryEnabled, rotaryBehavior, focusRequester, modifier) {
        if (rotaryEnabled && rotaryBehavior != null) {
            modifier
                .requestFocusOnHierarchyActive()
                .rotaryScrollable(
                    behavior = rotaryBehavior,
                    focusRequester = focusRequester,
                )
        } else {
            modifier
        }
    }

    ScreenScaffold(
        scrollState = state,
        contentPadding = contentPadding,
    ) { scaffoldPadding ->
        ScalingLazyColumn(
            state = state,
            modifier = rotaryModifier,
            contentPadding = scaffoldPadding,
            verticalArrangement = Arrangement.spacedBy(itemSpacing),
            scalingParams = scalingParams,
            autoCentering = autoCentering,
            content = content,
        )
    }
}
