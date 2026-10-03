package com.boompala.ui

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.wear.compose.foundation.lazy.ScalingLazyListState
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.wear.compose.material3.ButtonColors
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
                        HomeFeatureButton(
                            onClick = onSixYaoClick,
                            text = stringResource(R.string.home_feature_six_yao),
                            modifier = fullWidthModifier,
                            hapticEnabled = settings.hapticFeedbackEnabled,
                        )
                    }
                }

                HomeFeature.MEI_HUA -> {
                    item(key = "mei-hua") {
                        HomeFeatureButton(
                            onClick = onMeiHuaClick,
                            text = stringResource(R.string.home_feature_mei_hua),
                            modifier = fullWidthModifier,
                            hapticEnabled = settings.hapticFeedbackEnabled,
                        )
                    }
                }

                HomeFeature.DESTINY_CHART -> {
                    item(key = "destiny-chart") {
                        HomeFeatureButton(
                            onClick = onDestinyChartClick,
                            text = stringResource(R.string.home_feature_destiny_chart),
                            modifier = fullWidthModifier,
                            hapticEnabled = settings.hapticFeedbackEnabled,
                        )
                    }
                }

                HomeFeature.TAROT_ONE -> {
                    item(key = "tarot") {
                        HomeFeatureButton(
                            onClick = onTarotClick,
                            text = stringResource(R.string.home_feature_tarot_one),
                            modifier = fullWidthModifier,
                            hapticEnabled = settings.hapticFeedbackEnabled,
                        )
                    }
                }

                HomeFeature.TAROT_THREE -> {
                    item(key = "tarot-three") {
                        HomeFeatureButton(
                            onClick = onTarotThreeCardClick,
                            text = stringResource(R.string.home_feature_tarot_three),
                            modifier = fullWidthModifier,
                            hapticEnabled = settings.hapticFeedbackEnabled,
                        )
                    }
                }

                HomeFeature.TAROT_HOLY_TRIANGLE -> {
                    item(key = "tarot-holy-triangle") {
                        HomeFeatureButton(
                            onClick = onTarotHolyTriangleClick,
                            text = stringResource(R.string.home_feature_tarot_holy_triangle),
                            modifier = fullWidthModifier,
                            hapticEnabled = settings.hapticFeedbackEnabled,
                        )
                    }
                }

                HomeFeature.TAROT_CELTIC_CROSS -> {
                    item(key = "tarot-celtic-cross") {
                        HomeFeatureButton(
                            onClick = onTarotCelticCrossClick,
                            text = stringResource(R.string.home_feature_tarot_celtic_cross),
                            modifier = fullWidthModifier,
                            hapticEnabled = settings.hapticFeedbackEnabled,
                        )
                    }
                }

                HomeFeature.DAILY_FORTUNE -> {
                    item(key = "daily-fortune") {
                        HomeFeatureButton(
                            onClick = onDailyFortuneClick,
                            text = stringResource(R.string.home_feature_daily_fortune),
                            modifier = fullWidthModifier,
                            hapticEnabled = settings.hapticFeedbackEnabled,
                        )
                    }
                }

                HomeFeature.XIAO_LIU_REN -> {
                    item(key = "xiaoliuren") {
                        HomeFeatureButton(
                            onClick = onXiaoLiuRenClick,
                            text = stringResource(R.string.home_feature_xiao_liu_ren),
                            modifier = fullWidthModifier,
                            hapticEnabled = settings.hapticFeedbackEnabled,
                        )
                    }
                }

                HomeFeature.COMPASS -> {
                    item(key = "compass") {
                        HomeFeatureButton(
                            onClick = onCompassClick,
                            text = stringResource(R.string.home_feature_compass),
                            modifier = fullWidthModifier,
                            hapticEnabled = settings.hapticFeedbackEnabled,
                        )
                    }
                }

                HomeFeature.PULSE -> {
                    item(key = "pulse") {
                        HomeFeatureButton(
                            onClick = onPulseClick,
                            text = stringResource(R.string.home_feature_pulse),
                            modifier = fullWidthModifier,
                            hapticEnabled = settings.hapticFeedbackEnabled,
                        )
                    }
                }

                HomeFeature.MUYU -> {
                    item(key = "muyu") {
                        HomeFeatureButton(
                            onClick = onMuyuClick,
                            text = stringResource(R.string.home_feature_muyu),
                            modifier = fullWidthModifier,
                            hapticEnabled = settings.hapticFeedbackEnabled,
                        )
                    }
                }

                HomeFeature.ARCHIVES -> {
                    item(key = "archives") {
                        HomeFeatureButton(
                            onClick = onArchiveClick,
                            text = stringResource(R.string.home_feature_archives),
                            modifier = fullWidthModifier,
                            colors = BoompalaButtonDefaults.outlinedButtonColors(),
                            hapticEnabled = settings.hapticFeedbackEnabled,
                        )
                    }
                }

                HomeFeature.BROWSE -> {
                    item(key = "browse") {
                        HomeFeatureButton(
                            onClick = onBrowseClick,
                            text = stringResource(R.string.home_feature_browse),
                            modifier = fullWidthModifier,
                            hapticEnabled = settings.hapticFeedbackEnabled,
                        )
                    }
                }
            }
        }

        // Settings entry is permanent and can never be hidden
        item(key = "settings") {
            HomeFeatureButton(
                onClick = onSettingsClick,
                text = stringResource(R.string.home_feature_settings),
                modifier = fullWidthModifier,
                colors = BoompalaButtonDefaults.outlinedButtonColors(),
                hapticEnabled = settings.hapticFeedbackEnabled,
            )
        }
    }
}

@Composable
private fun HomeFeatureButton(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
    colors: ButtonColors = BoompalaButtonDefaults.buttonColors(),
    hapticEnabled: Boolean = true,
) {
    val pressInteraction = remember { MutableInteractionSource() }
    BoompalaCardButton(
        onClick = onClick,
        modifier = modifier.wearPressFeedback(pressInteraction, hapticEnabled = hapticEnabled),
        interactionSource = pressInteraction,
        colors = colors,
        horizontalArrangement = Arrangement.Start,
    ) {
        Text(
            text = text,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Start,
        )
    }
}
