package com.boompala.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.AlertDialog
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.OutlinedButton
import androidx.wear.compose.material3.Text
import com.boompala.R
import com.boompala.archive.ArchiveRepository
import com.boompala.settings.AppLanguage
import com.boompala.settings.HapticIntensity
import com.boompala.settings.AppSettings
import com.boompala.settings.ContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.text.style.TextOverflow
import androidx.wear.compose.material3.DatePicker
import androidx.wear.compose.material3.Picker
import androidx.wear.compose.material3.rememberPickerState
import com.boompala.settings.HomeFeature
import com.boompala.settings.ScreenMode
import com.boompala.engine.bazi.BaziEngine
import com.boompala.engine.bazi.BaziGender
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class SettingsSection {
    MENU,
    PROFILE,
    APPEARANCE,
    TAROT,
    COMPASS,
    LANGUAGE,
    HOME,
    HAPTICS,
    DATA,
}

private enum class ActivePicker {
    NONE,
    DATE,
    SHICHEN,
}

@Composable
fun SettingsScreen(
    settings: AppSettings,
    onScreenModeSelected: (ScreenMode) -> Unit,
    onContentSizeSelected: (ContentSize) -> Unit,
    onAnimationsEnabledChange: (Boolean) -> Unit,
    onRotaryScrollingEnabledChange: (Boolean) -> Unit,
    onHapticFeedbackEnabledChange: (Boolean) -> Unit = {},
    onHapticIntensityChange: (HapticIntensity) -> Unit = {},
    onLanguageSelected: (AppLanguage) -> Unit = {},
    onMoveHomeFeature: (HomeFeature, Boolean) -> Unit = { _, _ -> },
    onToggleHomeFeatureVisibility: (HomeFeature) -> Unit = {},
    onSaveUserBirth: (birthDate: String, birthHour: Int?, gender: BaziGender) -> Unit = { _, _, _ -> },
    onClearUserBirth: () -> Unit = {},
    onKeepScreenOnEnabledChange: (Boolean) -> Unit = {},
    onCompassTrueNorthEnabledChange: (Boolean) -> Unit = {},
    onCompassDeclinationChange: (Float) -> Unit = {},
    onTarotReversedEnabledChange: (Boolean) -> Unit = {},
    onTarotMajorArcanaOnlyChange: (Boolean) -> Unit = {},
    onResetAllPreferences: () -> Unit = {},
    archiveRepository: ArchiveRepository? = null,
    rotaryScrollingEnabled: Boolean,
    onAboutClick: () -> Unit,
    onBack: () -> Unit,
    // 上报设置内层分区是否可返回，供外层屏蔽滑动返回手势。
    onInnerBackAvailabilityChanged: (Boolean) -> Unit = {},
) {
    val metrics = LocalUiMetrics.current
    val context = LocalContext.current
    // 分区状态需要可保存：从 ABOUT 返回设置时不应重置回主菜单。
    var currentSection by rememberSaveable { mutableStateOf(SettingsSection.MENU) }

    BackHandler(enabled = currentSection != SettingsSection.MENU) {
        currentSection = SettingsSection.MENU
    }

    LaunchedEffect(currentSection) {
        onInnerBackAvailabilityChanged(currentSection != SettingsSection.MENU)
    }

    AnimatedContent(
        targetState = currentSection,
        transitionSpec = {
            val direction = when {
                initialState == SettingsSection.MENU && targetState != SettingsSection.MENU -> NavigationDirection.FORWARD
                initialState != SettingsSection.MENU && targetState == SettingsSection.MENU -> NavigationDirection.BACKWARD
                else -> NavigationDirection.LATERAL
            }
            pageTransitionSpec(direction, settings.animationsEnabled)
        },
        label = "SettingsSectionTransition",
        modifier = Modifier.fillMaxSize(),
    ) { section ->
        when (section) {
        SettingsSection.MENU -> {
            RotaryScrollColumn(
                rotaryEnabled = rotaryScrollingEnabled,
                modifier = Modifier.fillMaxSize(),
                contentPadding = metrics.screenPadding,
                itemSpacing = metrics.itemSpacing,
            ) {
                item(key = "title") {
                    Text(
                        text = stringResource(R.string.settings_title),
                        style = MaterialTheme.typography.titleLarge,
                    )
                }

                item(key = "module-profile") {
                    val baziProfile = settings.resolvedBaziProfile()
                    val subtitle = baziProfile?.shortSummaryZh
                        ?: stringResource(R.string.settings_bazi_not_configured)
                    SettingsModuleButton(
                        iconRes = R.drawable.ic_settings_bazi,
                        title = stringResource(R.string.settings_module_profile),
                        subtitle = subtitle,
                        onClick = { currentSection = SettingsSection.PROFILE },
                        animationsEnabled = settings.animationsEnabled,
                    )
                }

                item(key = "module-appearance") {
                    SettingsModuleButton(
                        iconRes = R.drawable.ic_settings_display,
                        title = stringResource(R.string.settings_module_appearance),
                        subtitle = stringResource(R.string.settings_module_appearance_desc),
                        onClick = { currentSection = SettingsSection.APPEARANCE },
                        animationsEnabled = settings.animationsEnabled,
                    )
                }

                item(key = "module-tarot") {
                    SettingsModuleButton(
                        iconRes = R.drawable.ic_settings_tarot,
                        title = stringResource(R.string.settings_module_tarot),
                        subtitle = stringResource(R.string.settings_module_tarot_desc),
                        onClick = { currentSection = SettingsSection.TAROT },
                        animationsEnabled = settings.animationsEnabled,
                    )
                }

                item(key = "module-compass") {
                    SettingsModuleButton(
                        iconRes = R.drawable.ic_settings_compass,
                        title = stringResource(R.string.settings_module_compass),
                        subtitle = stringResource(R.string.settings_module_compass_desc),
                        onClick = { currentSection = SettingsSection.COMPASS },
                        animationsEnabled = settings.animationsEnabled,
                    )
                }

                item(key = "module-language") {
                    SettingsModuleButton(
                        iconRes = R.drawable.ic_settings_language,
                        title = stringResource(R.string.settings_module_language),
                        subtitle = if (settings.language == AppLanguage.CHINESE) "简体中文" else "English",
                        onClick = { currentSection = SettingsSection.LANGUAGE },
                        animationsEnabled = settings.animationsEnabled,
                    )
                }

                item(key = "module-home") {
                    SettingsModuleButton(
                        iconRes = R.drawable.ic_settings_home,
                        title = stringResource(R.string.settings_module_home),
                        subtitle = stringResource(R.string.settings_module_home_desc),
                        onClick = { currentSection = SettingsSection.HOME },
                        animationsEnabled = settings.animationsEnabled,
                    )
                }

                item(key = "module-haptics") {
                    SettingsModuleButton(
                        iconRes = R.drawable.ic_settings_haptics,
                        title = stringResource(R.string.settings_module_haptics),
                        subtitle = stringResource(R.string.settings_module_haptics_desc),
                        onClick = { currentSection = SettingsSection.HAPTICS },
                        animationsEnabled = settings.animationsEnabled,
                    )
                }

                item(key = "module-data") {
                    SettingsModuleButton(
                        iconRes = R.drawable.ic_settings_data,
                        title = stringResource(R.string.settings_module_data),
                        subtitle = stringResource(R.string.settings_module_data_desc),
                        onClick = { currentSection = SettingsSection.DATA },
                        animationsEnabled = settings.animationsEnabled,
                    )
                }

                item(key = "module-about") {
                    SettingsModuleButton(
                        iconRes = R.drawable.ic_settings_about,
                        title = stringResource(R.string.settings_module_about),
                        subtitle = stringResource(R.string.settings_module_about_desc),
                        onClick = onAboutClick,
                        animationsEnabled = settings.animationsEnabled,
                    )
                }

                item(key = "back-home") {
                    val backInteraction = remember { MutableInteractionSource() }
                    BoompalaCardButton(
                        onClick = onBack,
                        modifier = Modifier
                            .fillMaxWidth()
                            .wearPressFeedback(backInteraction),
                        interactionSource = backInteraction,
                        colors = BoompalaButtonDefaults.outlinedButtonColors(),
                    ) {
                        Text(stringResource(R.string.action_back_home))
                    }
                }
            }
        }

        SettingsSection.APPEARANCE -> {
            RotaryScrollColumn(
                rotaryEnabled = rotaryScrollingEnabled,
                modifier = Modifier.fillMaxSize(),
                contentPadding = metrics.screenPadding,
                itemSpacing = metrics.itemSpacing,
            ) {
                item(key = "appearance-title") {
                    Text(
                        text = stringResource(R.string.settings_module_appearance),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }

                item(key = "screen-mode-title") {
                    Text(
                        text = stringResource(R.string.settings_screen_mode),
                        style = MaterialTheme.typography.titleSmall,
                    )
                }
                ScreenMode.entries.forEach { mode ->
                    item(key = "screen-mode-${mode.name}") {
                        val modeLabel = when (mode) {
                            ScreenMode.AUTO -> stringResource(R.string.settings_screen_mode_auto)
                            ScreenMode.ROUND -> stringResource(R.string.settings_screen_mode_round)
                            ScreenMode.SQUARE -> stringResource(R.string.settings_screen_mode_square)
                        }
                        SelectionButton(
                            selected = settings.screenMode == mode,
                            text = modeLabel,
                            onClick = { onScreenModeSelected(mode) },
                        )
                    }
                }

                item(key = "content-size-title") {
                    Text(
                        text = stringResource(R.string.settings_content_size),
                        style = MaterialTheme.typography.titleSmall,
                    )
                }
                ContentSize.entries.forEach { size ->
                    item(key = "content-size-${size.name}") {
                        val sizeLabel = when (size) {
                            ContentSize.SMALL -> stringResource(R.string.settings_content_size_small)
                            ContentSize.STANDARD -> stringResource(R.string.settings_content_size_standard)
                            ContentSize.LARGE -> stringResource(R.string.settings_content_size_large)
                        }
                        SelectionButton(
                            selected = settings.contentSize == size,
                            text = sizeLabel,
                            onClick = { onContentSizeSelected(size) },
                        )
                    }
                }

                item(key = "animation-title") {
                    Text(
                        text = stringResource(R.string.settings_animations),
                        style = MaterialTheme.typography.titleSmall,
                    )
                }
                item(key = "animation-toggle") {
                    SelectionButton(
                        selected = settings.animationsEnabled,
                        text = if (settings.animationsEnabled) stringResource(R.string.action_enabled) else stringResource(R.string.action_disabled),
                        onClick = { onAnimationsEnabledChange(!settings.animationsEnabled) },
                    )
                }

                item(key = "keep-screen-on-title") {
                    Text(
                        text = stringResource(R.string.settings_keep_screen_on),
                        style = MaterialTheme.typography.titleSmall,
                    )
                }
                item(key = "keep-screen-on-toggle") {
                    SelectionButton(
                        selected = settings.keepScreenOnEnabled,
                        text = if (settings.keepScreenOnEnabled) stringResource(R.string.action_enabled) else stringResource(R.string.action_disabled),
                        onClick = { onKeepScreenOnEnabledChange(!settings.keepScreenOnEnabled) },
                    )
                }

                item(key = "appearance-back") {
                    val backInteraction = remember { MutableInteractionSource() }
                    BoompalaCardButton(
                        onClick = { currentSection = SettingsSection.MENU },
                        modifier = Modifier
                            .fillMaxWidth()
                            .wearPressFeedback(backInteraction),
                        interactionSource = backInteraction,
                        colors = BoompalaButtonDefaults.outlinedButtonColors(),
                    ) {
                        Text(stringResource(R.string.action_back))
                    }
                }
            }
        }

        SettingsSection.TAROT -> {
            RotaryScrollColumn(
                rotaryEnabled = rotaryScrollingEnabled,
                modifier = Modifier.fillMaxSize(),
                contentPadding = metrics.screenPadding,
                itemSpacing = metrics.itemSpacing,
            ) {
                item(key = "tarot-title") {
                    Text(
                        text = stringResource(R.string.settings_module_tarot),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }

                item(key = "tarot-reversed-title") {
                    Text(
                        text = stringResource(R.string.settings_tarot_reversed),
                        style = MaterialTheme.typography.titleSmall,
                    )
                }
                item(key = "tarot-reversed-allow") {
                    SelectionButton(
                        selected = settings.tarotReversedEnabled,
                        text = stringResource(R.string.settings_tarot_reversed_allow),
                        onClick = { onTarotReversedEnabledChange(true) },
                    )
                }
                item(key = "tarot-reversed-disallow") {
                    SelectionButton(
                        selected = !settings.tarotReversedEnabled,
                        text = stringResource(R.string.settings_tarot_reversed_disallow),
                        onClick = { onTarotReversedEnabledChange(false) },
                    )
                }

                item(key = "tarot-deck-title") {
                    Text(
                        text = stringResource(R.string.settings_tarot_deck),
                        style = MaterialTheme.typography.titleSmall,
                    )
                }
                item(key = "tarot-deck-full") {
                    SelectionButton(
                        selected = !settings.tarotMajorArcanaOnly,
                        text = stringResource(R.string.settings_tarot_deck_full),
                        onClick = { onTarotMajorArcanaOnlyChange(false) },
                    )
                }
                item(key = "tarot-deck-major") {
                    SelectionButton(
                        selected = settings.tarotMajorArcanaOnly,
                        text = stringResource(R.string.settings_tarot_deck_major),
                        onClick = { onTarotMajorArcanaOnlyChange(true) },
                    )
                }

                item(key = "tarot-back") {
                    val backInteraction = remember { MutableInteractionSource() }
                    BoompalaCardButton(
                        onClick = { currentSection = SettingsSection.MENU },
                        modifier = Modifier
                            .fillMaxWidth()
                            .wearPressFeedback(backInteraction),
                        interactionSource = backInteraction,
                        colors = BoompalaButtonDefaults.outlinedButtonColors(),
                    ) {
                        Text(stringResource(R.string.action_back))
                    }
                }
            }
        }

        SettingsSection.COMPASS -> {
            RotaryScrollColumn(
                rotaryEnabled = rotaryScrollingEnabled,
                modifier = Modifier.fillMaxSize(),
                contentPadding = metrics.screenPadding,
                itemSpacing = metrics.itemSpacing,
            ) {
                item(key = "compass-title") {
                    Text(
                        text = stringResource(R.string.settings_module_compass),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }

                item(key = "compass-north-title") {
                    Text(
                        text = stringResource(R.string.settings_compass_north),
                        style = MaterialTheme.typography.titleSmall,
                    )
                }
                item(key = "compass-magnetic-north") {
                    SelectionButton(
                        selected = !settings.compassTrueNorthEnabled,
                        text = stringResource(R.string.settings_compass_magnetic_north),
                        onClick = { onCompassTrueNorthEnabledChange(false) },
                    )
                }
                item(key = "compass-true-north") {
                    SelectionButton(
                        selected = settings.compassTrueNorthEnabled,
                        text = stringResource(R.string.settings_compass_true_north),
                        onClick = { onCompassTrueNorthEnabledChange(true) },
                    )
                }

                if (settings.compassTrueNorthEnabled) {
                    item(key = "compass-declination-title") {
                        Text(
                            text = stringResource(R.string.settings_compass_declination),
                            style = MaterialTheme.typography.titleSmall,
                        )
                    }
                    item(key = "compass-declination-desc") {
                        Text(
                            text = stringResource(R.string.settings_compass_declination_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    item(key = "compass-declination-adjust") {
                        ResultCard {
                            val declinationInt = settings.compassDeclination.toInt()
                            val declinationText = if (declinationInt > 0) "+$declinationInt°" else "$declinationInt°"
                            Text(
                                text = "偏角校准：$declinationText",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Spacer(Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(metrics.itemSpacing),
                            ) {
                                val minusInter = remember { MutableInteractionSource() }
                                BoompalaCardButton(
                                    onClick = {
                                        val newDec = (settings.compassDeclination - 1f).coerceIn(-15f, 15f)
                                        onCompassDeclinationChange(newDec)
                                    },
                                    modifier = Modifier.weight(1f).wearPressFeedback(minusInter),
                                    interactionSource = minusInter,
                                    colors = BoompalaButtonDefaults.outlinedButtonColors(),
                                ) {
                                    Text("-1°")
                                }
                                val resetInter = remember { MutableInteractionSource() }
                                BoompalaCardButton(
                                    onClick = { onCompassDeclinationChange(0f) },
                                    modifier = Modifier.weight(1f).wearPressFeedback(resetInter),
                                    interactionSource = resetInter,
                                    colors = BoompalaButtonDefaults.outlinedButtonColors(),
                                ) {
                                    Text("0°")
                                }
                                val plusInter = remember { MutableInteractionSource() }
                                BoompalaCardButton(
                                    onClick = {
                                        val newDec = (settings.compassDeclination + 1f).coerceIn(-15f, 15f)
                                        onCompassDeclinationChange(newDec)
                                    },
                                    modifier = Modifier.weight(1f).wearPressFeedback(plusInter),
                                    interactionSource = plusInter,
                                    colors = BoompalaButtonDefaults.outlinedButtonColors(),
                                ) {
                                    Text("+1°")
                                }
                            }
                        }
                    }
                }

                item(key = "compass-back") {
                    val backInteraction = remember { MutableInteractionSource() }
                    BoompalaCardButton(
                        onClick = { currentSection = SettingsSection.MENU },
                        modifier = Modifier
                            .fillMaxWidth()
                            .wearPressFeedback(backInteraction),
                        interactionSource = backInteraction,
                        colors = BoompalaButtonDefaults.outlinedButtonColors(),
                    ) {
                        Text(stringResource(R.string.action_back))
                    }
                }
            }
        }

        SettingsSection.LANGUAGE -> {
            RotaryScrollColumn(
                rotaryEnabled = rotaryScrollingEnabled,
                modifier = Modifier.fillMaxSize(),
                contentPadding = metrics.screenPadding,
                itemSpacing = metrics.itemSpacing,
            ) {
                item(key = "language-title") {
                    Text(
                        text = stringResource(R.string.settings_module_language),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }

                AppLanguage.entries.forEach { lang ->
                    item(key = "lang-${lang.name}") {
                        SelectionButton(
                            selected = settings.language == lang,
                            text = "${lang.displayName} (${lang.englishName})",
                            onClick = { onLanguageSelected(lang) },
                        )
                    }
                }

                item(key = "language-back") {
                    val backInteraction = remember { MutableInteractionSource() }
                    BoompalaCardButton(
                        onClick = { currentSection = SettingsSection.MENU },
                        modifier = Modifier
                            .fillMaxWidth()
                            .wearPressFeedback(backInteraction),
                        interactionSource = backInteraction,
                        colors = BoompalaButtonDefaults.outlinedButtonColors(),
                    ) {
                        Text(stringResource(R.string.action_back))
                    }
                }
            }
        }

        SettingsSection.HOME -> {
            val fullOrder = settings.effectiveHomeOrder()
            RotaryScrollColumn(
                rotaryEnabled = rotaryScrollingEnabled,
                modifier = Modifier.fillMaxSize(),
                contentPadding = metrics.screenPadding,
                itemSpacing = metrics.itemSpacing,
            ) {
                item(key = "home-manage-title") {
                    Text(
                        text = stringResource(R.string.settings_module_home),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }

                item(key = "home-fixed-note") {
                    ResultCard {
                        Text(
                            text = stringResource(R.string.settings_home_fixed_hint),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                itemsIndexed(fullOrder, key = { _, feature -> "home-feat-${feature.id}" }) { index, feature ->
                    val isHidden = settings.hiddenHomeFeatures.contains(feature)
                    val featureName = when (feature) {
                        HomeFeature.SIX_YAO -> stringResource(R.string.home_feature_six_yao)
                        HomeFeature.MEI_HUA -> stringResource(R.string.home_feature_mei_hua)
                        HomeFeature.DESTINY_CHART -> stringResource(R.string.home_feature_destiny_chart)
                        HomeFeature.TAROT_ONE -> stringResource(R.string.home_feature_tarot_one)
                        HomeFeature.TAROT_THREE -> stringResource(R.string.home_feature_tarot_three)
                        HomeFeature.TAROT_HOLY_TRIANGLE -> stringResource(R.string.home_feature_tarot_holy_triangle)
                        HomeFeature.TAROT_CELTIC_CROSS -> stringResource(R.string.home_feature_tarot_celtic_cross)
                        HomeFeature.DAILY_FORTUNE -> stringResource(R.string.home_feature_daily_fortune)
                        HomeFeature.XIAO_LIU_REN -> stringResource(R.string.home_feature_xiao_liu_ren)
                        HomeFeature.COMPASS -> stringResource(R.string.home_feature_compass)
                        HomeFeature.PULSE -> stringResource(R.string.home_feature_pulse)
                        HomeFeature.MUYU -> stringResource(R.string.home_feature_muyu)
                        HomeFeature.ARCHIVES -> stringResource(R.string.home_feature_archives)
                        HomeFeature.BROWSE -> stringResource(R.string.home_feature_browse)
                    }

                    ResultCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = featureName,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isHidden) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier
                                    .weight(1f)
                                    .wearMarquee(settings.animationsEnabled),
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = if (isHidden) stringResource(R.string.action_hide) else stringResource(R.string.action_show),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isHidden) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            val toggleInteraction = remember { MutableInteractionSource() }
                            val upInteraction = remember { MutableInteractionSource() }
                            val downInteraction = remember { MutableInteractionSource() }
                            BoompalaCardButton(
                                onClick = { onToggleHomeFeatureVisibility(feature) },
                                modifier = Modifier
                                    .weight(1f)
                                    .wearPressFeedback(toggleInteraction),
                                interactionSource = toggleInteraction,
                                colors = BoompalaButtonDefaults.outlinedButtonColors(),
                            ) {
                                Text(if (isHidden) stringResource(R.string.action_show) else stringResource(R.string.action_hide))
                            }
                            BoompalaCardButton(
                                onClick = { onMoveHomeFeature(feature, true) },
                                enabled = index > 0,
                                modifier = Modifier
                                    .weight(0.7f)
                                    .wearPressFeedback(upInteraction, enabled = index > 0),
                                interactionSource = upInteraction,
                                colors = BoompalaButtonDefaults.outlinedButtonColors(),
                            ) {
                                Text("▲")
                            }
                            BoompalaCardButton(
                                onClick = { onMoveHomeFeature(feature, false) },
                                enabled = index < fullOrder.size - 1,
                                modifier = Modifier
                                    .weight(0.7f)
                                    .wearPressFeedback(downInteraction, enabled = index < fullOrder.size - 1),
                                interactionSource = downInteraction,
                                colors = BoompalaButtonDefaults.outlinedButtonColors(),
                            ) {
                                Text("▼")
                            }
                        }
                    }
                }

                item(key = "home-manage-back") {
                    val backInteraction = remember { MutableInteractionSource() }
                    BoompalaCardButton(
                        onClick = { currentSection = SettingsSection.MENU },
                        modifier = Modifier
                            .fillMaxWidth()
                            .wearPressFeedback(backInteraction),
                        interactionSource = backInteraction,
                        colors = BoompalaButtonDefaults.outlinedButtonColors(),
                    ) {
                        Text(stringResource(R.string.action_back))
                    }
                }
            }
        }

        SettingsSection.HAPTICS -> {
            RotaryScrollColumn(
                rotaryEnabled = rotaryScrollingEnabled,
                modifier = Modifier.fillMaxSize(),
                contentPadding = metrics.screenPadding,
                itemSpacing = metrics.itemSpacing,
            ) {
                item(key = "haptics-title") {
                    Text(
                        text = stringResource(R.string.settings_module_haptics),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }

                item(key = "rotary-title") {
                    Text(
                        text = stringResource(R.string.settings_rotary),
                        style = MaterialTheme.typography.titleSmall,
                    )
                }
                item(key = "rotary-toggle") {
                    SelectionButton(
                        selected = settings.rotaryScrollingEnabled,
                        text = if (settings.rotaryScrollingEnabled) stringResource(R.string.action_enabled) else stringResource(R.string.action_disabled),
                        onClick = {
                            onRotaryScrollingEnabledChange(!settings.rotaryScrollingEnabled)
                        },
                    )
                }

                item(key = "haptic-title") {
                    Text(
                        text = stringResource(R.string.settings_haptic),
                        style = MaterialTheme.typography.titleSmall,
                    )
                }
                item(key = "haptic-toggle") {
                    SelectionButton(
                        selected = settings.hapticFeedbackEnabled,
                        text = if (settings.hapticFeedbackEnabled) stringResource(R.string.action_enabled) else stringResource(R.string.action_disabled),
                        onClick = {
                            onHapticFeedbackEnabledChange(!settings.hapticFeedbackEnabled)
                        },
                    )
                }

                if (settings.hapticFeedbackEnabled) {
                    item(key = "haptic-intensity-title") {
                        Text(
                            text = stringResource(R.string.settings_haptic_intensity),
                            style = MaterialTheme.typography.titleSmall,
                        )
                    }
                    HapticIntensity.entries.forEach { intensity ->
                        item(key = "haptic-intensity-${intensity.name}") {
                            val label = when (intensity) {
                                HapticIntensity.LIGHT -> stringResource(R.string.settings_haptic_intensity_light)
                                HapticIntensity.STANDARD -> stringResource(R.string.settings_haptic_intensity_standard)
                                HapticIntensity.STRONG -> stringResource(R.string.settings_haptic_intensity_strong)
                            }
                            SelectionButton(
                                selected = settings.hapticIntensity == intensity,
                                text = label,
                                targetIntensity = intensity,
                                onClick = {
                                    onHapticIntensityChange(intensity)
                                },
                            )
                        }
                    }
                }

                item(key = "haptics-back") {
                    val backInteraction = remember { MutableInteractionSource() }
                    BoompalaCardButton(
                        onClick = { currentSection = SettingsSection.MENU },
                        modifier = Modifier
                            .fillMaxWidth()
                            .wearPressFeedback(backInteraction),
                        interactionSource = backInteraction,
                        colors = BoompalaButtonDefaults.outlinedButtonColors(),
                    ) {
                        Text(stringResource(R.string.action_back))
                    }
                }
            }
        }

        SettingsSection.PROFILE -> {
            var isEditing by rememberSaveable(settings.userBirthDate) {
                mutableStateOf(!settings.isBaziConfigured)
            }
            var showClearBaziDialog by remember { mutableStateOf(false) }
            var activePicker by remember { mutableStateOf(ActivePicker.NONE) }

            val initialDate = remember(settings.userBirthDate) {
                settings.userBirthDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
                    ?: LocalDate.of(1995, 6, 15)
            }

            var selectedGender by rememberSaveable { mutableStateOf(settings.userGender) }
            var selectedDate by rememberSaveable { mutableStateOf(initialDate) }
            var selectedShichenIndex by rememberSaveable { mutableIntStateOf(hourToShichenIndex(settings.userBirthHour)) }

            BackHandler(enabled = activePicker != ActivePicker.NONE) {
                activePicker = ActivePicker.NONE
            }

            val livePreviewProfile = remember(
                selectedDate,
                selectedShichenIndex,
                selectedGender,
            ) {
                runCatching {
                    BaziEngine.calculate(
                        birthDate = selectedDate,
                        birthHour = shichenIndexToHour(selectedShichenIndex),
                        gender = selectedGender,
                    )
                }.getOrNull()
            }

            when (activePicker) {
                ActivePicker.DATE -> {
                    DatePicker(
                        initialDate = selectedDate,
                        onDatePicked = { pickedDate ->
                            selectedDate = pickedDate
                            activePicker = ActivePicker.NONE
                        },
                        minValidDate = LocalDate.of(1920, 1, 1),
                        maxValidDate = LocalDate.now(),
                    )
                }

                ActivePicker.SHICHEN -> {
                    ShichenPicker(
                        initialIndex = selectedShichenIndex,
                        onShichenPicked = { pickedIndex ->
                            selectedShichenIndex = pickedIndex
                            activePicker = ActivePicker.NONE
                        },
                        onDismiss = { activePicker = ActivePicker.NONE },
                    )
                }

                ActivePicker.NONE -> {
                    RotaryScrollColumn(
                        rotaryEnabled = rotaryScrollingEnabled,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = metrics.screenPadding,
                        itemSpacing = metrics.itemSpacing,
                    ) {
                        if (!isEditing && settings.isBaziConfigured) {
                            val profile = settings.resolvedBaziProfile()
                            if (profile != null) {
                                item(key = "bazi-view-title") {
                                    Text(
                                        text = stringResource(R.string.settings_module_profile),
                                        style = MaterialTheme.typography.titleMedium,
                                    )
                                }

                                item(key = "bazi-overview-card") {
                                    ResultCard {
                                        Text(
                                            text = profile.shortSummaryZh,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            maxLines = 1,
                                            softWrap = false,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.wearMarquee(settings.animationsEnabled),
                                        )
                                        DetailField(
                                            label = stringResource(R.string.settings_bazi_gender),
                                            value = profile.gender.titleZh,
                                        )
                                        DetailField(
                                            label = "生肖属相",
                                            value = profile.shengXiao,
                                        )
                                        DetailField(
                                            label = stringResource(R.string.settings_bazi_birth_date),
                                            value = buildString {
                                                append(profile.birthDate.toString())
                                                append(" ")
                                                if (profile.birthHour != null) {
                                                    append(SHICHEN_LABELS[hourToShichenIndex(profile.birthHour)])
                                                } else {
                                                    append(stringResource(R.string.settings_bazi_hour_unknown))
                                                }
                                            },
                                            marquee = true,
                                            animationsEnabled = settings.animationsEnabled,
                                        )
                                    }
                                }

                                item(key = "bazi-pillars-card") {
                                    ResultCard {
                                        Text(
                                            text = stringResource(R.string.settings_bazi_preview_title),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                        )
                                        DetailField(
                                            label = "${stringResource(R.string.settings_bazi_pillar_year)} · ${profile.yearPillar.stemShiShen}",
                                            value = "${profile.yearPillar.ganzhi.displayName} · ${profile.yearPillar.naYin}",
                                            marquee = true,
                                            animationsEnabled = settings.animationsEnabled,
                                        )
                                        DetailField(
                                            label = "${stringResource(R.string.settings_bazi_pillar_month)} · ${profile.monthPillar.stemShiShen}",
                                            value = "${profile.monthPillar.ganzhi.displayName} · ${profile.monthPillar.naYin}",
                                            marquee = true,
                                            animationsEnabled = settings.animationsEnabled,
                                        )
                                        DetailField(
                                            label = "${stringResource(R.string.settings_bazi_pillar_day)} · 日主",
                                            value = "${profile.dayPillar.ganzhi.displayName} · ${profile.dayPillar.naYin}",
                                            marquee = true,
                                            animationsEnabled = settings.animationsEnabled,
                                        )
                                        val hourPillar = profile.hourPillar
                                        if (hourPillar != null) {
                                            DetailField(
                                                label = "${stringResource(R.string.settings_bazi_pillar_hour)} · ${hourPillar.stemShiShen}",
                                                value = "${hourPillar.ganzhi.displayName} · ${hourPillar.naYin}",
                                                marquee = true,
                                                animationsEnabled = settings.animationsEnabled,
                                            )
                                        } else {
                                            DetailField(
                                                label = stringResource(R.string.settings_bazi_pillar_hour),
                                                value = stringResource(R.string.settings_bazi_hour_unknown),
                                            )
                                        }
                                    }
                                }

                                item(key = "bazi-action-edit") {
                                    val editInteraction = remember { MutableInteractionSource() }
                                    BoompalaCardButton(
                                        onClick = {
                                            selectedGender = settings.userGender
                                            selectedDate = profile.birthDate
                                            selectedShichenIndex = hourToShichenIndex(profile.birthHour)
                                            isEditing = true
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .wearPressFeedback(editInteraction),
                                        interactionSource = editInteraction,
                                        colors = BoompalaButtonDefaults.buttonColors(),
                                    ) {
                                        Text(
                                            text = stringResource(R.string.settings_bazi_edit),
                                            maxLines = 1,
                                        )
                                    }
                                }

                                item(key = "bazi-action-clear") {
                                    val clearInteraction = remember { MutableInteractionSource() }
                                    BoompalaCardButton(
                                        onClick = { showClearBaziDialog = true },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .wearPressFeedback(clearInteraction),
                                        interactionSource = clearInteraction,
                                        colors = BoompalaButtonDefaults.outlinedButtonColors(),
                                    ) {
                                        Text(
                                            text = stringResource(R.string.settings_bazi_clear),
                                            color = MaterialTheme.colorScheme.error,
                                            maxLines = 1,
                                        )
                                    }
                                }
                            }
                        } else {
                            item(key = "bazi-edit-title") {
                                Text(
                                    text = stringResource(R.string.settings_module_profile),
                                    style = MaterialTheme.typography.titleMedium,
                                )
                            }

                            item(key = "bazi-gender-selector") {
                                ResultCard {
                                    Text(
                                        text = stringResource(R.string.settings_bazi_gender),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    ) {
                                        val maleSelected = selectedGender == BaziGender.MALE
                                        val mInter = remember { MutableInteractionSource() }
                                        SelectableCardButton(
                                            selected = maleSelected,
                                            onClick = { selectedGender = BaziGender.MALE },
                                            contentPadding = BoompalaButtonDefaults.compactContentPadding,
                                            modifier = Modifier
                                                .weight(1f)
                                                .wearPressFeedback(mInter),
                                            interactionSource = mInter,
                                        ) {
                                            Text(
                                                text = if (maleSelected) "✓ 乾造" else "乾造",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = if (maleSelected) FontWeight.Bold else FontWeight.Normal,
                                                maxLines = 1,
                                                softWrap = false,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                        }
                                        val femaleSelected = selectedGender == BaziGender.FEMALE
                                        val fInter = remember { MutableInteractionSource() }
                                        SelectableCardButton(
                                            selected = femaleSelected,
                                            onClick = { selectedGender = BaziGender.FEMALE },
                                            contentPadding = BoompalaButtonDefaults.compactContentPadding,
                                            modifier = Modifier
                                                .weight(1f)
                                                .wearPressFeedback(fInter),
                                            interactionSource = fInter,
                                        ) {
                                            Text(
                                                text = if (femaleSelected) "✓ 坤造" else "坤造",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = if (femaleSelected) FontWeight.Bold else FontWeight.Normal,
                                                maxLines = 1,
                                                softWrap = false,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                        }
                                    }
                                }
                            }

                            item(key = "bazi-pick-date-card") {
                                val dateInter = remember { MutableInteractionSource() }
                                BoompalaCardButton(
                                    onClick = { activePicker = ActivePicker.DATE },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .wearPressFeedback(dateInter),
                                    interactionSource = dateInter,
                                    colors = BoompalaButtonDefaults.outlinedButtonColors(),
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalAlignment = Alignment.Start,
                                        verticalArrangement = Arrangement.spacedBy(2.dp),
                                    ) {
                                        Text(
                                            text = stringResource(R.string.settings_bazi_birth_date),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            softWrap = false,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        Text(
                                            text = "${selectedDate.year}年${selectedDate.monthValue}月${selectedDate.dayOfMonth}日",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            maxLines = 1,
                                            softWrap = false,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                }
                            }

                            item(key = "bazi-pick-hour-card") {
                                val hourInter = remember { MutableInteractionSource() }
                                BoompalaCardButton(
                                    onClick = { activePicker = ActivePicker.SHICHEN },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .wearPressFeedback(hourInter),
                                    interactionSource = hourInter,
                                    colors = BoompalaButtonDefaults.outlinedButtonColors(),
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalAlignment = Alignment.Start,
                                        verticalArrangement = Arrangement.spacedBy(2.dp),
                                    ) {
                                        Text(
                                            text = stringResource(R.string.settings_bazi_birth_hour),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            softWrap = false,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        Text(
                                            text = SHICHEN_LABELS[selectedShichenIndex],
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            maxLines = 1,
                                            softWrap = false,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                }
                            }

                            if (livePreviewProfile != null) {
                                item(key = "bazi-live-preview") {
                                    ResultCard {
                                        Text(
                                            text = "实时推算命盘",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                        Text(
                                            text = livePreviewProfile.fourPillarsText,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            softWrap = false,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.wearMarquee(settings.animationsEnabled),
                                        )
                                        Text(
                                            text = "${selectedGender.titleZh} · ${livePreviewProfile.dayMaster.displayName}${livePreviewProfile.dayMasterElement.displayName}日主 · 属${livePreviewProfile.shengXiao}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            softWrap = false,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.wearMarquee(settings.animationsEnabled),
                                        )
                                    }
                                }
                            }

                            item(key = "bazi-save-button") {
                                val saveInteraction = remember { MutableInteractionSource() }
                                BoompalaCardButton(
                                    onClick = {
                                        val dateStr = String.format(
                                            java.util.Locale.US,
                                            "%04d-%02d-%02d",
                                            selectedDate.year,
                                            selectedDate.monthValue,
                                            selectedDate.dayOfMonth,
                                        )
                                        val hour = shichenIndexToHour(selectedShichenIndex)
                                        onSaveUserBirth(dateStr, hour, selectedGender)
                                        isEditing = false
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .wearPressFeedback(saveInteraction),
                                    interactionSource = saveInteraction,
                                    colors = BoompalaButtonDefaults.buttonColors(),
                                ) {
                                    Text(
                                        text = stringResource(R.string.settings_bazi_save),
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                    )
                                }
                            }

                            if (settings.isBaziConfigured) {
                                item(key = "bazi-cancel-edit-button") {
                                    val cancelInteraction = remember { MutableInteractionSource() }
                                    BoompalaCardButton(
                                        onClick = { isEditing = false },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .wearPressFeedback(cancelInteraction),
                                        interactionSource = cancelInteraction,
                                        colors = BoompalaButtonDefaults.outlinedButtonColors(),
                                    ) {
                                        Text(
                                            text = stringResource(R.string.action_cancel),
                                            maxLines = 1,
                                        )
                                    }
                                }
                            }
                        }

                        item(key = "bazi-back-btn") {
                            val backInteraction = remember { MutableInteractionSource() }
                            BoompalaCardButton(
                                onClick = { currentSection = SettingsSection.MENU },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .wearPressFeedback(backInteraction),
                                interactionSource = backInteraction,
                                colors = BoompalaButtonDefaults.outlinedButtonColors(),
                            ) {
                                Text(
                                    text = stringResource(R.string.action_back),
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                }
            }

            if (showClearBaziDialog) {
                AlertDialog(
                    visible = true,
                    onDismissRequest = { showClearBaziDialog = false },
                    title = {
                        Text(
                            stringResource(R.string.settings_bazi_clear_confirm_title),
                            style = MaterialTheme.typography.titleMedium,
                        )
                    },
                    text = {
                        Text(
                            stringResource(R.string.settings_bazi_clear_confirm_desc),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    },
                    confirmButton = {
                        val confirmInteraction = remember { MutableInteractionSource() }
                        BoompalaCardButton(
                            onClick = {
                                onClearUserBirth()
                                showClearBaziDialog = false
                                isEditing = true
                            },
                            modifier = Modifier.wearPressFeedback(confirmInteraction),
                            interactionSource = confirmInteraction,
                            colors = BoompalaButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.25f),
                            ),
                        ) {
                            Text(
                                stringResource(R.string.action_delete),
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    },
                    dismissButton = {
                        val dismissInteraction = remember { MutableInteractionSource() }
                        BoompalaCardButton(
                            onClick = { showClearBaziDialog = false },
                            modifier = Modifier.wearPressFeedback(dismissInteraction),
                            interactionSource = dismissInteraction,
                            colors = BoompalaButtonDefaults.outlinedButtonColors(),
                        ) {
                            Text(stringResource(R.string.action_cancel))
                        }
                    },
                )
            }
        }

        SettingsSection.DATA -> {
            val scope = rememberCoroutineScope()
            var archiveCount by remember { mutableIntStateOf(0) }
            var showClearDialog by remember { mutableStateOf(false) }
            var showResetAllDialog by remember { mutableStateOf(false) }

            LaunchedEffect(archiveRepository) {
                if (archiveRepository != null) {
                    val count = withContext(Dispatchers.IO) {
                        archiveRepository.list(null, null).size
                    }
                    archiveCount = count
                }
            }

            RotaryScrollColumn(
                rotaryEnabled = rotaryScrollingEnabled,
                modifier = Modifier.fillMaxSize(),
                contentPadding = metrics.screenPadding,
                itemSpacing = metrics.itemSpacing,
            ) {
                item(key = "data-title") {
                    Text(
                        text = stringResource(R.string.settings_module_data),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }

                item(key = "data-stats") {
                    ResultCard {
                        Text(
                            text = stringResource(R.string.settings_data_total, archiveCount),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }

                if (archiveCount > 0) {
                    item(key = "data-clear") {
                        val pressInteraction = remember { MutableInteractionSource() }
                        BoompalaCardButton(
                            onClick = { showClearDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .wearPressFeedback(pressInteraction),
                            interactionSource = pressInteraction,
                            colors = BoompalaButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        ) {
                            Text(
                                text = stringResource(R.string.settings_data_clear_all),
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }

                item(key = "data-reset-all") {
                    val pressInteraction = remember { MutableInteractionSource() }
                    BoompalaCardButton(
                        onClick = { showResetAllDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .wearPressFeedback(pressInteraction),
                        interactionSource = pressInteraction,
                        colors = BoompalaButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    ) {
                        Text(
                            text = stringResource(R.string.settings_reset_all),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }

                item(key = "data-back") {
                    val backInteraction = remember { MutableInteractionSource() }
                    BoompalaCardButton(
                        onClick = { currentSection = SettingsSection.MENU },
                        modifier = Modifier
                            .fillMaxWidth()
                            .wearPressFeedback(backInteraction),
                        interactionSource = backInteraction,
                        colors = BoompalaButtonDefaults.outlinedButtonColors(),
                    ) {
                        Text(stringResource(R.string.action_back))
                    }
                }
            }

            if (showClearDialog && archiveRepository != null) {
                AlertDialog(
                    visible = true,
                    onDismissRequest = { showClearDialog = false },
                    title = { Text(stringResource(R.string.settings_data_clear_confirm_title)) },
                    text = { Text(stringResource(R.string.settings_data_clear_confirm_desc)) },
                    confirmButton = {
                        val confirmInteraction = remember { MutableInteractionSource() }
                        BoompalaCardButton(
                            onClick = {
                                scope.launch {
                                    withContext(Dispatchers.IO) {
                                        val records = archiveRepository.list(null, null)
                                        records.forEach { archiveRepository.delete(it.id) }
                                    }
                                    archiveCount = 0
                                    showClearDialog = false
                                }
                            },
                            modifier = Modifier.wearPressFeedback(confirmInteraction),
                            interactionSource = confirmInteraction,
                            colors = BoompalaButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.25f),
                            ),
                        ) {
                            Text(
                                stringResource(R.string.action_delete),
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    },
                    dismissButton = {
                        val dismissInteraction = remember { MutableInteractionSource() }
                        BoompalaCardButton(
                            onClick = { showClearDialog = false },
                            modifier = Modifier.wearPressFeedback(dismissInteraction),
                            interactionSource = dismissInteraction,
                            colors = BoompalaButtonDefaults.outlinedButtonColors(),
                        ) {
                            Text(stringResource(R.string.action_cancel))
                        }
                    },
                )
            }

            if (showResetAllDialog) {
                AlertDialog(
                    visible = true,
                    onDismissRequest = { showResetAllDialog = false },
                    title = { Text(stringResource(R.string.settings_reset_all_confirm_title)) },
                    text = { Text(stringResource(R.string.settings_reset_all_confirm_desc)) },
                    confirmButton = {
                        val confirmInteraction = remember { MutableInteractionSource() }
                        BoompalaCardButton(
                            onClick = {
                                onResetAllPreferences()
                                showResetAllDialog = false
                            },
                            modifier = Modifier.wearPressFeedback(confirmInteraction),
                            interactionSource = confirmInteraction,
                            colors = BoompalaButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.25f),
                            ),
                        ) {
                            Text(
                                stringResource(R.string.settings_reset_all),
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    },
                    dismissButton = {
                        val dismissInteraction = remember { MutableInteractionSource() }
                        BoompalaCardButton(
                            onClick = { showResetAllDialog = false },
                            modifier = Modifier.wearPressFeedback(dismissInteraction),
                            interactionSource = dismissInteraction,
                            colors = BoompalaButtonDefaults.outlinedButtonColors(),
                        ) {
                            Text(stringResource(R.string.action_cancel))
                        }
                    },
                )
            }
        }
    }
}
}

@Composable
private fun SettingsModuleButton(
    iconRes: Int,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    animationsEnabled: Boolean = true,
) {
    val pressInteraction = remember { MutableInteractionSource() }
    BoompalaCardButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .wearPressFeedback(pressInteraction),
        interactionSource = pressInteraction,
        colors = BoompalaButtonDefaults.buttonColors(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp),
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.wearMarquee(animationsEnabled),
                )
            }
        }
    }
}

@Composable
private fun SelectionButton(
    selected: Boolean,
    text: String,
    onClick: () -> Unit,
    targetIntensity: HapticIntensity? = null,
) {
    val pressInteraction = remember { MutableInteractionSource() }
    val intensity = targetIntensity ?: LocalHapticIntensity.current
    SelectableCardButton(
        selected = selected,
        onClick = onClick,
        contentPadding = BoompalaButtonDefaults.compactContentPadding,
        modifier = Modifier
            .fillMaxWidth()
            .wearPressFeedback(pressInteraction, intensity = intensity),
        interactionSource = pressInteraction,
    ) {
        Text(
            text = if (selected) "✓ $text" else text,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private val SHICHEN_LABELS = listOf(
    "子时 · 23-01点",
    "丑时 · 01-03点",
    "寅时 · 03-05点",
    "卯时 · 05-07点",
    "辰时 · 07-09点",
    "巳时 · 09-11点",
    "午时 · 11-13点",
    "未时 · 13-15点",
    "申时 · 15-17点",
    "酉时 · 17-19点",
    "戌时 · 19-21点",
    "亥时 · 21-23点",
    "时辰未知",
)

private fun hourToShichenIndex(hour: Int?): Int {
    if (hour == null) return 12
    if (hour >= 23 || hour == 0) return 0
    return ((hour + 1) / 2).coerceIn(0, 11)
}

private fun shichenIndexToHour(index: Int): Int? {
    return when (index) {
        0 -> 0
        1 -> 2
        2 -> 4
        3 -> 6
        4 -> 8
        5 -> 10
        6 -> 12
        7 -> 14
        8 -> 16
        9 -> 18
        10 -> 20
        11 -> 22
        else -> null
    }
}

@Composable
private fun ShichenPicker(
    initialIndex: Int,
    onShichenPicked: (Int) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onDismiss)
    val pickerState = rememberPickerState(
        initialNumberOfOptions = SHICHEN_LABELS.size,
        initiallySelectedIndex = initialIndex.coerceIn(0, SHICHEN_LABELS.lastIndex),
        shouldRepeatOptions = false,
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 6.dp),
        ) {
            Text(
                text = stringResource(R.string.settings_bazi_birth_hour),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 2.dp),
                maxLines = 1,
            )
            Picker(
                state = pickerState,
                contentDescription = { "选择出生时辰" },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) { optionIndex ->
                val isSelected = optionIndex == pickerState.selectedOptionIndex
                Text(
                    text = SHICHEN_LABELS[optionIndex],
                    style = if (isSelected) {
                        MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    } else {
                        MaterialTheme.typography.bodySmall
                    },
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    },
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            val confirmInteraction = remember { MutableInteractionSource() }
            BoompalaCardButton(
                onClick = { onShichenPicked(pickerState.selectedOptionIndex) },
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .padding(bottom = 6.dp)
                    .wearPressFeedback(confirmInteraction),
                interactionSource = confirmInteraction,
                colors = BoompalaButtonDefaults.buttonColors(),
            ) {
                Text(
                    text = "✓ 确定",
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
            }
        }
    }
}
