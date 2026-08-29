package com.boompala.ui.pulse

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.remember
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.boompala.engine.pulse.PulseDiagnosisResult
import com.boompala.ui.BoompalaButtonDefaults
import com.boompala.ui.BoompalaCardButton
import com.boompala.ui.DetailField
import com.boompala.ui.LocalHapticFeedbackEnabled
import com.boompala.ui.LocalHapticIntensity
import com.boompala.ui.LocalUiMetrics
import com.boompala.ui.ResultCard
import com.boompala.ui.RotaryScrollColumn
import com.boompala.ui.wearPressFeedback

/**
 * 脉象推演结果页面。
 * 严格遵从项目统一样式规范与 8 层垂直辨证调摄架构：
 * 1. 脉象名称与属性
 * 2. 典型脉象图与四字脉诀特征
 * 3. 宜 / 忌
 * 4. 辨证调理方法与食疗
 * 5. 情绪情志建议
 * 6. 生活习惯与起居
 * 7. 运动建议
 * 8. 中医典籍渊源与医理解释
 * 9. 当令经络巡行 (子午流注)
 * 10. 保存归档操作
 */
@Composable
fun PulseResultScreen(
    result: PulseDiagnosisResult,
    rotaryEnabled: Boolean,
    onSaveArchive: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val metrics = LocalUiMetrics.current
    val profile = result.profile

    RotaryScrollColumn(
        rotaryEnabled = rotaryEnabled,
        modifier = modifier.fillMaxSize(),
        contentPadding = metrics.screenPadding,
        itemSpacing = metrics.itemSpacing,
    ) {
        // 顶部标题
        item(key = "result-title") {
            Text(
                text = "把脉结果",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = metrics.itemSpacing / 2),
            )
        }

        // 第 1 层：脉象名称与主属性
        item(key = "pulse-name-card") {
            ResultCard {
                Text(
                    text = "【${result.category.chineseName}】",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = result.category.natureSummary,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // 第 2 层：脉象波形图（置顶） + 脉象特征文字表述
        item(key = "pulse-feature-card") {
            ResultCard {
                // 典型波形图示置于卡片顶部呈现
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .padding(vertical = 4.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    ReferenceWaveformCanvas(
                        points = profile.waveformPoints,
                        lineColor = Color(0xFF00E5A3),
                    )
                }
                Text(
                    text = "脉象特征 · ${result.category.classicPhrase}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF00E5A3),
                )
                Text(
                    text = profile.featureDescription,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        // 第 3 层：宜做什么，不宜做什么 (宜/忌)
        item(key = "pulse-dos-donts-card") {
            ResultCard {
                Text(
                    text = "【宜】 ${profile.dosList.joinToString(" · ")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF00E5A3),
                )
                Text(
                    text = "【忌】 ${profile.dontsList.joinToString(" · ")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFFF6B6B),
                )
            }
        }

        // 第 4 层：辨证调理方法 (兼夹症状与对症食疗)
        item(key = "pulse-syndromes-card") {
            ResultCard {
                Text(
                    text = "辨证调理",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                profile.syndromes.forEachIndexed { index, syndrome ->
                    Column(
                        modifier = Modifier.padding(top = if (index > 0) 6.dp else 0.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            text = "· ${syndrome.title}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = syndrome.symptoms,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = "调理方案：${syndrome.dietaryRecommendations}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }

        // 第 5 层：情绪建议
        item(key = "pulse-emotion-card") {
            ResultCard {
                DetailField(
                    label = "情绪调摄",
                    value = profile.emotionalAdvice,
                )
            }
        }

        // 第 6 层：生活习惯建议
        item(key = "pulse-lifestyle-card") {
            ResultCard {
                DetailField(
                    label = "生活起居",
                    value = profile.lifestyleAdvice,
                )
            }
        }

        // 第 7 层：运动建议
        item(key = "pulse-exercise-card") {
            ResultCard {
                DetailField(
                    label = "运动调养",
                    value = profile.exerciseAdvice,
                )
            }
        }

        // 第 8 层：中医典籍出处与医理解析
        item(key = "pulse-theory-card") {
            ResultCard {
                Text(
                    text = "典籍出处",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFE0A96D),
                )
                Text(
                    text = profile.classicLiterature,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "医理释义",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFE0A96D),
                    modifier = Modifier.padding(top = 4.dp),
                )
                Text(
                    text = profile.theoreticalReason,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        // 第 9 层：时令经络巡行 (子午流注)
        item(key = "meridian-flow-card") {
            ResultCard {
                DetailField(
                    label = "时辰经络 · ${result.meridianInfo.earthlyBranch} (${result.meridianInfo.timeRangeText})",
                    value = "${result.meridianInfo.meridianName}当令",
                )
                Text(
                    text = result.meridianInfo.healthGuidance,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // 底部操作：存入归档
        item(key = "pulse-archive-btn") {
            val archivePressInteraction = remember { MutableInteractionSource() }
            val hapticEnabled = LocalHapticFeedbackEnabled.current
            val hapticIntensity = LocalHapticIntensity.current
            BoompalaCardButton(
                onClick = onSaveArchive,
                modifier = Modifier
                    .fillMaxWidth()
                    .wearPressFeedback(
                        interactionSource = archivePressInteraction,
                        hapticEnabled = hapticEnabled,
                        intensity = hapticIntensity,
                    ),
                interactionSource = archivePressInteraction,
                colors = BoompalaButtonDefaults.outlinedButtonColors(),
            ) {
                Text("存入归档")
            }
        }
    }
}
