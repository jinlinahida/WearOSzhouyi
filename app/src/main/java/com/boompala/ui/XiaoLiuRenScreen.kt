package com.boompala.ui

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.OutlinedButton
import androidx.wear.compose.material3.Text
import com.boompala.engine.xiaoliuren.XiaoLiuRenEngine
import com.boompala.engine.xiaoliuren.XiaoLiuRenPalace
import com.boompala.engine.xiaoliuren.XiaoLiuRenReading
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun XiaoLiuRenScreen(engine: XiaoLiuRenEngine, initial: XiaoLiuRenReading?, rotary: Boolean, onReading: (XiaoLiuRenReading) -> Unit, onArchive: (XiaoLiuRenReading) -> Unit, onBack: () -> Unit) {
    val metrics = LocalUiMetrics.current
    val reading = remember(initial, engine) { initial ?: engine.calculate(Instant.now(), ZoneId.systemDefault()) }
    androidx.compose.runtime.LaunchedEffect(reading) { onReading(reading) }
    val fmt = remember { DateTimeFormatter.ofPattern("yyyy年M月d日 HH:mm", Locale.CHINA) }
    RotaryScrollColumn(rotaryEnabled = rotary, modifier = Modifier.fillMaxSize(), contentPadding = metrics.screenPadding, itemSpacing = metrics.itemSpacing) {
        item {
            ScreenTitle(
                text = "小六壬",
                style = MaterialTheme.typography.titleMedium,
            )
        }
        item {
            ResultCard {
                DetailField("公历", fmt.format(reading.timeInfo.gregorianDateTime), marquee = true)
                DetailField("农历", reading.timeInfo.lunarDate, marquee = true)
                DetailField("当前时辰", reading.timeInfo.hourGanzhi.earthlyBranch.displayName + "时", marquee = true)
                DetailField("起课数据", "月${reading.timeInfo.lunarMonth} 日${reading.timeInfo.lunarDay} 时${reading.timeInfo.hourGanzhi.earthlyBranch.index + 1}", marquee = true)
            }
        }
        item {
            ResultCard {
                Text("六宫次序")
                XiaoLiuRenPalace.entries.forEach { p ->
                    Text(
                        text = if (p == reading.finalPalace) "▶ ${p.displayName} · 课成" else p.displayName,
                        modifier = Modifier.wearMarquee(),
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        item {
            ResultCard {
                DetailField("月宫", reading.monthPalace.displayName, marquee = true)
                DetailField("日宫", reading.dayPalace.displayName, marquee = true)
                DetailField("时宫/最终", reading.finalPalace.displayName, marquee = true)
                Text(reading.finalPalace.meaning)
            }
        }
        item {
            val pressInteraction = remember { MutableInteractionSource() }
            BoompalaCardButton(
                onClick = { onReading(engine.calculate(Instant.now(), ZoneId.systemDefault())) },
                modifier = Modifier.fillMaxWidth().wearPressFeedback(pressInteraction),
                interactionSource = pressInteraction,
            ) {
                Text(
                    text = "重新起课",
                    modifier = Modifier.wearMarquee(),
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                val archiveInteraction = remember { MutableInteractionSource() }
                BoompalaCardButton(
                    onClick = { onArchive(reading) },
                    modifier = Modifier.weight(1f).wearPressFeedback(archiveInteraction),
                    interactionSource = archiveInteraction,
                    colors = BoompalaButtonDefaults.outlinedButtonColors(),
                ) {
                    Text(
                        text = "归档",
                        modifier = Modifier.wearMarquee(),
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                val backInteraction = remember { MutableInteractionSource() }
                BoompalaCardButton(
                    onClick = onBack,
                    modifier = Modifier.weight(1f).wearPressFeedback(backInteraction),
                    interactionSource = backInteraction,
                    colors = BoompalaButtonDefaults.outlinedButtonColors(),
                ) {
                    Text(
                        text = "首页",
                        modifier = Modifier.wearMarquee(),
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
