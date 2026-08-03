package com.boompala.ui

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performRotaryScrollInput
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.AppScaffold
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalTestApi::class)
class RotaryScrollColumnTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun enabledColumnAcquiresFocusAndRotaryInputScrollsItsLazyListState() {
        lateinit var state: LazyListState
        composeRule.setContent {
            AppScaffold {
                state = rememberLazyListState()
                TestRotaryColumn(
                    rotaryEnabled = true,
                    state = state,
                )
            }
        }

        composeRule.onNodeWithTag(ROTARY_LIST_TAG).assertIsFocused()
        composeRule.onNodeWithTag(ROTARY_LIST_TAG).performRotaryScrollInput {
            rotateToScrollVertically(600f)
        }
        composeRule.waitForIdle()

        composeRule.runOnIdle {
            assertTrue(
                "Rotary pixels must be dispatched to the LazyColumn state.",
                state.canScrollBackward,
            )
        }
    }

    @Test
    fun disabledColumnDoesNotConsumeRotaryInput() {
        lateinit var state: LazyListState
        composeRule.setContent {
            AppScaffold {
                state = rememberLazyListState()
                TestRotaryColumn(
                    rotaryEnabled = false,
                    state = state,
                )
            }
        }

        composeRule.onNodeWithTag(ROTARY_LIST_TAG).performRotaryScrollInput {
            rotateToScrollVertically(600f)
        }
        composeRule.waitForIdle()

        composeRule.runOnIdle {
            assertEquals(0, state.firstVisibleItemIndex)
            assertEquals(0, state.firstVisibleItemScrollOffset)
        }
    }

    @Composable
    private fun TestRotaryColumn(
        rotaryEnabled: Boolean,
        state: LazyListState,
    ) {
        RotaryScrollColumn(
            rotaryEnabled = rotaryEnabled,
            state = state,
            modifier = Modifier.testTag(ROTARY_LIST_TAG),
            itemSpacing = 0.dp,
        ) {
            items(30) { index ->
                androidx.wear.compose.material3.Text(
                    text = "Item $index",
                    modifier = Modifier.height(80.dp),
                )
            }
        }
    }

    private companion object {
        const val ROTARY_LIST_TAG = "rotary-list"
    }
}
