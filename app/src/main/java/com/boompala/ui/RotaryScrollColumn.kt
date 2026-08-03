package com.boompala.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.Dp
import androidx.wear.compose.foundation.requestFocusOnHierarchyActive
import androidx.wear.compose.foundation.rotary.RotaryScrollableDefaults
import androidx.wear.compose.foundation.rotary.rotaryScrollable
import androidx.wear.compose.material3.ScreenScaffold

/**
 * Lightweight Wear Material 3 list shared by all vertically scrolling screens.
 *
 * ScreenScaffold and LazyColumn receive the exact same state:
 * - the list consumes touch and rotary scroll deltas;
 * - the scaffold derives and displays the right-side position indicator;
 * - rotaryScrollable supplies Wear OS haptic feedback without the additional
 *   measuring and transformation pipeline of TransformingLazyColumn.
 */
@Composable
fun RotaryScrollColumn(
    rotaryEnabled: Boolean,
    modifier: Modifier = Modifier.fillMaxSize(),
    state: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(),
    itemSpacing: Dp,
    content: LazyListScope.() -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    val rotaryBehavior = if (rotaryEnabled) {
        RotaryScrollableDefaults.behavior(
            scrollableState = state,
            hapticFeedbackEnabled = true,
        )
    } else {
        null
    }
    val rotaryModifier = if (rotaryEnabled) {
        modifier
            .requestFocusOnHierarchyActive()
            .rotaryScrollable(
                behavior = requireNotNull(rotaryBehavior),
                focusRequester = focusRequester,
            )
    } else {
        modifier
    }

    ScreenScaffold(
        scrollState = state,
        contentPadding = contentPadding,
    ) { scaffoldPadding ->
        LazyColumn(
            state = state,
            modifier = rotaryModifier,
            contentPadding = scaffoldPadding,
            verticalArrangement = Arrangement.spacedBy(itemSpacing),
            content = content,
        )
    }
}
