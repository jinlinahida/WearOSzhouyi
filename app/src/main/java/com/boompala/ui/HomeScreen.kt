package com.boompala.ui

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.OutlinedButton
import androidx.wear.compose.material3.Text
import com.boompala.R
import com.boompala.settings.AppSettings
import com.boompala.settings.HomeFeature

private const val CONTENT_TYPE_TITLE = "title"
private const val CONTENT_TYPE_BUTTON = "button"
private const val CONTENT_TYPE_OUTLINED_BUTTON = "outlined_button"

@Composable
fun HomeScreen(
    settings: AppSettings,
    onSixYaoClick: () -> Unit,
    onMeiHuaClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onXiaoLiuRenClick: () -> Unit,
    onArchiveClick: () -> Unit,
    onCompassClick: () -> Unit,
    onBrowseClick: () -> Unit,
    onDestinyChartClick: () -> Unit = { },
    onDailyFortuneClick: () -> Unit = { },
    onTarotClick: () -> Unit = { },
    onTarotThreeCardClick: () -> Unit = { },
    onTarotHolyTriangleClick: () -> Unit = { },
    onPulseClick: () -> Unit = { },
) {
    val metrics = LocalUiMetrics.current
    val titlePaddingModifier = remember(metrics.itemSpacing) {
        Modifier.padding(bottom = metrics.itemSpacing / 2)
    }
    val fullWidthModifier = Modifier.fillMaxWidth()
    val visibleFeatures = remember(settings.homeOrder, settings.hiddenHomeFeatures) {
        settings.visibleHomeFeatures()
    }

    RotaryScrollColumn(
        rotaryEnabled = settings.rotaryScrollingEnabled,
        modifier = Modifier.fillMaxSize(),
        contentPadding = metrics.screenPadding,
        itemSpacing = metrics.itemSpacing,
    ) {
        item(key = "title", contentType = CONTENT_TYPE_TITLE) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleLarge,
                modifier = titlePaddingModifier,
            )
        }

        visibleFeatures.forEach { feature ->
            when (feature) {
                HomeFeature.SIX_YAO -> {
                    item(key = "six-yao", contentType = CONTENT_TYPE_BUTTON) {
                        val pressInteraction = remember { MutableInteractionSource() }
                        BoompalaCardButton(
                            onClick = onSixYaoClick,
                            modifier = fullWidthModifier.wearPressFeedback(pressInteraction, hapticEnabled = settings.hapticFeedbackEnabled),
                            interactionSource = pressInteraction,
                        ) {
                            Text(stringResource(R.string.home_feature_six_yao))
                        }
                    }
                }

                HomeFeature.MEI_HUA -> {
                    item(key = "mei-hua", contentType = CONTENT_TYPE_BUTTON) {
                        val pressInteraction = remember { MutableInteractionSource() }
                        BoompalaCardButton(
                            onClick = onMeiHuaClick,
                            modifier = fullWidthModifier.wearPressFeedback(pressInteraction, hapticEnabled = settings.hapticFeedbackEnabled),
                            interactionSource = pressInteraction,
                        ) {
                            Text(stringResource(R.string.home_feature_mei_hua))
                        }
                    }
                }

                HomeFeature.DESTINY_CHART -> {
                    item(key = "destiny-chart", contentType = CONTENT_TYPE_BUTTON) {
                        val pressInteraction = remember { MutableInteractionSource() }
                        BoompalaCardButton(
                            onClick = onDestinyChartClick,
                            modifier = fullWidthModifier.wearPressFeedback(pressInteraction, hapticEnabled = settings.hapticFeedbackEnabled),
                            interactionSource = pressInteraction,
                        ) {
                            Text(stringResource(R.string.home_feature_destiny_chart))
                        }
                    }
                }

                HomeFeature.TAROT_ONE -> {
                    item(key = "tarot", contentType = CONTENT_TYPE_BUTTON) {
                        val pressInteraction = remember { MutableInteractionSource() }
                        BoompalaCardButton(
                            onClick = onTarotClick,
                            modifier = fullWidthModifier.wearPressFeedback(pressInteraction, hapticEnabled = settings.hapticFeedbackEnabled),
                            interactionSource = pressInteraction,
                        ) {
                            Text(stringResource(R.string.home_feature_tarot_one))
                        }
                    }
                }

                HomeFeature.TAROT_THREE -> {
                    item(key = "tarot-three", contentType = CONTENT_TYPE_BUTTON) {
                        val pressInteraction = remember { MutableInteractionSource() }
                        BoompalaCardButton(
                            onClick = onTarotThreeCardClick,
                            modifier = fullWidthModifier.wearPressFeedback(pressInteraction, hapticEnabled = settings.hapticFeedbackEnabled),
                            interactionSource = pressInteraction,
                        ) {
                            Text(stringResource(R.string.home_feature_tarot_three))
                        }
                    }
                }

                HomeFeature.TAROT_HOLY_TRIANGLE -> {
                    item(key = "tarot-holy-triangle", contentType = CONTENT_TYPE_BUTTON) {
                        val pressInteraction = remember { MutableInteractionSource() }
                        BoompalaCardButton(
                            onClick = onTarotHolyTriangleClick,
                            modifier = fullWidthModifier.wearPressFeedback(pressInteraction, hapticEnabled = settings.hapticFeedbackEnabled),
                            interactionSource = pressInteraction,
                        ) {
                            Text(stringResource(R.string.home_feature_tarot_holy_triangle))
                        }
                    }
                }

                HomeFeature.DAILY_FORTUNE -> {
                    item(key = "daily-fortune", contentType = CONTENT_TYPE_BUTTON) {
                        val pressInteraction = remember { MutableInteractionSource() }
                        BoompalaCardButton(
                            onClick = onDailyFortuneClick,
                            modifier = fullWidthModifier.wearPressFeedback(pressInteraction, hapticEnabled = settings.hapticFeedbackEnabled),
                            interactionSource = pressInteraction,
                        ) {
                            Text(stringResource(R.string.home_feature_daily_fortune))
                        }
                    }
                }

                HomeFeature.XIAO_LIU_REN -> {
                    item(key = "xiaoliuren", contentType = CONTENT_TYPE_BUTTON) {
                        val pressInteraction = remember { MutableInteractionSource() }
                        BoompalaCardButton(
                            onClick = onXiaoLiuRenClick,
                            modifier = fullWidthModifier.wearPressFeedback(pressInteraction, hapticEnabled = settings.hapticFeedbackEnabled),
                            interactionSource = pressInteraction,
                        ) {
                            Text(stringResource(R.string.home_feature_xiao_liu_ren))
                        }
                    }
                }

                HomeFeature.COMPASS -> {
                    item(key = "compass", contentType = CONTENT_TYPE_BUTTON) {
                        val pressInteraction = remember { MutableInteractionSource() }
                        BoompalaCardButton(
                            onClick = onCompassClick,
                            modifier = fullWidthModifier.wearPressFeedback(pressInteraction, hapticEnabled = settings.hapticFeedbackEnabled),
                            interactionSource = pressInteraction,
                        ) {
                            Text(stringResource(R.string.home_feature_compass))
                        }
                    }
                }

                HomeFeature.PULSE -> {
                    item(key = "pulse", contentType = CONTENT_TYPE_BUTTON) {
                        val pressInteraction = remember { MutableInteractionSource() }
                        BoompalaCardButton(
                            onClick = onPulseClick,
                            modifier = fullWidthModifier.wearPressFeedback(pressInteraction, hapticEnabled = settings.hapticFeedbackEnabled),
                            interactionSource = pressInteraction,
                        ) {
                            Text(stringResource(R.string.home_feature_pulse))
                        }
                    }
                }

                HomeFeature.ARCHIVES -> {
                    item(key = "archives", contentType = CONTENT_TYPE_OUTLINED_BUTTON) {
                        val pressInteraction = remember { MutableInteractionSource() }
                        BoompalaCardButton(
                            onClick = onArchiveClick,
                            modifier = fullWidthModifier.wearPressFeedback(pressInteraction, hapticEnabled = settings.hapticFeedbackEnabled),
                            interactionSource = pressInteraction,
                            colors = BoompalaButtonDefaults.outlinedButtonColors(),
                        ) {
                            Text(stringResource(R.string.home_feature_archives))
                        }
                    }
                }

                HomeFeature.BROWSE -> {
                    item(key = "browse", contentType = CONTENT_TYPE_BUTTON) {
                        val pressInteraction = remember { MutableInteractionSource() }
                        BoompalaCardButton(
                            onClick = onBrowseClick,
                            modifier = fullWidthModifier.wearPressFeedback(pressInteraction, hapticEnabled = settings.hapticFeedbackEnabled),
                            interactionSource = pressInteraction,
                        ) {
                            Text(stringResource(R.string.home_feature_browse))
                        }
                    }
                }
            }
        }

        // Settings entry is permanent and can never be hidden
        item(key = "settings", contentType = CONTENT_TYPE_OUTLINED_BUTTON) {
            val pressInteraction = remember { MutableInteractionSource() }
            BoompalaCardButton(
                onClick = onSettingsClick,
                modifier = fullWidthModifier.wearPressFeedback(pressInteraction, hapticEnabled = settings.hapticFeedbackEnabled),
                interactionSource = pressInteraction,
                colors = BoompalaButtonDefaults.outlinedButtonColors(),
            ) {
                Text(stringResource(R.string.home_feature_settings))
            }
        }
    }
}
