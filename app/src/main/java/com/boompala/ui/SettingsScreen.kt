package com.boompala.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.OutlinedButton
import androidx.wear.compose.material3.Text
import com.boompala.settings.AppSettings
import com.boompala.settings.ContentSize
import com.boompala.settings.ScreenMode

@Composable
fun SettingsScreen(
    settings: AppSettings,
    onScreenModeSelected: (ScreenMode) -> Unit,
    onContentSizeSelected: (ContentSize) -> Unit,
    onAnimationsEnabledChange: (Boolean) -> Unit,
    onRotaryScrollingEnabledChange: (Boolean) -> Unit,
    rotaryScrollingEnabled: Boolean,
    onAboutClick: () -> Unit,
    onBack: () -> Unit,
) {
    val metrics = LocalUiMetrics.current

    RotaryScrollColumn(
        rotaryEnabled = rotaryScrollingEnabled,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            horizontal = metrics.horizontalPadding,
            vertical = metrics.verticalPadding,
        ),
        itemSpacing = metrics.itemSpacing,
    ) {
        item(key = "title") {
            Text("设置", style = MaterialTheme.typography.titleLarge)
        }
        item(key = "display-section") {
            Text("显示", style = MaterialTheme.typography.titleSmall)
        }
        item(key = "screen-mode-label") {
            Text("屏幕模式：${settings.screenMode.displayName}")
        }
        ScreenMode.entries.forEach { mode ->
            item(key = "screen-mode-${mode.name}") {
                SelectionButton(
                    selected = settings.screenMode == mode,
                    text = mode.displayName,
                    onClick = { onScreenModeSelected(mode) },
                )
            }
        }
        item(key = "content-size-label") {
            Text("内容大小：${settings.contentSize.displayName}")
        }
        ContentSize.entries.forEach { size ->
            item(key = "content-size-${size.name}") {
                SelectionButton(
                    selected = settings.contentSize == size,
                    text = size.displayName,
                    onClick = { onContentSizeSelected(size) },
                )
            }
        }
        item(key = "animation-label") {
            Text("动画")
        }
        item(key = "animation-toggle") {
            SelectionButton(
                selected = settings.animationsEnabled,
                text = if (settings.animationsEnabled) "已开启" else "已关闭",
                onClick = { onAnimationsEnabledChange(!settings.animationsEnabled) },
            )
        }
        item(key = "interaction-section") {
            Text("交互设置", style = MaterialTheme.typography.titleSmall)
        }
        item(key = "rotary-label") {
            Text("表冠滚动")
        }
        item(key = "rotary-toggle") {
            SelectionButton(
                selected = settings.rotaryScrollingEnabled,
                text = if (settings.rotaryScrollingEnabled) "已开启" else "已关闭",
                onClick = {
                    onRotaryScrollingEnabledChange(!settings.rotaryScrollingEnabled)
                },
            )
        }
        item(key = "about-section") {
            Text("关于", style = MaterialTheme.typography.titleSmall)
        }
        item(key = "about") {
            OutlinedButton(
                onClick = onAboutClick,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("版本、作者与开源信息")
            }
        }
        item(key = "back") {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("返回")
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
    if (selected) {
        Button(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("✓ $text")
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text)
        }
    }
}
