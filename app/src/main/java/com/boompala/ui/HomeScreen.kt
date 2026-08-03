package com.boompala.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.OutlinedButton
import androidx.wear.compose.material3.Text

private data class HomeEntry(
    val title: String,
    val enabled: Boolean = false,
)

@Composable
fun HomeScreen(
    rotaryScrollingEnabled: Boolean,
    onSixYaoClick: () -> Unit,
    onMeiHuaClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onXiaoLiuRenClick: () -> Unit,
    onArchiveClick: () -> Unit,
    onCompassClick: () -> Unit,
    onBrowseClick: () -> Unit,
) {
    val entries = listOf(
        HomeEntry("历史记录"),
        HomeEntry("更多功能"),
    )
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
            Text(
                text = "Boompala 易学",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = metrics.itemSpacing / 2),
            )
        }
        item(key = "six-yao") {
            Button(
                onClick = onSixYaoClick,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("六爻排盘")
            }
        }
        item(key = "mei-hua") {
            Button(
                onClick = onMeiHuaClick,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("时间起卦")
            }
        }
        item(key = "xiaoliuren") { Button(onClick = onXiaoLiuRenClick, modifier = Modifier.fillMaxWidth()) { Text("小六壬") } }
        item(key = "compass") { Button(onClick = onCompassClick, modifier = Modifier.fillMaxWidth()) { Text("罗盘") } }
        item(key = "archives") { OutlinedButton(onClick = onArchiveClick, modifier = Modifier.fillMaxWidth()) { Text("归档") } }
        item(key = "browse") { Button(onClick = onBrowseClick, modifier = Modifier.fillMaxWidth()) { Text("浏览") } }
        item(key = "settings") {
            OutlinedButton(
                onClick = onSettingsClick,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("设置")
            }
        }
        items(
            items = entries,
            key = HomeEntry::title,
        ) { entry ->
            OutlinedButton(
                onClick = { },
                enabled = entry.enabled,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("${entry.title} · 开发中")
            }
        }
    }
}
