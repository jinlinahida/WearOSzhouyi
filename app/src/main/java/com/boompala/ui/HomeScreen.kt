package com.boompala.ui

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.wear.compose.foundation.lazy.ScalingLazyListState
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.wear.compose.material3.Text
import com.boompala.R
import com.boompala.settings.AppSettings
import com.boompala.settings.HomeFeature

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
    onTarotCelticCrossClick: () -> Unit = { },
    onPulseClick: () -> Unit = { },
    onMuyuClick: () -> Unit = { },
    state: ScalingLazyListState = rememberScalingLazyListState(),
) {
    val metrics = LocalUiMetrics.current
    val fullWidthModifier = Modifier.fillMaxWidth()
    val visibleFeatures = remember(settings.homeOrder, settings.hiddenHomeFeatures) {
        settings.visibleHomeFeatures()
    }

    ScalingRotaryScrollColumn(
        rotaryEnabled = settings.rotaryScrollingEnabled,
        hapticFeedbackEnabled = settings.hapticFeedbackEnabled,
        animationsEnabled = settings.scalingListEnabled && settings.animationsEnabled,
        modifier = Modifier.fillMaxSize(),
        state = state,
        contentPadding = metrics.screenPadding,
        itemSpacing = metrics.itemSpacing,
    ) {
        visibleFeatures.forEach { feature ->
            when (feature) {
                HomeFeature.SIX_YAO -> {
                    item(key = "six-yao") {
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
                    item(key = "mei-hua") {
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
                    item(key = "destiny-chart") {
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
                    item(key = "tarot") {
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
                    item(key = "tarot-three") {
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
                    item(key = "tarot-holy-triangle") {
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

                HomeFeature.TAROT_CELTIC_CROSS -> {
                    item(key = "tarot-celtic-cross") {
                        val pressInteraction = remember { MutableInteractionSource() }
                        BoompalaCardButton(
                            onClick = onTarotCelticCrossClick,
                            modifier = fullWidthModifier.wearPressFeedback(pressInteraction, hapticEnabled = settings.hapticFeedbackEnabled),
                            interactionSource = pressInteraction,
                        ) {
                            Text(stringResource(R.string.home_feature_tarot_celtic_cross))
                        }
                    }
                }

                HomeFeature.DAILY_FORTUNE -> {
                    item(key = "daily-fortune") {
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
                    item(key = "xiaoliuren") {
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
                    item(key = "compass") {
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
                    item(key = "pulse") {
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

                HomeFeature.MUYU -> {
                    item(key = "muyu") {
                        val pressInteraction = remember { MutableInteractionSource() }
                        BoompalaCardButton(
                            onClick = onMuyuClick,
                            modifier = fullWidthModifier.wearPressFeedback(pressInteraction, hapticEnabled = settings.hapticFeedbackEnabled),
                            interactionSource = pressInteraction,
                        ) {
                            Text(stringResource(R.string.home_feature_muyu))
                        }
                    }
                }

                HomeFeature.ARCHIVES -> {
                    item(key = "archives") {
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
                    item(key = "browse") {
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
        item(key = "settings") {
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
