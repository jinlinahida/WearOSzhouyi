package com.boompala.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.OutlinedButton
import androidx.wear.compose.material3.Text
import com.boompala.engine.data.HexagramInterpretation
import com.boompala.engine.model.Hexagram
import com.boompala.engine.model.Yao
import com.boompala.engine.model.YaoPosition
import com.boompala.engine.model.DivinationResult
import java.time.format.DateTimeFormatter
import java.util.Locale

private val ResultCardShape = RoundedCornerShape(12.dp)
private val ResultCardColor = Color(0xFF1B1B1B)

@Composable
fun LiuYaoResultContent(
    reading: GeneratedReading,
    rotaryScrollingEnabled: Boolean,
    onBack: () -> Unit,
    onArchive: (DivinationResult) -> Unit = {},
) {
    val metrics = LocalUiMetrics.current
    val result = reading.result
    val dateTimeFormatter = remember {
        DateTimeFormatter.ofPattern("yyyy年M月d日 HH:mm", Locale.CHINA)
    }
    val castTime = remember(result.timeInfo.gregorianDateTime) {
        dateTimeFormatter.format(result.timeInfo.gregorianDateTime)
    }
    val originalYaoCards = remember(result.original) {
        result.yaoFromBottom
            .forResultDisplay()
            .map(Yao::toCardData)
    }
    val changedYaoCards = remember(result) {
        result.changed?.yaoFromBottom
            ?.forResultDisplay()
            ?.map { changedYao ->
                changedYao.toCardData(
                    originalYao = result.original.yaoFromBottom.single { originalYao ->
                        originalYao.position == changedYao.position
                    },
                )
            }
            .orEmpty()
    }
    val originalInterpretation = remember(result.original.pattern.codeFromBottom) {
        reading.interpretations.interpretationFor(result.original.pattern.codeFromBottom)
    }
    val changedInterpretation = remember(result.changed?.pattern?.codeFromBottom) {
        result.changed?.let { changed ->
            reading.interpretations.interpretationFor(changed.pattern.codeFromBottom)
        }
    }
    val movingSummary = remember(result) {
        result.changingPositions.takeIf { it.isNotEmpty() }
            ?.joinToString(prefix = "动爻：") { it.displayName }
            ?: "无动爻"
    }
    val voidSummary = remember(result) { result.toVoidSummary() }

    CommonDivinationResultScreen(
        title = "起卦结果",
        rotaryEnabled = rotaryScrollingEnabled,
        contentPadding = metrics.screenPadding,
        itemSpacing = metrics.itemSpacing,
    ) {
        // The line-card order is intentionally unchanged from the former
        // ResultScreen: shared chrome owns only the Wear scrolling shell.
        item(key = "calendar-time") {
            ResultCard {
                Text("公历", style = MaterialTheme.typography.titleSmall)
                Text(castTime, style = MaterialTheme.typography.bodySmall)
                Text("农历", style = MaterialTheme.typography.titleSmall)
                Text(result.timeInfo.lunarDate, style = MaterialTheme.typography.bodySmall)
            }
        }
        item(key = "original-hexagram") {
            HexagramSummaryCard(title = "本卦", hexagram = result.original)
        }
        result.changed?.let { changed ->
            item(key = "changed-hexagram") {
                HexagramSummaryCard(title = "变卦", hexagram = changed)
            }
        }
        item(key = "original-interpretation") {
            HexagramInterpretationCard(
                title = "本卦",
                interpretation = originalInterpretation,
            )
        }
        result.changed?.let {
            item(key = "changed-interpretation") {
                HexagramInterpretationCard(
                    title = "变卦",
                    interpretation = changedInterpretation,
                )
            }
            item(key = "hexagram-transition") {
                HexagramTransitionCard(
                    original = originalInterpretation,
                    changed = changedInterpretation,
                    changingPositions = result.changingPositions,
                )
            }
        }
        item(key = "four-pillars") {
            FourPillarsCard(result)
        }
        item(key = "moving-summary") {
            Text(movingSummary, style = MaterialTheme.typography.bodyMedium)
        }
        item(key = "original-yao-section-title") {
            Text("本卦完整装卦", style = MaterialTheme.typography.titleSmall)
        }
        items(
            items = originalYaoCards,
            key = { card -> "original-${card.position.indexFromBottom}" },
        ) { card ->
            YaoDetailCard(card)
        }
        result.changed?.let { changed ->
            item(key = "changed-yao-section-title") {
                Text(
                    "变卦完整装卦：${changed.name}",
                    style = MaterialTheme.typography.titleSmall,
                )
            }
            items(
                items = changedYaoCards,
                key = { card -> "changed-${card.position.indexFromBottom}" },
            ) { card ->
                YaoDetailCard(card)
            }
        }
        item(key = "void-summary") {
            VoidSummaryCard(voidSummary)
        }
        item(key = "archive") { OutlinedButton(onClick = { onArchive(result) }, modifier = Modifier.fillMaxWidth()) { Text("归档此次结果") } }
        item(key = "back") {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("返回修改")
            }
        }
    }
}

@Composable
private fun HexagramSummaryCard(
    title: String,
    hexagram: Hexagram,
) {
    val displayModel = hexagram.toDisplayModel()
    ResultCard {
        Text("$title：${hexagram.name}", style = MaterialTheme.typography.titleSmall)
        Text(
            "卦宫：${hexagram.palace.displayName} · ${hexagram.element.displayName}",
            style = MaterialTheme.typography.bodySmall,
        )
        Text(
            "世应：${hexagram.shiPosition.displayName}/${hexagram.yingPosition.displayName}",
            style = MaterialTheme.typography.bodySmall,
        )
        Text("卦象", style = MaterialTheme.typography.labelSmall)
        displayModel.linesFromBottom.indices.reversed().forEach { index ->
            HexagramLine(displayModel.lineDisplayAt(index))
        }
    }
}

@Composable
private fun FourPillarsCard(result: DivinationResult) {
    val timeInfo = result.timeInfo
    ResultCard {
        Text("四柱", style = MaterialTheme.typography.titleSmall)
        Text("年柱：${timeInfo.yearGanzhi.displayName}    月柱：${timeInfo.monthGanzhi.displayName}")
        Text("日柱：${timeInfo.dayGanzhi.displayName}    时柱：${timeInfo.hourGanzhi.displayName}")
    }
}

@Composable
private fun YaoDetailCard(card: YaoCardData) {
    ResultCard {
        HexagramLine(card.lineDisplay)
        Text(
            "${card.position.displayName} · ${card.yinYang} · ${card.motion}",
            style = MaterialTheme.typography.titleSmall,
        )
        card.changeDescription?.let { description ->
            Text(description, style = MaterialTheme.typography.labelMedium)
        }
        card.shiYing?.let { marker ->
            Text(marker, style = MaterialTheme.typography.labelMedium)
        }
        DetailField(label = "六神", value = card.sixSpirit)
        DetailField(label = "六亲", value = card.sixRelation)
        DetailField(label = "天干地支", value = card.ganzhi)
        DetailField(label = "五行", value = card.element)
        if (card.isVoid) {
            DetailField(label = "状态", value = "空亡")
        }
        card.lineText?.let { lineText ->
            Text("动爻爻辞", style = MaterialTheme.typography.labelMedium)
            Text(lineText, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
internal fun HexagramInterpretationCard(
    title: String,
    interpretation: HexagramInterpretation?,
) {
    var expanded by rememberSaveable(interpretation?.codeFromBottom) { mutableStateOf(false) }

    ResultCard {
        Text("${title}解释", style = MaterialTheme.typography.titleSmall)
        if (interpretation == null) {
            Text("离线解释数据不可用", style = MaterialTheme.typography.bodySmall)
        } else {
            Text(interpretation.coreMeaning, style = MaterialTheme.typography.bodySmall)
            Text(
                "关键词：${interpretation.keywords.joinToString(" · ")}",
                style = MaterialTheme.typography.labelSmall,
            )
            OutlinedButton(
                onClick = { expanded = !expanded },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (expanded) "收起详细解释" else "展开详细解释")
            }
            if (expanded) {
                DetailField(
                    label = "上卦",
                    value = formatTrigram(interpretation.upperTrigram),
                )
                DetailField(
                    label = "下卦",
                    value = formatTrigram(interpretation.lowerTrigram),
                )
                DetailField(label = "通用趋势", value = interpretation.generalTrend)
                DetailField(label = "处事建议", value = interpretation.advice)
                DetailField(label = "感情说明", value = interpretation.relationship)
                DetailField(label = "学业/事业说明", value = interpretation.career)
                DetailField(label = "财运说明", value = interpretation.wealth)
            }
        }
    }
}

@Composable
private fun HexagramTransitionCard(
    original: HexagramInterpretation?,
    changed: HexagramInterpretation?,
    changingPositions: List<YaoPosition>,
) {
    val changingLines = changingPositions
        .sortedByDescending(YaoPosition::indexFromBottom)
        .joinToString("、") { position -> position.displayName }
    ResultCard {
        Text("本卦到变卦", style = MaterialTheme.typography.titleSmall)
        if (original != null && changed != null) {
            Text(
                "动爻：$changingLines。由“${original.coreMeaning}”转向“${changed.coreMeaning}”",
                style = MaterialTheme.typography.bodySmall,
            )
        } else {
            Text("离线解释数据不可用；动爻：$changingLines", style = MaterialTheme.typography.bodySmall)
        }
    }
}

private fun formatTrigram(trigram: com.boompala.engine.data.TrigramInterpretation): String =
    "${trigram.name}（${trigram.image}）：${trigram.meaning}"

@Composable
private fun VoidSummaryCard(summary: String) {
    ResultCard {
        Text("旬空", style = MaterialTheme.typography.titleSmall)
        Text(summary, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
internal fun HexagramLine(
    line: YaoLineDisplay,
    modifier: Modifier = Modifier,
) {
    val lineWidth = 64.dp
    val gapWidth = 10.dp
    val segmentWidth = 27.dp
    val lineHeight = 3.dp

    Row(
        modifier = modifier.width(88.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.width(lineWidth),
            contentAlignment = Alignment.CenterStart,
        ) {
            when (line.shape) {
                YaoLineShape.SOLID -> {
                    LineSegment(
                        modifier = Modifier
                            .width(lineWidth)
                            .height(lineHeight),
                    )
                }
                YaoLineShape.BROKEN -> {
                    Row(
                        modifier = Modifier.width(lineWidth),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        LineSegment(
                            modifier = Modifier
                                .width(segmentWidth)
                                .height(lineHeight),
                        )
                        Spacer(Modifier.width(gapWidth))
                        LineSegment(
                            modifier = Modifier
                                .width(segmentWidth)
                                .height(lineHeight),
                        )
                    }
                }
            }
        }
        if (line.isMoving) {
            Spacer(Modifier.width(6.dp))
            Text(
                text = "动",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun LineSegment(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(1.dp))
            .background(Color.White),
    )
}

@Immutable
private data class YaoCardData(
    val position: YaoPosition,
    val lineDisplay: YaoLineDisplay,
    val yinYang: String,
    val motion: String,
    val changeDescription: String?,
    val sixSpirit: String,
    val sixRelation: String,
    val ganzhi: String,
    val element: String,
    val shiYing: String?,
    val isVoid: Boolean,
    val lineText: String?,
)

internal fun List<Yao>.forResultDisplay(): List<Yao> =
    sortedByDescending { yao -> yao.position.indexFromBottom }

internal fun Hexagram.toDisplayModel(): HexagramDisplayModel = HexagramDisplayModel(
    name = name,
    linesFromBottom = pattern.linesFromBottom.map { it.isYang },
    movingPositions = yaoFromBottom
        .filter(Yao::moving)
        .map { it.position.indexFromBottom }
        .toSet(),
)

private fun Yao.toCardData(originalYao: Yao? = null): YaoCardData = YaoCardData(
    position = position,
    lineDisplay = toLineDisplay(),
    yinYang = yinYang.displayName,
    motion = if (moving) {
        "动爻：${yinYang.displayName}→${yinYang.opposite().displayName}"
    } else {
        "静爻"
    },
    changeDescription = originalYao
        ?.takeIf(Yao::moving)
        ?.let { source -> "对应本卦动爻：${source.yinYang.displayName}→${yinYang.displayName}" },
    sixSpirit = sixSpirit.displayName,
    sixRelation = sixRelation.displayName,
    ganzhi = heavenlyStem.displayName + earthlyBranch.displayName,
    element = element.displayName,
    shiYing = when {
        isShi -> "世爻"
        isYing -> "应爻"
        else -> null
    },
    isVoid = isVoid,
    lineText = if (moving) lineText ?: "爻辞数据不可用" else null,
)

private fun com.boompala.engine.model.YaoPolarity.opposite() =
    if (this == com.boompala.engine.model.YaoPolarity.YANG) {
        com.boompala.engine.model.YaoPolarity.YIN
    } else {
        com.boompala.engine.model.YaoPolarity.YANG
    }

private fun DivinationResult.toVoidSummary(): String {
    val voidLines = yaoFromBottom
        .filter(Yao::isVoid)
        .joinToString { it.position.displayName }
    return "空亡：${voidBranches.joinToString("") { it.displayName }}" +
        if (voidLines.isEmpty()) "（无空亡爻）" else "（$voidLines）"
}
