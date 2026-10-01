package com.boompala.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import com.boompala.settings.AiNetworkMode
import com.boompala.settings.AiProvider
import com.boompala.ai.sync.LocalKeySyncServer
import com.boompala.ai.sync.QrCodeGenerator
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.zIndex
import androidx.wear.compose.material3.DatePicker
import androidx.wear.compose.material3.Picker
import androidx.wear.compose.material3.rememberPickerState
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
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
    AI,
    AI_SYNC,
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
    onScalingListEnabledChange: (Boolean) -> Unit = {},
    onRotaryScrollingEnabledChange: (Boolean) -> Unit,
    onHapticFeedbackEnabledChange: (Boolean) -> Unit = {},
    onHapticIntensityChange: (HapticIntensity) -> Unit = {},
    onLanguageSelected: (AppLanguage) -> Unit = {},
    onMoveHomeFeature: (HomeFeature, Boolean) -> Unit = { _, _ -> },
    onReorderHomeFeatures: (List<HomeFeature>) -> Unit = {},
    onToggleHomeFeatureVisibility: (HomeFeature) -> Unit = {},
    onSaveUserBirth: (birthDate: String, birthHour: Int?, gender: BaziGender) -> Unit = { _, _, _ -> },
    onClearUserBirth: () -> Unit = {},
    onKeepScreenOnEnabledChange: (Boolean) -> Unit = {},
    onCompassTrueNorthEnabledChange: (Boolean) -> Unit = {},
    onCompassDeclinationChange: (Float) -> Unit = {},
    onTarotReversedEnabledChange: (Boolean) -> Unit = {},
    onTarotMajorArcanaOnlyChange: (Boolean) -> Unit = {},
    onResetAllPreferences: () -> Unit = {},
    onResetMuyuCount: () -> Unit = {},
    onAiNetworkModeSelected: (AiNetworkMode) -> Unit = {},
    onAiProviderSelected: (AiProvider) -> Unit = {},
    onSaveAiConfig: (provider: AiProvider, apiKey: String, baseUrl: String, model: String) -> Unit = { _, _, _, _ -> },
    onClearAiApiKey: () -> Unit = {},
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
        AppHaptics.back(
            context = context,
            intensity = settings.hapticIntensity,
            enabled = settings.hapticFeedbackEnabled,
        )
        if (currentSection == SettingsSection.AI_SYNC) {
            currentSection = SettingsSection.AI
        } else {
            currentSection = SettingsSection.MENU
        }
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
            ScalingRotaryScrollColumn(
                rotaryEnabled = rotaryScrollingEnabled,
                hapticFeedbackEnabled = settings.hapticFeedbackEnabled,
                animationsEnabled = settings.scalingListEnabled && settings.animationsEnabled,
                modifier = Modifier.fillMaxSize(),
                contentPadding = metrics.screenPadding,
                itemSpacing = metrics.itemSpacing,
            ) {
                item(key = "title") {
                    ScreenTitle(
                        text = stringResource(R.string.settings_title),
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

                item(key = "module-ai") {
                    val aiDesc = when (settings.aiNetworkMode) {
                        AiNetworkMode.LOCAL -> stringResource(R.string.settings_ai_mode_local)
                        AiNetworkMode.ONLINE -> {
                            if (settings.isAiConfigured) {
                                "${settings.aiProvider.displayName} · ${settings.maskedApiKey()}"
                            } else {
                                "${settings.aiProvider.displayName} · ${stringResource(R.string.settings_ai_key_not_set)}"
                            }
                        }
                    }
                    SettingsModuleButton(
                        iconRes = R.drawable.ic_settings_ai,
                        title = stringResource(R.string.settings_module_ai),
                        subtitle = aiDesc,
                        onClick = { currentSection = SettingsSection.AI },
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
                    ScreenTitle(
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
                    ToggleSwitchButton(
                        checked = settings.animationsEnabled,
                        text = if (settings.animationsEnabled) stringResource(R.string.action_enabled) else stringResource(R.string.action_disabled),
                        onCheckedChange = { onAnimationsEnabledChange(it) },
                        hapticIntensity = settings.hapticIntensity,
                        hapticEnabled = settings.hapticFeedbackEnabled,
                    )
                }

                item(key = "scaling-list-title") {
                    Text(
                        text = stringResource(R.string.settings_scaling_list),
                        style = MaterialTheme.typography.titleSmall,
                    )
                }
                item(key = "scaling-list-toggle") {
                    ToggleSwitchButton(
                        checked = settings.scalingListEnabled,
                        text = if (settings.scalingListEnabled) stringResource(R.string.action_enabled) else stringResource(R.string.action_disabled),
                        onCheckedChange = { onScalingListEnabledChange(it) },
                        hapticIntensity = settings.hapticIntensity,
                        hapticEnabled = settings.hapticFeedbackEnabled,
                    )
                }

                item(key = "keep-screen-on-title") {
                    Text(
                        text = stringResource(R.string.settings_keep_screen_on),
                        style = MaterialTheme.typography.titleSmall,
                    )
                }
                item(key = "keep-screen-on-toggle") {
                    ToggleSwitchButton(
                        checked = settings.keepScreenOnEnabled,
                        text = if (settings.keepScreenOnEnabled) stringResource(R.string.action_enabled) else stringResource(R.string.action_disabled),
                        onCheckedChange = { onKeepScreenOnEnabledChange(it) },
                        hapticIntensity = settings.hapticIntensity,
                        hapticEnabled = settings.hapticFeedbackEnabled,
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
                    ScreenTitle(
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
                    ScreenTitle(
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
                    ScreenTitle(
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
            var localOrder by remember(settings.homeOrder) {
                mutableStateOf(settings.effectiveHomeOrder())
            }
            val lazyListState = rememberLazyListState()
            val reorderableLazyColumnState = rememberReorderableLazyListState(lazyListState) { from, to ->
                val fromKey = (from.key as? String)?.removePrefix("home-feat-")
                val toKey = (to.key as? String)?.removePrefix("home-feat-")

                if (fromKey != null && toKey != null) {
                    val fromIndex = localOrder.indexOfFirst { it.id == fromKey }
                    val toIndex = localOrder.indexOfFirst { it.id == toKey }

                    if (fromIndex != -1 && toIndex != -1 && fromIndex != toIndex) {
                        localOrder = localOrder.toMutableList().apply {
                            add(toIndex, removeAt(fromIndex))
                        }
                        AppHaptics.click(
                            context = context,
                            intensity = HapticIntensity.LIGHT,
                            enabled = settings.hapticFeedbackEnabled,
                        )
                    }
                }
            }

            RotaryScrollColumn(
                rotaryEnabled = rotaryScrollingEnabled,
                modifier = Modifier.fillMaxSize(),
                state = lazyListState,
                contentPadding = metrics.screenPadding,
                itemSpacing = metrics.itemSpacing,
            ) {
                item(key = "home-manage-title") {
                    ScreenTitle(
                        text = stringResource(R.string.settings_module_home),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }

                item(key = "home-drag-hint") {
                    ResultCard {
                        Text(
                            text = stringResource(R.string.settings_home_drag_hint),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
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

                itemsIndexed(localOrder, key = { _, feature -> "home-feat-${feature.id}" }) { index, feature ->
                    val isHidden = settings.hiddenHomeFeatures.contains(feature)
                    ReorderableItem(
                        state = reorderableLazyColumnState,
                        key = "home-feat-${feature.id}",
                    ) { isDragging ->
                        val handleInteraction = remember { MutableInteractionSource() }
                        val cardInteraction = remember { MutableInteractionSource() }

                        HomeFeatureReorderCard(
                            feature = feature,
                            index = index,
                            isHidden = isHidden,
                            isDragging = isDragging,
                            animationsEnabled = settings.animationsEnabled,
                            onToggleVisibility = {
                                val willBeShown = isHidden
                                AppHaptics.toggle(
                                    context = context,
                                    targetState = willBeShown,
                                    intensity = settings.hapticIntensity,
                                    enabled = settings.hapticFeedbackEnabled,
                                )
                                onToggleHomeFeatureVisibility(feature)
                            },
                            modifier = Modifier.longPressDraggableHandle(
                                onDragStarted = {
                                    AppHaptics.cardFlip(
                                        context = context,
                                        intensity = settings.hapticIntensity,
                                        enabled = settings.hapticFeedbackEnabled,
                                    )
                                },
                                onDragStopped = {
                                    onReorderHomeFeatures(localOrder)
                                    AppHaptics.click(
                                        context = context,
                                        intensity = settings.hapticIntensity,
                                        enabled = settings.hapticFeedbackEnabled,
                                    )
                                },
                                interactionSource = cardInteraction,
                            ),
                            handleModifier = Modifier.draggableHandle(
                                onDragStarted = {
                                    AppHaptics.cardFlip(
                                        context = context,
                                        intensity = settings.hapticIntensity,
                                        enabled = settings.hapticFeedbackEnabled,
                                    )
                                },
                                onDragStopped = {
                                    onReorderHomeFeatures(localOrder)
                                    AppHaptics.click(
                                        context = context,
                                        intensity = settings.hapticIntensity,
                                        enabled = settings.hapticFeedbackEnabled,
                                    )
                                },
                                interactionSource = handleInteraction,
                            ),
                        )
                    }
                }

                item(key = "home-manage-back") {
                    val backInteraction = remember { MutableInteractionSource() }
                    BoompalaCardButton(
                        onClick = {
                            if (localOrder != settings.homeOrder) {
                                onReorderHomeFeatures(localOrder)
                            }
                            currentSection = SettingsSection.MENU
                        },
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
                    ScreenTitle(
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
                    ToggleSwitchButton(
                        checked = settings.rotaryScrollingEnabled,
                        text = if (settings.rotaryScrollingEnabled) stringResource(R.string.action_enabled) else stringResource(R.string.action_disabled),
                        onCheckedChange = { onRotaryScrollingEnabledChange(it) },
                        hapticIntensity = settings.hapticIntensity,
                        hapticEnabled = settings.hapticFeedbackEnabled,
                    )
                }

                item(key = "haptic-title") {
                    Text(
                        text = stringResource(R.string.settings_haptic),
                        style = MaterialTheme.typography.titleSmall,
                    )
                }
                item(key = "haptic-toggle") {
                    ToggleSwitchButton(
                        checked = settings.hapticFeedbackEnabled,
                        text = if (settings.hapticFeedbackEnabled) stringResource(R.string.action_enabled) else stringResource(R.string.action_disabled),
                        onCheckedChange = { nextState ->
                            AppHaptics.toggle(
                                context = context,
                                targetState = nextState,
                                intensity = settings.hapticIntensity,
                                enabled = true,
                            )
                            onHapticFeedbackEnabledChange(nextState)
                        },
                        hapticIntensity = settings.hapticIntensity,
                        hapticEnabled = false,
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
                                onClick = {
                                    onHapticIntensityChange(intensity)
                                    AppHaptics.preview(context, intensity)
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
                                    ScreenTitle(
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
                                ScreenTitle(
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
            var showResetMuyuDialog by remember { mutableStateOf(false) }
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
                    ScreenTitle(
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

                item(key = "data-muyu-stats") {
                    ResultCard {
                        Text(
                            text = stringResource(R.string.settings_data_muyu_total, settings.muyuTotalCount),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }

                if (settings.muyuTotalCount > 0L) {
                    item(key = "data-muyu-reset") {
                        val pressInteraction = remember { MutableInteractionSource() }
                        BoompalaCardButton(
                            onClick = { showResetMuyuDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .wearPressFeedback(pressInteraction),
                            interactionSource = pressInteraction,
                            colors = BoompalaButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        ) {
                            Text(
                                text = stringResource(R.string.settings_data_muyu_reset),
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

            if (showResetMuyuDialog) {
                AlertDialog(
                    visible = true,
                    onDismissRequest = { showResetMuyuDialog = false },
                    title = { Text(stringResource(R.string.settings_data_muyu_reset_confirm_title)) },
                    text = { Text(stringResource(R.string.settings_data_muyu_reset_confirm_desc)) },
                    confirmButton = {
                        val confirmInteraction = remember { MutableInteractionSource() }
                        BoompalaCardButton(
                            onClick = {
                                onResetMuyuCount()
                                showResetMuyuDialog = false
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
                            onClick = { showResetMuyuDialog = false },
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

        SettingsSection.AI -> {
            var showClearKeyDialog by remember { mutableStateOf(false) }
            var showTestResultDialog by remember { mutableStateOf(false) }

            RotaryScrollColumn(
                rotaryEnabled = rotaryScrollingEnabled,
                modifier = Modifier.fillMaxSize(),
                contentPadding = metrics.screenPadding,
                itemSpacing = metrics.itemSpacing,
            ) {
                item(key = "ai-title") {
                    ScreenTitle(
                        text = stringResource(R.string.settings_ai_title),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }

                item(key = "ai-mode-header") {
                    Text(
                        text = stringResource(R.string.settings_ai_mode),
                        style = MaterialTheme.typography.titleSmall,
                    )
                }

                item(key = "ai-mode-selector") {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        val isLocal = settings.aiNetworkMode == AiNetworkMode.LOCAL
                        SelectableCardButton(
                            selected = isLocal,
                            onClick = { onAiNetworkModeSelected(AiNetworkMode.LOCAL) },
                            contentPadding = BoompalaButtonDefaults.compactContentPadding,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = if (isLocal) "✓ ${stringResource(R.string.settings_ai_mode_local)}" else stringResource(R.string.settings_ai_mode_local),
                                fontWeight = if (isLocal) FontWeight.Bold else FontWeight.Normal,
                            )
                        }

                        val isOnline = settings.aiNetworkMode == AiNetworkMode.ONLINE
                        SelectableCardButton(
                            selected = isOnline,
                            onClick = { onAiNetworkModeSelected(AiNetworkMode.ONLINE) },
                            contentPadding = BoompalaButtonDefaults.compactContentPadding,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = if (isOnline) "✓ ${stringResource(R.string.settings_ai_mode_online)}" else stringResource(R.string.settings_ai_mode_online),
                                fontWeight = if (isOnline) FontWeight.Bold else FontWeight.Normal,
                            )
                        }
                    }
                }

                if (settings.aiNetworkMode == AiNetworkMode.ONLINE) {
                    item(key = "ai-provider-header") {
                        Text(
                            text = stringResource(R.string.settings_ai_provider),
                            style = MaterialTheme.typography.titleSmall,
                        )
                    }

                    item(key = "ai-provider-chips") {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            AiProvider.entries.forEach { provider ->
                                val isSelected = settings.aiProvider == provider
                                SelectableCardButton(
                                    selected = isSelected,
                                    onClick = { onAiProviderSelected(provider) },
                                    contentPadding = BoompalaButtonDefaults.compactContentPadding,
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text(
                                        text = if (isSelected) "✓ ${provider.displayName}" else provider.displayName,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    )
                                }
                            }
                        }
                    }

                    item(key = "ai-info-card") {
                        ResultCard {
                            DetailField(
                                label = stringResource(R.string.settings_ai_model),
                                value = settings.effectiveAiModel.ifBlank { "默认" },
                            )
                            val keyDisplay = if (settings.isAiConfigured) {
                                stringResource(R.string.settings_ai_key_configured, settings.maskedApiKey())
                            } else {
                                stringResource(R.string.settings_ai_key_not_set)
                            }
                            DetailField(
                                label = stringResource(R.string.settings_ai_api_key),
                                value = keyDisplay,
                            )
                            if (settings.effectiveAiBaseUrl.isNotBlank()) {
                                DetailField(
                                    label = "Base URL",
                                    value = settings.effectiveAiBaseUrl,
                                )
                            }
                        }
                    }

                    item(key = "ai-sync-action") {
                        val syncInteraction = remember { MutableInteractionSource() }
                        BoompalaCardButton(
                            onClick = { currentSection = SettingsSection.AI_SYNC },
                            modifier = Modifier
                                .fillMaxWidth()
                                .wearPressFeedback(syncInteraction),
                            interactionSource = syncInteraction,
                        ) {
                            Text(
                                text = stringResource(R.string.settings_ai_sync_button),
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }

                    item(key = "ai-test-action") {
                        val testInteraction = remember { MutableInteractionSource() }
                        BoompalaCardButton(
                            onClick = { showTestResultDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .wearPressFeedback(testInteraction),
                            interactionSource = testInteraction,
                            colors = BoompalaButtonDefaults.outlinedButtonColors(),
                        ) {
                            Text(stringResource(R.string.settings_ai_test_button))
                        }
                    }

                    if (settings.isAiConfigured) {
                        item(key = "ai-clear-action") {
                            val clearInteraction = remember { MutableInteractionSource() }
                            BoompalaCardButton(
                                onClick = { showClearKeyDialog = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .wearPressFeedback(clearInteraction),
                                interactionSource = clearInteraction,
                                colors = BoompalaButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.25f),
                                ),
                            ) {
                                Text(
                                    text = stringResource(R.string.settings_ai_clear_key),
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                    }
                } else {
                    item(key = "ai-local-desc") {
                        Text(
                            text = stringResource(R.string.settings_ai_mode_local_desc),
                            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xB3FFFFFF)),
                        )
                    }
                }

                item(key = "ai-back") {
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

            if (showClearKeyDialog) {
                AlertDialog(
                    visible = true,
                    onDismissRequest = { showClearKeyDialog = false },
                    title = { Text(stringResource(R.string.settings_ai_clear_confirm_title)) },
                    text = { Text(stringResource(R.string.settings_ai_clear_confirm_desc)) },
                    confirmButton = {
                        val confirmInteraction = remember { MutableInteractionSource() }
                        BoompalaCardButton(
                            onClick = {
                                onClearAiApiKey()
                                showClearKeyDialog = false
                            },
                            modifier = Modifier.wearPressFeedback(confirmInteraction),
                            interactionSource = confirmInteraction,
                            colors = BoompalaButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.25f),
                            ),
                        ) {
                            Text(
                                stringResource(R.string.action_confirm),
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    },
                    dismissButton = {
                        val dismissInteraction = remember { MutableInteractionSource() }
                        BoompalaCardButton(
                            onClick = { showClearKeyDialog = false },
                            modifier = Modifier.wearPressFeedback(dismissInteraction),
                            interactionSource = dismissInteraction,
                            colors = BoompalaButtonDefaults.outlinedButtonColors(),
                        ) {
                            Text(stringResource(R.string.action_cancel))
                        }
                    },
                )
            }

            if (showTestResultDialog) {
                AlertDialog(
                    visible = true,
                    onDismissRequest = { showTestResultDialog = false },
                    title = { Text(stringResource(R.string.settings_ai_test_button)) },
                    text = {
                        Text(
                            if (settings.isAiConfigured) stringResource(R.string.settings_ai_test_ready)
                            else stringResource(R.string.settings_ai_test_need_key),
                        )
                    },
                    confirmButton = {
                        val confirmInteraction = remember { MutableInteractionSource() }
                        BoompalaCardButton(
                            onClick = { showTestResultDialog = false },
                            modifier = Modifier.wearPressFeedback(confirmInteraction),
                            interactionSource = confirmInteraction,
                        ) {
                            Text(stringResource(R.string.action_confirm))
                        }
                    },
                )
            }
        }

        SettingsSection.AI_SYNC -> {
            var pairingInfo by remember { mutableStateOf<LocalKeySyncServer.PairingInfo?>(null) }
            var isPairedSuccess by remember { mutableStateOf(false) }
            var isTimeout by remember { mutableStateOf(false) }
            var hasNoWifi by remember { mutableStateOf(false) }

            val server = remember { LocalKeySyncServer() }
            DisposableEffect(Unit) {
                val info = server.start(
                    onConfigReceived = { payload ->
                        onSaveAiConfig(
                            payload.provider,
                            payload.apiKey,
                            payload.customBaseUrl,
                            payload.customModel,
                        )
                        AppHaptics.success(
                            context = context,
                            intensity = settings.hapticIntensity,
                            enabled = settings.hapticFeedbackEnabled,
                        )
                        isPairedSuccess = true
                    },
                    onTimeout = {
                        isTimeout = true
                    },
                )
                if (info != null) {
                    pairingInfo = info
                } else {
                    hasNoWifi = true
                }
                onDispose {
                    server.stop()
                }
            }

            LaunchedEffect(isPairedSuccess) {
                if (isPairedSuccess) {
                    kotlinx.coroutines.delay(1800)
                    currentSection = SettingsSection.AI
                }
            }

            val currentPairingUrl = pairingInfo?.url
            val qrBitmap = remember(currentPairingUrl) {
                currentPairingUrl?.let { QrCodeGenerator.generateQrBitmap(it, 220) }
            }

            RotaryScrollColumn(
                rotaryEnabled = rotaryScrollingEnabled,
                modifier = Modifier.fillMaxSize(),
                contentPadding = metrics.screenPadding,
                itemSpacing = metrics.itemSpacing,
            ) {
                item(key = "sync-title") {
                    ScreenTitle(
                        text = stringResource(R.string.settings_ai_sync_title),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }

                if (isPairedSuccess) {
                    item(key = "sync-success-card") {
                        ResultCard(
                            borderColor = Color(0xFF66BB6A),
                        ) {
                            Text(
                                text = "🎉 " + stringResource(R.string.settings_ai_sync_success),
                                style = MaterialTheme.typography.titleSmall,
                                color = Color(0xFF81C784),
                            )
                            Text(
                                text = "秘钥与服务商配置已写入手表",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                    item(key = "sync-success-back") {
                        val doneInteraction = remember { MutableInteractionSource() }
                        BoompalaCardButton(
                            onClick = { currentSection = SettingsSection.AI },
                            modifier = Modifier
                                .fillMaxWidth()
                                .wearPressFeedback(doneInteraction),
                            interactionSource = doneInteraction,
                        ) {
                            Text(stringResource(R.string.action_confirm))
                        }
                    }
                } else if (hasNoWifi) {
                    item(key = "sync-no-wifi-card") {
                        ResultCard(
                            borderColor = MaterialTheme.colorScheme.error,
                        ) {
                            Text(
                                text = "⚠️ " + stringResource(R.string.settings_ai_sync_no_wifi),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                            Text(
                                text = stringResource(R.string.settings_ai_sync_no_wifi_desc),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                    item(key = "sync-no-wifi-back") {
                        val backInteraction = remember { MutableInteractionSource() }
                        BoompalaCardButton(
                            onClick = { currentSection = SettingsSection.AI },
                            modifier = Modifier
                                .fillMaxWidth()
                                .wearPressFeedback(backInteraction),
                            interactionSource = backInteraction,
                        ) {
                            Text(stringResource(R.string.action_back))
                        }
                    }
                } else if (isTimeout) {
                    item(key = "sync-timeout-card") {
                        ResultCard(
                            borderColor = Color(0xFFFFB74D),
                        ) {
                            Text(
                                text = "⏰ 配对超时",
                                style = MaterialTheme.typography.titleSmall,
                                color = Color(0xFFFFB74D),
                            )
                            Text(
                                text = "配对服务已自动关闭，请重试",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                    item(key = "sync-timeout-back") {
                        val backInteraction = remember { MutableInteractionSource() }
                        BoompalaCardButton(
                            onClick = { currentSection = SettingsSection.AI },
                            modifier = Modifier
                                .fillMaxWidth()
                                .wearPressFeedback(backInteraction),
                            interactionSource = backInteraction,
                        ) {
                            Text(stringResource(R.string.action_back))
                        }
                    }
                } else {
                    pairingInfo?.let { info ->
                        item(key = "sync-qr-box") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(136.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.White)
                                        .padding(6.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (qrBitmap != null) {
                                        Image(
                                            bitmap = qrBitmap,
                                            contentDescription = "Pairing QR",
                                            modifier = Modifier.fillMaxSize(),
                                        )
                                    }
                                }
                            }
                        }

                        item(key = "sync-hint") {
                            Text(
                                text = stringResource(R.string.settings_ai_sync_hint),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp,
                                ),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }

                        item(key = "sync-address-card") {
                            ResultCard {
                                DetailField(label = "局域网", value = "${info.ip}:${info.port}")
                                DetailField(label = "凭证", value = info.token)
                            }
                        }

                        item(key = "sync-cancel-action") {
                            val cancelInteraction = remember { MutableInteractionSource() }
                            BoompalaCardButton(
                                onClick = { currentSection = SettingsSection.AI },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .wearPressFeedback(cancelInteraction),
                                interactionSource = cancelInteraction,
                                colors = BoompalaButtonDefaults.outlinedButtonColors(),
                            ) {
                                Text(stringResource(R.string.action_cancel))
                            }
                        }
                    }
                }
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
) {
    val pressInteraction = remember { MutableInteractionSource() }
    SelectableCardButton(
        selected = selected,
        onClick = onClick,
        contentPadding = BoompalaButtonDefaults.compactContentPadding,
        modifier = Modifier
            .fillMaxWidth()
            .wearPressFeedback(pressInteraction),
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

@Composable
private fun ToggleSwitchButton(
    checked: Boolean,
    text: String,
    onCheckedChange: (Boolean) -> Unit,
    hapticIntensity: HapticIntensity,
    hapticEnabled: Boolean,
) {
    val context = LocalContext.current
    val pressInteraction = remember { MutableInteractionSource() }
    SelectableCardButton(
        selected = checked,
        onClick = {
            val nextState = !checked
            AppHaptics.toggle(
                context = context,
                targetState = nextState,
                intensity = hapticIntensity,
                enabled = hapticEnabled,
            )
            onCheckedChange(nextState)
        },
        contentPadding = BoompalaButtonDefaults.compactContentPadding,
        modifier = Modifier
            .fillMaxWidth()
            .wearPressFeedback(pressInteraction, hapticEnabled = false),
        interactionSource = pressInteraction,
    ) {
        Text(
            text = if (checked) "✓ $text" else text,
            fontWeight = if (checked) FontWeight.Bold else FontWeight.Normal,
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

@Composable
private fun HomeFeatureReorderCard(
    feature: HomeFeature,
    index: Int,
    isHidden: Boolean,
    isDragging: Boolean,
    animationsEnabled: Boolean,
    onToggleVisibility: () -> Unit,
    modifier: Modifier = Modifier,
    handleModifier: Modifier = Modifier,
) {
    val scale by animateFloatAsState(
        targetValue = if (isDragging) 1.04f else 1.0f,
        animationSpec = if (animationsEnabled) {
            spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow,
            )
        } else {
            androidx.compose.animation.core.snap()
        },
        label = "dragScale",
    )
    val borderColor by animateColorAsState(
        targetValue = if (isDragging) MaterialTheme.colorScheme.primary else CardBorderColor,
        label = "dragBorderColor",
    )
    val borderWidth by animateDpAsState(
        targetValue = if (isDragging) 1.8.dp else CardBorderWidth,
        label = "dragBorderWidth",
    )
    val cardBgColor by animateColorAsState(
        targetValue = when {
            isDragging -> MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.95f)
            isHidden -> CardBackgroundColor.copy(alpha = 0.18f)
            else -> CardBackgroundColor
        },
        label = "dragBgColor",
    )

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

    Box(
        modifier = modifier
            .scale(scale)
            .zIndex(if (isDragging) 10f else 1f)
            .clip(CardShape)
            .border(
                width = borderWidth,
                shape = CardShape,
                brush = if (isDragging) {
                    SolidColor(borderColor)
                } else {
                    Brush.linearGradient(
                        listOf(borderColor, Color.Transparent),
                        start = Offset.Zero,
                        end = Offset.Infinite,
                    )
                },
            )
            .background(cardBgColor)
            .padding(horizontal = 8.dp, vertical = 7.dp)
            .fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 序号胶囊徽章
            Box(
                modifier = Modifier
                    .size(width = 26.dp, height = 22.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        if (isDragging) MaterialTheme.colorScheme.primaryContainer
                        else if (isHidden) MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.5f)
                        else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "%02d".format(index + 1),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isHidden) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.primary,
                )
            }

            Spacer(Modifier.width(8.dp))

            // 功能名称与状态文本
            Column(
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = featureName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isHidden) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.wearMarquee(animationsEnabled),
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = if (isHidden) stringResource(R.string.action_hide) else stringResource(R.string.action_show),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isHidden) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                )
            }

            Spacer(Modifier.width(6.dp))

            // 右侧操作：眼睛显隐切换按钮 + 拖拽手柄
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                val toggleInteraction = remember { MutableInteractionSource() }
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = toggleInteraction,
                            indication = null,
                            onClick = onToggleVisibility,
                        )
                        .background(MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(if (isHidden) R.drawable.ic_visibility_off else R.drawable.ic_visibility),
                        contentDescription = if (isHidden) stringResource(R.string.action_show) else stringResource(R.string.action_hide),
                        tint = if (isHidden) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(17.dp),
                    )
                }

                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .then(handleModifier)
                        .background(
                            if (isDragging) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                            else MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.5f)
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_drag_handle),
                        contentDescription = stringResource(R.string.action_drag_reorder),
                        tint = if (isDragging) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(17.dp),
                    )
                }
            }
        }
    }
}
