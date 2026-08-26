package com.boompala.ui

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.wear.compose.material3.OutlinedButton
import androidx.wear.compose.material3.Text
import com.boompala.engine.data.HexagramInterpretationRepository
import com.boompala.engine.meihua.MeiHuaHexagram
import com.boompala.engine.meihua.MeiHuaTimeReading
import com.boompala.engine.model.YaoPolarity
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun MeiHuaResultContent(
    reading: MeiHuaTimeReading,
    interpretations: HexagramInterpretationRepository,
    rotaryScrollingEnabled: Boolean,
    onBack: () -> Unit,
    onArchive: (MeiHuaTimeReading) -> Unit = {},
) {
    val metrics = LocalUiMetrics.current
    val castTime = remember(reading.timeInfo.gregorianDateTime) {
        DateTimeFormatter.ofPattern("yyyy年M月d日 HH:mm", Locale.CHINA)
            .format(reading.timeInfo.gregorianDateTime)
    }
    CommonDivinationResultScreen(
        title = "时间起卦结果",
        rotaryEnabled = rotaryScrollingEnabled,
        contentPadding = metrics.screenPadding,
        itemSpacing = metrics.itemSpacing,
    ) {
        item(key = "meihua-time") {
            ResultCard {
                Text("时间信息")
                DetailField("公历", castTime)
                DetailField("农历", reading.timeInfo.lunarDate)
                DetailField(
                    "四柱",
                    "${reading.timeInfo.yearGanzhi.displayName} " +
                        "${reading.timeInfo.monthGanzhi.displayName} " +
                        "${reading.timeInfo.dayGanzhi.displayName} " +
                        reading.timeInfo.hourGanzhi.displayName,
                )
            }
        }
        item(key = "meihua-calculation") {
            ResultCard {
                Text("起卦数字与计算")
                DetailField(
                    "年、月、日、时",
                    "${reading.numbers.yearBranch}、${reading.numbers.lunarMonth}、" +
                        "${reading.numbers.lunarDay}、${reading.numbers.hourBranch}",
                )
                DetailField(
                    "上卦",
                    "${reading.numbers.upperSum} ÷ 8 取余 = ${reading.upperTrigram.number}（${reading.upperTrigram.displayName}）",
                )
                DetailField(
                    "下卦/动爻",
                    "${reading.numbers.lowerSum} ÷ 8 取余 = ${reading.lowerTrigram.number}（${reading.lowerTrigram.displayName}）；" +
                        "÷ 6 取余 = ${reading.movingPosition.displayName}",
                )
            }
        }
        item(key = "meihua-trigrams") {
            ResultCard {
                DetailField("上卦", reading.upperTrigram.displayName)
                DetailField("下卦", reading.lowerTrigram.displayName)
                DetailField("动爻", reading.movingPosition.displayName)
                DetailField("体卦", reading.bodyTrigram.displayName)
                DetailField("用卦", reading.useTrigram.displayName)
            }
        }
        hexagramCards(reading)
        item(key = "meihua-original-interpretation") {
            HexagramInterpretationCard(
                title = "本卦",
                interpretation = interpretations.interpretationFor(reading.original.codeFromBottom),
            )
        }
        item(key = "meihua-mutual-interpretation") {
            HexagramInterpretationCard(
                title = "互卦",
                interpretation = interpretations.interpretationFor(reading.mutual.codeFromBottom),
            )
        }
        item(key = "meihua-changed-interpretation") {
            HexagramInterpretationCard(
                title = "变卦",
                interpretation = interpretations.interpretationFor(reading.changed.codeFromBottom),
            )
        }
        item(key = "meihua-structure") {
            ResultCard {
                Text("时间起卦结构说明")
                DetailField("本卦", "事情当前或初始状态")
                DetailField("互卦", "事情发展过程")
                DetailField("变卦", "事情变化后的趋势")
                DetailField("体卦", "主体或所问者")
                DetailField("用卦", "外部事物或所问之事")
            }
        }
        item(key = "meihua-archive") {
            val archiveInteraction = remember { MutableInteractionSource() }
            OutlinedButton(
                onClick = { onArchive(reading) },
                modifier = Modifier
                    .fillMaxWidth()
                    .wearPressFeedback(archiveInteraction),
                interactionSource = archiveInteraction,
            ) { Text("归档此次结果") }
        }
        item(key = "meihua-back") {
            val backInteraction = remember { MutableInteractionSource() }
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier
                    .fillMaxWidth()
                    .wearPressFeedback(backInteraction),
                interactionSource = backInteraction,
            ) {
                Text("返回时间起卦")
            }
        }
    }
}

private fun LazyListScope.hexagramCards(reading: MeiHuaTimeReading) {
    val items = listOf(
        "本卦" to reading.original.toDisplayModel(setOf(reading.movingPosition.indexFromBottom)),
        "互卦" to reading.mutual.toDisplayModel(),
        "变卦" to reading.changed.toDisplayModel(setOf(reading.movingPosition.indexFromBottom)),
    )
    items(items = items, key = { "meihua-${it.first}" }) { (label, model) ->
        ResultCard {
            Text("$label：${model.name}")
            model.linesFromBottom.indices.reversed().forEach { index ->
                HexagramLine(model.lineDisplayAt(index))
            }
        }
    }
}

internal fun MeiHuaHexagram.toDisplayModel(
    movingPositions: Set<Int> = emptySet(),
): HexagramDisplayModel = HexagramDisplayModel(
    name = name,
    linesFromBottom = linesFromBottom.map { it == YaoPolarity.YANG },
    movingPositions = movingPositions,
)
