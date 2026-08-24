package com.boompala.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.boompala.settings.AppSettings
import com.boompala.settings.ContentSize
import com.boompala.settings.ScreenShape

data class UiMetrics(
    val horizontalPadding: Dp,
    val verticalPadding: Dp,
    val itemSpacing: Dp,
    val cardVerticalPadding: Dp,
    val screenPadding: PaddingValues = PaddingValues(
        horizontal = horizontalPadding,
        vertical = verticalPadding,
    ),
)

val LocalUiMetrics = staticCompositionLocalOf {
    ContentSize.STANDARD.uiMetrics(ScreenShape.ROUND)
}

fun ContentSize.uiMetrics(screenShape: ScreenShape): UiMetrics {
    val scale = when (this) {
        ContentSize.SMALL -> 0.88f
        ContentSize.STANDARD -> 1.0f
        ContentSize.LARGE -> 1.12f
    }
    val horizontalBase = if (screenShape == ScreenShape.ROUND) 20.dp else 16.dp
    return UiMetrics(
        horizontalPadding = horizontalBase * scale,
        verticalPadding = 24.dp * scale,
        itemSpacing = 8.dp * scale,
        cardVerticalPadding = 10.dp * scale,
    )
}

@Composable
fun WithContentScale(
    settings: AppSettings,
    screenShape: ScreenShape,
    content: @Composable () -> Unit,
) {
    val baseDensity = LocalDensity.current
    val currentContext = androidx.compose.ui.platform.LocalContext.current
    val currentConfiguration = androidx.compose.ui.platform.LocalConfiguration.current

    val scaledDensity = remember(baseDensity.density, settings.contentSize) {
        androidx.compose.ui.unit.Density(
            density = baseDensity.density,
            fontScale = baseDensity.fontScale * settings.contentSize.fontScale,
        )
    }
    val metrics = remember(settings.contentSize, screenShape) {
        settings.contentSize.uiMetrics(screenShape)
    }

    val targetLocale = remember(settings.language) {
        if (settings.language == com.boompala.settings.AppLanguage.ENGLISH) {
            java.util.Locale.ENGLISH
        } else {
            java.util.Locale.SIMPLIFIED_CHINESE
        }
    }

    val localizedContext = remember(currentContext, targetLocale) {
        val config = android.content.res.Configuration(currentContext.resources.configuration).apply {
            setLocale(targetLocale)
        }
        currentContext.createConfigurationContext(config)
    }

    val localizedConfiguration = remember(currentConfiguration, targetLocale) {
        android.content.res.Configuration(currentConfiguration).apply {
            setLocale(targetLocale)
        }
    }

    CompositionLocalProvider(
        androidx.compose.ui.platform.LocalContext provides localizedContext,
        androidx.compose.ui.platform.LocalConfiguration provides localizedConfiguration,
        LocalDensity provides scaledDensity,
        LocalUiMetrics provides metrics,
        content = content,
    )
}
