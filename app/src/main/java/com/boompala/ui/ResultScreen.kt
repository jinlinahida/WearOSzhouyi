package com.boompala.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.OutlinedButton
import androidx.wear.compose.material3.Text
import com.boompala.engine.data.HexagramInterpretation
import com.boompala.engine.model.Hexagram
import com.boompala.engine.model.Yao
import com.boompala.engine.model.YaoPosition
import com.boompala.archive.AiArchiveData
import com.boompala.engine.model.DivinationResult
import com.boompala.engine.rules.YongShenCategory
import com.boompala.engine.rules.YongShenEvaluator
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.boompala.engine.ai.AiChatMessage
import com.boompala.engine.ai.AiChatRequest
import com.boompala.engine.ai.AiDivinationTopic
import com.boompala.engine.ai.AiError
import com.boompala.engine.ai.AiPromptBuilder
import com.boompala.engine.ai.AiStreamEvent
import com.boompala.engine.ai.OpenAiCompatibleClient
import com.boompala.engine.ai.bufferTextDeltas
import com.boompala.settings.AppSettings
import com.boompala.ui.ai.AiCardState
import com.boompala.ui.ai.AiDivinationCard
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import java.util.Locale

private val ResultCardShape = RoundedCornerShape(12.dp)
private val ResultCardColor = Color(0xFF1B1B1B)

@Composable
fun LiuYaoResultContent(
    reading: GeneratedReading,
    rotaryScrollingEnabled: Boolean,
    animationsEnabled: Boolean = true,
    settings: AppSettings = AppSettings.DEFAULT,
    onNavigateToSettings: () -> Unit = {},
    onBack: () -> Unit,
    onArchive: (DivinationResult, AiArchiveData?) -> Unit = { _, _ -> },
) {
    val context = LocalContext.current
    val metrics = LocalUiMetrics.current
    val result = reading.result
    val dateTimeFormatter = remember {
        DateTimeFormatter.ofPattern("yyyy年M月d日 HH:mm", Locale.CHINA)
    }
    val castTime = remember(result.timeInfo.gregorianDateTime) {
        dateTimeFormatter.format(result.timeInfo.gregorianDateTime)
    }
    var selectedYongShen by rememberSaveable { mutableStateOf(YongShenCategory.SHI_YAO) }
    val yongShenEval = remember(result, selectedYongShen) {
        YongShenEvaluator.evaluate(result, selectedYongShen)
    }

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var aiState by remember { mutableStateOf<AiCardState>(AiCardState.Idle) }
    var activeAiJob by remember { mutableStateOf<Job?>(null) }

    // 页面完全离开或退出时才取消后台网络流式请求
    DisposableEffect(Unit) {
        onDispose {
            activeAiJob?.cancel()
        }
    }

    val startAiDivination: (AiDivinationTopic, String) -> Unit = { topic, question ->
        if (activeAiJob?.isActive != true) {
            AppHaptics.click(context, settings.hapticIntensity, settings.hapticFeedbackEnabled)
            aiState = AiCardState.Loading(topic, question)

            // 自动平滑滚动至 AI 卡片位置（index = 2），确保在手表小屏上居中完全可见
            scope.launch {
                listState.animateScrollToItem(index = 2)
            }

            activeAiJob = scope.launch {
                try {
                    val prompt = AiPromptBuilder.buildPrompt(
                        result = result,
                        topic = topic,
                        question = question,
                        userGender = settings.userGender,
                    )

                    val request = AiChatRequest(
                        model = settings.effectiveAiModel,
                        messages = listOf(
                            AiChatMessage(role = "system", content = prompt.systemPrompt),
                            AiChatMessage(role = "user", content = prompt.userPrompt),
                        ),
                        temperature = 0.7,
                        maxTokens = 1000,
                    )

                    val client = OpenAiCompatibleClient()
                    val fullAccumulator = StringBuilder()

                    client.streamChat(
                        baseUrl = settings.effectiveAiBaseUrl,
                        apiKey = settings.aiApiKey,
                        request = request,
                    )
                        .bufferTextDeltas(windowMs = 90L)
                        .collect { event ->
                            when (event) {
                                is AiStreamEvent.TextDelta -> {
                                    fullAccumulator.append(event.text)
                                    aiState = AiCardState.Streaming(
                                        topic = topic,
                                        question = question,
                                        text = fullAccumulator.toString(),
                                    )
                                }
                                is AiStreamEvent.Completed -> {
                                    val finalText = if (fullAccumulator.isNotEmpty()) {
                                        fullAccumulator.toString()
                                    } else {
                                        event.fullText
                                    }
                                    aiState = AiCardState.Completed(
                                        topic = topic,
                                        question = question,
                                        fullText = finalText,
                                    )
                                    AppHaptics.success(context, settings.hapticIntensity, settings.hapticFeedbackEnabled)
                                }
                                is AiStreamEvent.Error -> {
                                    aiState = AiCardState.Error(
                                        topic = topic,
                                        question = question,
                                        error = event.error,
                                    )
                                    AppHaptics.click(context, settings.hapticIntensity, settings.hapticFeedbackEnabled)
                                }
                            }
                        }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Throwable) {
                    aiState = AiCardState.Error(
                        topic = topic,
                        question = question,
                        error = AiError.Unknown(e.message ?: "未知异常", e),
                    )
                    AppHaptics.click(context, settings.hapticIntensity, settings.hapticFeedbackEnabled)
                }
            }
        }
    }

    val cancelAiDivination: () -> Unit = {
        activeAiJob?.cancel()
        aiState = AiCardState.Idle
    }

    val resetAiState: () -> Unit = {
        aiState = AiCardState.Idle
    }

    val originalYaoCards = remember(result.original, selectedYongShen, yongShenEval) {
        result.yaoFromBottom
            .forResultDisplay()
            .map { yao ->
                yao.toCardData(
                    isYongShenTarget = yongShenEval?.targetPosition == yao.position && (!yongShenEval.isFuShen),
                )
            }
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
        state = listState,
    ) {
        // The line-card order is intentionally unchanged from the former
        // ResultScreen: shared chrome owns only the Wear scrolling shell.
        item(key = "calendar-time") {
            ResultCard {
                DetailField("公历", castTime)
                DetailField("农历", result.timeInfo.lunarDate)
            }
        }
        item(key = "ai-divination") {
            AiDivinationCard(
                result = result,
                settings = settings,
                onNavigateToSettings = onNavigateToSettings,
                aiState = aiState,
                onStartDivination = startAiDivination,
                onCancelDivination = cancelAiDivination,
                onResetState = resetAiState,
            )
        }
        item(key = "yongshen-selector") {
            ResultCard {
                Text(
                    text = "用神选择",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                )
                Spacer(Modifier.height(4.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    YongShenCategory.entries.chunked(2).forEach { pair ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            pair.forEach { category ->
                                val selected = category == selectedYongShen
                                val pressInteraction = remember { MutableInteractionSource() }
                                SelectableCardButton(
                                    selected = selected,
                                    onClick = { selectedYongShen = category },
                                    modifier = Modifier
                                        .weight(1f)
                                        .wearPressFeedback(pressInteraction),
                                    interactionSource = pressInteraction,
                                ) {
                                    Text(
                                        text = category.displayName,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                        maxLines = 1,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        yongShenEval?.let { eval ->
            item(key = "yongshen-evaluation") {
                ResultCard(
                    borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                ) {
                    Text(
                        text = "用神 · ${eval.category.displayName}",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(eval.summary, style = MaterialTheme.typography.bodySmall)
                }
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
            Text(movingSummary, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
        }
        item(key = "original-yao-section-title") {
            Text("本卦装卦", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
        items(
            items = originalYaoCards,
            key = { card -> "original-${card.position.indexFromBottom}" },
        ) { card ->
            YaoDetailCard(card, animationsEnabled)
        }
        result.changed?.let { changed ->
            item(key = "changed-yao-section-title") {
                Text(
                    text = "变卦装卦 · ${changed.name}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
            items(
                items = changedYaoCards,
                key = { card -> "changed-${card.position.indexFromBottom}" },
            ) { card ->
                YaoDetailCard(card, animationsEnabled)
            }
        }
        item(key = "void-summary") {
            VoidSummaryCard(voidSummary)
        }
        item(key = "archive") {
            val archiveInteraction = remember { MutableInteractionSource() }
            val completedAiData = (aiState as? AiCardState.Completed)?.let {
                AiArchiveData(
                    topic = it.topic.displayName,
                    question = it.question,
                    fullText = it.fullText,
                )
            }
            BoompalaCardButton(
                onClick = { onArchive(result, completedAiData) },
                modifier = Modifier
                    .fillMaxWidth()
                    .wearPressFeedback(archiveInteraction),
                interactionSource = archiveInteraction,
                colors = BoompalaButtonDefaults.outlinedButtonColors(),
            ) {
                Text("归档此次结果")
            }
        }
        item(key = "back") {
            val backInteraction = remember { MutableInteractionSource() }
            BoompalaCardButton(
                onClick = onBack,
                modifier = Modifier
                    .fillMaxWidth()
                    .wearPressFeedback(backInteraction),
                interactionSource = backInteraction,
                colors = BoompalaButtonDefaults.outlinedButtonColors(),
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
        if (hexagram.statuses.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                hexagram.statuses.forEach { status ->
                    Text(
                        text = "· ${status.displayName}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
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
private fun YaoDetailCard(card: YaoCardData, animationsEnabled: Boolean = true) {
    val isMoving = card.lineDisplay.isMoving
    val isYongShen = card.isYongShenTarget
    val glowModifier = when {
        isYongShen -> Modifier.border(
            width = 1.5.dp,
            color = Color(0xFFFFD700),
            shape = ResultCardShape,
        )
        isMoving -> if (animationsEnabled) {
            val infiniteTransition = rememberInfiniteTransition(label = "MovingYaoGlow")
            val glowAlpha by infiniteTransition.animateFloat(
                initialValue = 0.35f,
                targetValue = 0.95f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1200, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse,
                ),
                label = "glowAlpha",
            )
            Modifier.border(
                width = 1.2.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = glowAlpha),
                shape = ResultCardShape,
            )
        } else {
            Modifier.border(
                width = 1.2.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                shape = ResultCardShape,
            )
        }
        else -> Modifier
    }

    ResultCard(modifier = glowModifier) {
        HexagramLine(card.lineDisplay)
        Text(
            "${card.position.displayName} · ${card.yinYang} · ${card.motion}" + if (isYongShen) " [用神]" else "",
            style = MaterialTheme.typography.titleSmall,
            color = if (isYongShen) Color(0xFFFFD700) else if (isMoving) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
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
        card.fuShenDisplay?.let { fuShenText ->
            DetailField(label = "伏神", value = fuShenText)
        }
        if (card.statusBadges.isNotEmpty()) {
            DetailField(label = "神煞/状态", value = card.statusBadges.joinToString(" · "))
        }
        if (card.isVoid) {
            DetailField(label = "旬空", value = "空亡")
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
            val expandInteraction = remember { MutableInteractionSource() }
            BoompalaCardButton(
                onClick = { expanded = !expanded },
                modifier = Modifier
                    .fillMaxWidth()
                    .wearPressFeedback(expandInteraction),
                interactionSource = expandInteraction,
                colors = BoompalaButtonDefaults.outlinedButtonColors(),
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
    val fuShenDisplay: String?,
    val statusBadges: List<String>,
    val isYongShenTarget: Boolean,
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

private fun Yao.toCardData(
    originalYao: Yao? = null,
    isYongShenTarget: Boolean = false,
): YaoCardData = YaoCardData(
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
    fuShenDisplay = fuShen?.let { "${it.displayName}(${it.feiFuRelation.displayName})" },
    statusBadges = statuses.map { it.displayName },
    isYongShenTarget = isYongShenTarget,
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
        if (voidLines.isEmpty()) " · 无空爻" else " · $voidLines"
}
