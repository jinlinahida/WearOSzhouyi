package com.boompala.ui

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.OutlinedButton
import androidx.wear.compose.material3.Text
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.boompala.R
import com.boompala.engine.bazi.BaziProfile
import com.boompala.engine.dailyfortune.DailyFortuneReading
import com.boompala.engine.dailyfortune.PersonalFortuneEvaluator
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Daily fortune page: a deterministic almanac-style presentation of the
 * current natural day. Readings with null text fields (repository degraded)
 * hide the affected section instead of crashing.
 *
 * When [baziProfile] is provided, personal daily fortune (ShiShen theme,
 * active ShenSha, branch interactions, and personal balance color) is
 * evaluated and prominently displayed at the top.
 */
@Composable
fun DailyFortuneScreen(
    reading: DailyFortuneReading,
    rotaryScrollingEnabled: Boolean,
    onBack: () -> Unit,
    baziProfile: BaziProfile? = null,
    animationsEnabled: Boolean = true,
    onConfigureBazi: (() -> Unit)? = null,
) {
    val metrics = LocalUiMetrics.current
    val gregorianText = remember(reading.date) {
        DateTimeFormatter.ofPattern("yyyy-MM-dd EEEE", Locale.getDefault()).format(reading.date)
    }
    val personalFortune = remember(baziProfile, reading) {
        baziProfile?.let { PersonalFortuneEvaluator.evaluate(it, reading) }
    }

    RotaryScrollColumn(
        rotaryEnabled = rotaryScrollingEnabled,
        modifier = Modifier.fillMaxSize(),
        contentPadding = metrics.screenPadding,
        itemSpacing = metrics.itemSpacing,
    ) {
        item(key = "header") {
            Text(
                text = stringResource(R.string.daily_fortune_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = metrics.itemSpacing / 2),
            )
        }
        item(key = "date") {
            Column(
                verticalArrangement = Arrangement.spacedBy(metrics.itemSpacing / 2),
            ) {
                Text(gregorianText, style = MaterialTheme.typography.bodySmall)
                Text(
                    "农历${reading.lunarDateText} · ${reading.dayGanzhi.displayName}日 · 日干属${reading.dayStemElement.displayName}",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        if (baziProfile != null && personalFortune != null) {
            item(key = "personal-fortune-main") {
                ResultCard {
                    Text(
                        text = "${baziProfile.gender.titleZh} · ${baziProfile.dayMaster.displayName}${baziProfile.dayMasterElement.displayName}日主",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.wearMarquee(animationsEnabled),
                    )
                    Text(
                        text = "【${personalFortune.shiShenName}】${personalFortune.themeTitle}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.wearMarquee(animationsEnabled),
                    )
                    Text(
                        text = personalFortune.themeAdvice,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    DetailField(
                        label = "专属平衡色",
                        value = "${personalFortune.balanceColor.displayName} · 幸运数 ${personalFortune.balanceNumbers.joinToString("、")}",
                    )
                    DetailField(
                        label = "卦日感应",
                        value = personalFortune.hexagramResonance,
                        marquee = true,
                        animationsEnabled = animationsEnabled,
                    )
                }
            }

            if (personalFortune.events.isNotEmpty()) {
                item(key = "personal-fortune-events") {
                    ResultCard {
                        Text(
                            text = "命盘星曜感应",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        personalFortune.events.forEach { event ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                verticalArrangement = Arrangement.spacedBy(1.dp),
                            ) {
                                Text(
                                    text = (if (event.isAuspicious) "✦ " else "▲ ") + event.title,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (event.isAuspicious) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.wearMarquee(animationsEnabled),
                                )
                                Text(
                                    text = event.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        } else {
            item(key = "personal-fortune-prompt") {
                ResultCard {
                    Text(
                        text = "专属命盘日运",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = "在设置中配置生辰八字，即可解锁每日流日十神、个人神煞与干支合冲分析",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (onConfigureBazi != null) {
                        val configInteraction = remember { MutableInteractionSource() }
                        BoompalaCardButton(
                            onClick = onConfigureBazi,
                            modifier = Modifier
                                .fillMaxWidth()
                                .wearPressFeedback(configInteraction),
                            interactionSource = configInteraction,
                            colors = BoompalaButtonDefaults.buttonColors(),
                        ) {
                            Text(
                                text = "前往我的档案设置",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        }
        item(key = "hexagram") {
            Column(
                verticalArrangement = Arrangement.spacedBy(metrics.itemSpacing / 2),
            ) {
                Text(
                    stringResource(R.string.daily_fortune_day_hexagram, reading.dayHexagramName),
                    style = MaterialTheme.typography.titleMedium,
                )
                val summary = reading.hexagramSummary
                if (summary != null) {
                    Text(summary, style = MaterialTheme.typography.bodyMedium)
                }
                val advice = reading.hexagramAdvice
                if (advice != null) {
                    Text(advice, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        val lineText = reading.dayLineText
        if (lineText != null) {
            item(key = "day-line") {
                ResultCard {
                    Text(
                        stringResource(R.string.daily_fortune_day_line, reading.dayLinePosition.displayName),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(lineText, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        item(key = "colors") {
            ResultCard {
                Text(stringResource(R.string.daily_fortune_colors), style = MaterialTheme.typography.titleSmall)
                DetailField(stringResource(R.string.daily_fortune_lucky_color), reading.luckyColor.displayName)
                DetailField(stringResource(R.string.daily_fortune_support_color), reading.supportColor.displayName)
                DetailField(stringResource(R.string.daily_fortune_avoid_color), reading.avoidColor.displayName)
            }
        }
        if (reading.luckyNumbers.isNotEmpty()) {
            item(key = "numbers") {
                ResultCard {
                    Text(stringResource(R.string.daily_fortune_numbers), style = MaterialTheme.typography.titleSmall)
                    Text(reading.luckyNumbers.joinToString("、"), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        if (reading.directions.isNotEmpty()) {
            item(key = "directions") {
                ResultCard {
                    Text(stringResource(R.string.daily_fortune_directions), style = MaterialTheme.typography.titleSmall)
                    reading.directions.forEach { direction ->
                        DetailField(
                            direction.deity.displayName,
                            "${direction.direction.directionText}方 · ${direction.description}",
                        )
                    }
                }
            }
        }
        if (reading.hours.isNotEmpty()) {
            item(key = "hours") {
                ResultCard {
                    Text(stringResource(R.string.daily_fortune_hours), style = MaterialTheme.typography.titleSmall)
                    reading.hours.forEach { hour ->
                        Text(
                            "${hour.branch.displayName}时 · ${hour.periodText} · ${hour.deityName}",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
        item(key = "jianchu") {
            ResultCard {
                Text(
                    stringResource(R.string.daily_fortune_jianchu, reading.jianChu.displayName),
                    style = MaterialTheme.typography.titleSmall,
                )
                if (reading.jianChu.suitable.isNotEmpty()) {
                    DetailField(stringResource(R.string.daily_fortune_suitable), reading.jianChu.suitable.joinToString("、"))
                }
                if (reading.jianChu.avoid.isNotEmpty()) {
                    DetailField(stringResource(R.string.daily_fortune_avoid), reading.jianChu.avoid.joinToString("、"))
                }
            }
        }
        item(key = "disclaimer") {
            Text(
                stringResource(R.string.daily_fortune_disclaimer),
                style = MaterialTheme.typography.bodySmall,
            )
        }
        item(key = "back") {
            val pressInteraction = remember { MutableInteractionSource() }
            BoompalaCardButton(
                onClick = onBack,
                modifier = Modifier
                    .fillMaxWidth()
                    .wearPressFeedback(pressInteraction),
                interactionSource = pressInteraction,
                colors = BoompalaButtonDefaults.outlinedButtonColors(),
            ) {
                Text(stringResource(R.string.action_back_home))
            }
        }
    }
}
