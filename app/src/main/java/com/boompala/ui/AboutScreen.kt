package com.boompala.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.OutlinedButton
import androidx.wear.compose.material3.Text
import com.boompala.BuildConfig

private const val GitHubProjectUrl = ""

@Composable
fun AboutScreen(
    rotaryScrollingEnabled: Boolean,
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
            Text("Boompala", style = MaterialTheme.typography.titleLarge)
        }
        item(key = "version") {
            Text("版本：${BuildConfig.VERSION_NAME}")
        }
        item(key = "author") {
            Text("作者：Glorious Aster")
        }
        item(key = "description") {
            Text("离线六爻排盘工具，Inspiration nmixx")
        }
        item(key = "open-source-title") {
            Text("开源信息", style = MaterialTheme.typography.titleSmall)
        }
        item(key = "technology") {
            Text("使用 Kotlin 与 Wear OS Compose 构建")
        }
        item(key = "algorithm-source") {
            Text("六爻算法参考 bopo/najia 的 MIT 规则实现，并重写为纯 Kotlin")
        }
        item(key = "calendar-source") {
            Text("四柱历法使用 6tail/lunar-java（MIT）")
        }
        item(key = "data-source") {
            Text("爻辞数据来自中文维基文库《周易》，数据按 CC BY-SA 4.0 归因")
        }
        item(key = "interpretation-source") {
            Text("六十四卦通用解释为项目原创整理，按 CC0 1.0 发布")
        }
        item(key = "github") {
            OutlinedButton(
                onClick = { },
                enabled = GitHubProjectUrl.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (GitHubProjectUrl.isBlank()) "GitHub：待配置" else "打开 GitHub")
            }
        }
        item(key = "back") {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("返回设置")
            }
        }
    }
}
