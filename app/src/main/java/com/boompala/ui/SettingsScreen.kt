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
import com.boompala.settings.HomeFeature
import com.boompala.settings.ScreenMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class SettingsSection {
    MENU,
    APPEARANCE,
    LANGUAGE,
    HOME,
    HAPTICS,
    DATA,
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

                item(key = "module-appearance") {
                    SettingsModuleButton(
                        iconRes = R.drawable.ic_settings_display,
                        title = stringResource(R.string.settings_module_appearance),
                        subtitle = stringResource(R.string.settings_module_appearance_desc),
                        onClick = { currentSection = SettingsSection.APPEARANCE },
                    )
                }

                item(key = "module-language") {
                    SettingsModuleButton(
                        iconRes = R.drawable.ic_settings_language,
                        title = stringResource(R.string.settings_module_language),
                        subtitle = if (settings.language == AppLanguage.CHINESE) "简体中文" else "English",
                        onClick = { currentSection = SettingsSection.LANGUAGE },
                    )
                }

                item(key = "module-home") {
                    SettingsModuleButton(
                        iconRes = R.drawable.ic_settings_home,
                        title = stringResource(R.string.settings_module_home),
                        subtitle = stringResource(R.string.settings_module_home_desc),
                        onClick = { currentSection = SettingsSection.HOME },
                    )
                }

                item(key = "module-haptics") {
                    SettingsModuleButton(
                        iconRes = R.drawable.ic_settings_haptics,
                        title = stringResource(R.string.settings_module_haptics),
                        subtitle = stringResource(R.string.settings_module_haptics_desc),
                        onClick = { currentSection = SettingsSection.HAPTICS },
                    )
                }

                item(key = "module-data") {
                    SettingsModuleButton(
                        iconRes = R.drawable.ic_settings_data,
                        title = stringResource(R.string.settings_module_data),
                        subtitle = stringResource(R.string.settings_module_data_desc),
                        onClick = { currentSection = SettingsSection.DATA },
                    )
                }

                item(key = "module-about") {
                    SettingsModuleButton(
                        iconRes = R.drawable.ic_settings_about,
                        title = stringResource(R.string.settings_module_about),
                        subtitle = stringResource(R.string.settings_module_about_desc),
                        onClick = onAboutClick,
                    )
                }

                item(key = "back-home") {
                    val backInteraction = remember { MutableInteractionSource() }
                    OutlinedButton(
                        onClick = onBack,
                        modifier = Modifier
                            .fillMaxWidth()
                            .wearPressFeedback(backInteraction),
                        interactionSource = backInteraction,
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

                item(key = "appearance-back") {
                    val backInteraction = remember { MutableInteractionSource() }
                    OutlinedButton(
                        onClick = { currentSection = SettingsSection.MENU },
                        modifier = Modifier
                            .fillMaxWidth()
                            .wearPressFeedback(backInteraction),
                        interactionSource = backInteraction,
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
                    OutlinedButton(
                        onClick = { currentSection = SettingsSection.MENU },
                        modifier = Modifier
                            .fillMaxWidth()
                            .wearPressFeedback(backInteraction),
                        interactionSource = backInteraction,
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
                        HomeFeature.TAROT_ONE -> stringResource(R.string.home_feature_tarot_one)
                        HomeFeature.TAROT_THREE -> stringResource(R.string.home_feature_tarot_three)
                        HomeFeature.TAROT_HOLY_TRIANGLE -> stringResource(R.string.home_feature_tarot_holy_triangle)
                        HomeFeature.DAILY_FORTUNE -> stringResource(R.string.home_feature_daily_fortune)
                        HomeFeature.XIAO_LIU_REN -> stringResource(R.string.home_feature_xiao_liu_ren)
                        HomeFeature.COMPASS -> stringResource(R.string.home_feature_compass)
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
                            )
                            Text(
                                text = if (isHidden) stringResource(R.string.action_hide) else stringResource(R.string.action_show),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isHidden) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            val toggleInteraction = remember { MutableInteractionSource() }
                            val upInteraction = remember { MutableInteractionSource() }
                            val downInteraction = remember { MutableInteractionSource() }
                            OutlinedButton(
                                onClick = { onToggleHomeFeatureVisibility(feature) },
                                modifier = Modifier
                                    .weight(1f)
                                    .wearPressFeedback(toggleInteraction),
                                interactionSource = toggleInteraction,
                            ) {
                                Text(if (isHidden) stringResource(R.string.action_show) else stringResource(R.string.action_hide))
                            }
                            OutlinedButton(
                                onClick = { onMoveHomeFeature(feature, true) },
                                enabled = index > 0,
                                modifier = Modifier
                                    .weight(0.7f)
                                    .wearPressFeedback(upInteraction, enabled = index > 0),
                                interactionSource = upInteraction,
                            ) {
                                Text("▲")
                            }
                            OutlinedButton(
                                onClick = { onMoveHomeFeature(feature, false) },
                                enabled = index < fullOrder.size - 1,
                                modifier = Modifier
                                    .weight(0.7f)
                                    .wearPressFeedback(downInteraction, enabled = index < fullOrder.size - 1),
                                interactionSource = downInteraction,
                            ) {
                                Text("▼")
                            }
                        }
                    }
                }

                item(key = "home-manage-back") {
                    val backInteraction = remember { MutableInteractionSource() }
                    OutlinedButton(
                        onClick = { currentSection = SettingsSection.MENU },
                        modifier = Modifier
                            .fillMaxWidth()
                            .wearPressFeedback(backInteraction),
                        interactionSource = backInteraction,
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
                    OutlinedButton(
                        onClick = { currentSection = SettingsSection.MENU },
                        modifier = Modifier
                            .fillMaxWidth()
                            .wearPressFeedback(backInteraction),
                        interactionSource = backInteraction,
                    ) {
                        Text(stringResource(R.string.action_back))
                    }
                }
            }
        }

        SettingsSection.DATA -> {
            val scope = rememberCoroutineScope()
            var archiveCount by remember { mutableIntStateOf(0) }
            var showClearDialog by remember { mutableStateOf(false) }

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
                        OutlinedButton(
                            onClick = { showClearDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .wearPressFeedback(pressInteraction),
                            interactionSource = pressInteraction,
                        ) {
                            Text(
                                text = stringResource(R.string.settings_data_clear_all),
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }

                item(key = "data-back") {
                    val backInteraction = remember { MutableInteractionSource() }
                    OutlinedButton(
                        onClick = { currentSection = SettingsSection.MENU },
                        modifier = Modifier
                            .fillMaxWidth()
                            .wearPressFeedback(backInteraction),
                        interactionSource = backInteraction,
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
                        Button(
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
                        ) {
                            Text(stringResource(R.string.action_delete))
                        }
                    },
                    dismissButton = {
                        val dismissInteraction = remember { MutableInteractionSource() }
                        OutlinedButton(
                            onClick = { showClearDialog = false },
                            modifier = Modifier.wearPressFeedback(dismissInteraction),
                            interactionSource = dismissInteraction,
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
) {
    val pressInteraction = remember { MutableInteractionSource() }
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .wearPressFeedback(pressInteraction),
        interactionSource = pressInteraction,
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
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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
    if (selected) {
        Button(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .wearPressFeedback(pressInteraction, intensity = intensity),
            interactionSource = pressInteraction,
        ) {
            Text("✓ $text")
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .wearPressFeedback(pressInteraction, intensity = intensity),
            interactionSource = pressInteraction,
        ) {
            Text(text)
        }
    }
}
