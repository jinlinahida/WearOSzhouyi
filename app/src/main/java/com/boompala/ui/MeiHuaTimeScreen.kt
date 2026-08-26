package com.boompala.ui

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.OutlinedButton
import androidx.wear.compose.material3.Text
import com.boompala.engine.meihua.MeiHuaTimeEngine
import com.boompala.engine.meihua.MeiHuaTimeReading
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * A page visit fixes one reading. It only obtains another device time after the
 * explicit recast action, never as a ticking clock recomposition.
 */
@Composable
fun MeiHuaTimeScreen(
    engine: MeiHuaTimeEngine,
    initialReading: MeiHuaTimeReading?,
    rotaryScrollingEnabled: Boolean,
    onReadingChanged: (MeiHuaTimeReading) -> Unit,
    onViewReading: (MeiHuaTimeReading) -> Unit,
    onBack: () -> Unit,
) {
    val metrics = LocalUiMetrics.current
    val fixedReading = remember(initialReading, engine) {
        initialReading ?: engine.calculate(Instant.now(), ZoneId.systemDefault())
    }
    LaunchedEffect(fixedReading) { onReadingChanged(fixedReading) }
    val gregorian = remember(fixedReading.timeInfo.gregorianDateTime) {
        DateTimeFormatter.ofPattern("yyyy年M月d日 HH:mm", Locale.CHINA)
            .format(fixedReading.timeInfo.gregorianDateTime)
    }

    RotaryScrollColumn(
        rotaryEnabled = rotaryScrollingEnabled,
        modifier = Modifier.fillMaxSize(),
        contentPadding = metrics.screenPadding,
        itemSpacing = metrics.itemSpacing,
    ) {
        item(key = "meihua-time-title") { Text("时间起卦") }
        item(key = "meihua-time-info") {
            ResultCard {
                DetailField("公历", gregorian)
                DetailField("农历", fixedReading.timeInfo.lunarDate)
                DetailField("当前时辰", fixedReading.timeInfo.hourGanzhi.earthlyBranch.displayName + "时")
                DetailField(
                    "参与数字",
                    "年${fixedReading.numbers.yearBranch} 月${fixedReading.numbers.lunarMonth} " +
                        "日${fixedReading.numbers.lunarDay} 时${fixedReading.numbers.hourBranch}",
                )
            }
        }
        item(key = "meihua-time-preview") {
            ResultCard {
                DetailField("上卦", fixedReading.upperTrigram.displayName)
                fixedReading.upperTrigram.linesFromBottom.indices.reversed().forEach { index ->
                    HexagramLine(
                        trigramLineDisplayAt(
                            linesFromBottom = fixedReading.upperTrigram.linesFromBottom.map { it.isYang },
                            indexFromBottom = index,
                        ),
                    )
                }
                DetailField("下卦", fixedReading.lowerTrigram.displayName)
                fixedReading.lowerTrigram.linesFromBottom.indices.reversed().forEach { index ->
                    HexagramLine(
                        trigramLineDisplayAt(
                            linesFromBottom = fixedReading.lowerTrigram.linesFromBottom.map { it.isYang },
                            indexFromBottom = index,
                        ),
                    )
                }
                DetailField("动爻", fixedReading.movingPosition.displayName)
                DetailField("本卦", fixedReading.original.name)
            }
        }
        item(key = "meihua-time-view") {
            val pressInteraction = remember { MutableInteractionSource() }
            Button(
                onClick = { onViewReading(fixedReading) },
                modifier = Modifier
                    .fillMaxWidth()
                    .wearPressFeedback(pressInteraction),
                interactionSource = pressInteraction,
            ) {
                Text("查看此卦")
            }
        }
        item(key = "meihua-time-recast") {
            val pressInteraction = remember { MutableInteractionSource() }
            OutlinedButton(
                onClick = {
                    onReadingChanged(engine.calculate(Instant.now(), ZoneId.systemDefault()))
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .wearPressFeedback(pressInteraction),
                interactionSource = pressInteraction,
            ) {
                Text("重新按当前时间起卦")
            }
        }
        item(key = "meihua-time-back") {
            val pressInteraction = remember { MutableInteractionSource() }
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier
                    .fillMaxWidth()
                    .wearPressFeedback(pressInteraction),
                interactionSource = pressInteraction,
            ) {
                Text("返回首页")
            }
        }
    }
}
