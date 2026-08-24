package com.boompala.ui

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.OutlinedButton
import androidx.wear.compose.material3.Text
import com.boompala.BuildConfig
import com.boompala.R

@Composable
fun AboutScreen(
    rotaryScrollingEnabled: Boolean,
    onBack: () -> Unit,
) {
    val metrics = LocalUiMetrics.current

    RotaryScrollColumn(
        rotaryEnabled = rotaryScrollingEnabled,
        modifier = Modifier.fillMaxSize(),
        contentPadding = metrics.screenPadding,
        itemSpacing = metrics.itemSpacing,
    ) {
        item(key = "title") {
            Text(
                text = "关于应用",
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        // 1. 应用信息卡片
        item(key = "app-info-card") {
            ResultCard {
                Text(
                    text = "boompala",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "离线东方易学与西方塔罗研习工具，单机独立运行，零网络依赖。",
                    style = MaterialTheme.typography.bodySmall,
                )
                DetailField("当前版本", BuildConfig.VERSION_NAME)
            }
        }

        // 2. 开发者卡片
        item(key = "developer-card") {
            ResultCard {
                Text(
                    text = "开发者",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Image(
                        painter = painterResource(R.drawable.developer_avatar),
                        contentDescription = "开发者头像",
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape),
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Glorious Aster",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "All for hearts2hearts",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        // 3. 免责声明卡片
        item(key = "disclaimer-card") {
            ResultCard {
                Text(
                    text = "免责声明",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "本应用提供的六爻、梅花易数、小六壬、每日运势及塔罗牌等内容仅供传统文化研究、学习与娱乐参考，不构成医疗、法律、金融或任何专业领域的决策建议，请勿将结果作为现实决定的唯一依据。",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        // 4. 数据与版权/许可声明卡片
        item(key = "license-card") {
            ResultCard {
                Text(
                    text = "数据与版权声明",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "• 周易经典：爻辞来自中文维基文库《周易》（CC BY-SA 4.0），算法参考 bopo/najia（MIT）。",
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    text = "• 历法引擎：四柱干支与农历计算使用 6tail/lunar-java（MIT）。",
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    text = "• 塔罗牌面：基于 1909 年 A. E. Waite 与 P. C. Smith 公有领域 (Public Domain) 原作，由 LuciellaES 高清修复并采用 CC0 1.0 Universal 协议。",
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    text = "• 塔罗数据：英文语料来自 Mark McElroy / Corpora（CC0 1.0）；中文核心牌义、关键词与占卜指引由 Boompala 项目独立翻译与结构化整理。",
                    style = MaterialTheme.typography.bodySmall,
                )
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
