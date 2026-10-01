package com.boompala.ui.ai

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.boompala.R
import com.boompala.engine.ai.AiChatMessage
import com.boompala.engine.ai.AiChatRequest
import com.boompala.engine.ai.AiDivinationTopic
import com.boompala.engine.ai.AiError
import com.boompala.engine.ai.AiPromptBuilder
import com.boompala.engine.ai.AiStreamEvent
import com.boompala.engine.ai.OpenAiCompatibleClient
import com.boompala.engine.ai.bufferTextDeltas
import com.boompala.engine.model.DivinationResult
import com.boompala.settings.AiNetworkMode
import com.boompala.settings.AppSettings
import com.boompala.ui.AppHaptics
import com.boompala.ui.BoompalaButtonDefaults
import com.boompala.ui.BoompalaCardButton
import com.boompala.ui.ExpressiveShapeMorphingLoader
import com.boompala.ui.ResultCard
import com.boompala.ui.SelectableCardButton
import com.boompala.ui.wearPressFeedback
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

sealed interface AiCardState {
    object Idle : AiCardState
    data class Loading(val topic: AiDivinationTopic, val question: String) : AiCardState
    data class Streaming(val topic: AiDivinationTopic, val question: String, val text: String) : AiCardState
    data class Completed(val topic: AiDivinationTopic, val question: String, val fullText: String) : AiCardState
    data class Error(val topic: AiDivinationTopic, val question: String, val error: AiError) : AiCardState
}

/**
 * 六爻结果页「SI 解卦」卡片。
 *
 * 紧密集成阶段二的 AI Prompt Engine 与 OpenAI-compatible 流式客户端，
 * 支持本地离线防误触、未配置引导、多测事类别选择、推荐问题切换、90ms 流式缓冲渲染及全生命周期取消。
 */
@Composable
fun AiDivinationCard(
    result: DivinationResult,
    settings: AppSettings,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier,
    aiState: AiCardState? = null,
    onStartDivination: ((AiDivinationTopic, String) -> Unit)? = null,
    onCancelDivination: (() -> Unit)? = null,
    onResetState: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedTopic by rememberSaveable { mutableStateOf(AiDivinationTopic.GENERAL) }
    var userQuestion by rememberSaveable { mutableStateOf("") }
    var presetIndex by rememberSaveable { mutableIntStateOf(0) }

    // 本地回退状态（未由页面级状态提升管理时使用）
    var localAiState by remember { mutableStateOf<AiCardState>(AiCardState.Idle) }
    var localActiveJob by remember { mutableStateOf<Job?>(null) }

    val currentAiState = aiState ?: localAiState

    // 仅在未从页面级提升状态时，才在卡片自身卸载时取消网络请求
    if (aiState == null) {
        DisposableEffect(Unit) {
            onDispose {
                localActiveJob?.cancel()
            }
        }
    }

    val presetQuestions = remember(selectedTopic) {
        getPresetQuestionsForTopic(selectedTopic)
    }

    val cyclePresetQuestion: () -> Unit = {
        if (presetQuestions.isNotEmpty()) {
            userQuestion = presetQuestions[presetIndex % presetQuestions.size]
            presetIndex++
            AppHaptics.click(context, settings.hapticIntensity, settings.hapticFeedbackEnabled)
        }
    }

    val startDivination: () -> Unit = {
        val topic = selectedTopic
        val question = userQuestion.trim().ifBlank { "请结合当前卦象综合解读。" }
        if (onStartDivination != null) {
            onStartDivination(topic, question)
        } else {
            if (localActiveJob?.isActive != true) {
                AppHaptics.click(context, settings.hapticIntensity, settings.hapticFeedbackEnabled)
                localAiState = AiCardState.Loading(topic, question)

                localActiveJob = scope.launch {
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
                                        localAiState = AiCardState.Streaming(
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
                                        localAiState = AiCardState.Completed(
                                            topic = topic,
                                            question = question,
                                            fullText = finalText,
                                        )
                                        AppHaptics.success(context, settings.hapticIntensity, settings.hapticFeedbackEnabled)
                                    }
                                    is AiStreamEvent.Error -> {
                                        localAiState = AiCardState.Error(
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
                        localAiState = AiCardState.Error(
                            topic = topic,
                            question = question,
                            error = AiError.Unknown(e.message ?: "未知异常", e),
                        )
                        AppHaptics.click(context, settings.hapticIntensity, settings.hapticFeedbackEnabled)
                    }
                }
            }
        }
    }

    val cancelDivination: () -> Unit = {
        if (onCancelDivination != null) {
            onCancelDivination()
        } else {
            localActiveJob?.cancel()
            localAiState = AiCardState.Idle
        }
    }

    val resetState: () -> Unit = {
        if (onResetState != null) {
            onResetState()
        } else {
            localAiState = AiCardState.Idle
        }
    }

    val cardBorderColor = Color(0xFF9575CD).copy(alpha = 0.65f)

    ResultCard(
        borderColor = cardBorderColor,
        modifier = modifier.defaultMinSize(minHeight = 220.dp),
    ) {
        // 卡片顶部统一标题
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_settings_ai),
                    contentDescription = "SI 解卦",
                    tint = Color(0xFFCE93D8),
                    modifier = Modifier.size(17.dp),
                )
                Text(
                    text = "SI 解卦",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color(0xFFE1BEE7),
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        Spacer(Modifier.height(2.dp))

        // 状态分支判断
        when {
            settings.aiNetworkMode == AiNetworkMode.LOCAL -> {
                LocalModeCardContent(onNavigateToSettings = onNavigateToSettings)
            }
            !settings.isAiConfigured -> {
                UnconfiguredCardContent(onNavigateToSettings = onNavigateToSettings)
            }
            else -> {
                when (val state = currentAiState) {
                    is AiCardState.Idle -> {
                        IdleCardContent(
                            selectedTopic = selectedTopic,
                            onTopicSelected = {
                                selectedTopic = it
                                AppHaptics.click(context, settings.hapticIntensity, settings.hapticFeedbackEnabled)
                            },
                            userQuestion = userQuestion,
                            onQuestionChange = {
                                userQuestion = it
                            },
                            onCyclePreset = cyclePresetQuestion,
                            onClearQuestion = {
                                userQuestion = ""
                                AppHaptics.click(context, settings.hapticIntensity, settings.hapticFeedbackEnabled)
                            },
                            onStartClick = startDivination,
                        )
                    }
                    is AiCardState.Loading -> {
                        LoadingCardContent(
                            topic = state.topic,
                            providerName = settings.aiProvider.displayName,
                        )
                    }
                    is AiCardState.Streaming -> {
                        StreamingCardContent(
                            topic = state.topic,
                            question = state.question,
                            streamingText = state.text,
                            onCancel = cancelDivination,
                        )
                    }
                    is AiCardState.Completed -> {
                        CompletedCardContent(
                            topic = state.topic,
                            question = state.question,
                            fullText = state.fullText,
                            onRestart = resetState,
                        )
                    }
                    is AiCardState.Error -> {
                        ErrorCardContent(
                            topic = state.topic,
                            error = state.error,
                            onRetry = startDivination,
                            onNavigateToSettings = onNavigateToSettings,
                            onBackToIdle = resetState,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LocalModeCardContent(
    onNavigateToSettings: () -> Unit,
) {
    Text(
        text = "🔮 本地纯净模式",
        style = MaterialTheme.typography.labelMedium,
        color = Color(0xFFFFB74D),
        fontWeight = FontWeight.SemiBold,
    )
    Text(
        text = "当前处于离线纯净模式，SI 联网解卦功能已禁用。可在设置中开启联网 SI 模式。",
        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, lineHeight = 15.sp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    val toSettingsInteraction = remember { MutableInteractionSource() }
    BoompalaCardButton(
        onClick = onNavigateToSettings,
        modifier = Modifier
            .fillMaxWidth()
            .wearPressFeedback(toSettingsInteraction),
        interactionSource = toSettingsInteraction,
        colors = BoompalaButtonDefaults.outlinedButtonColors(),
    ) {
        Text("前往设置开启 SI", style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun UnconfiguredCardContent(
    onNavigateToSettings: () -> Unit,
) {
    Text(
        text = "⚡ 尚未配置 SI 秘钥",
        style = MaterialTheme.typography.labelMedium,
        color = Color(0xFFFFCC80),
        fontWeight = FontWeight.SemiBold,
    )
    Text(
        text = "使用 SI 解卦需配置服务商 API Key。可在设置中扫码快速配对导入。",
        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, lineHeight = 15.sp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    val toSettingsInteraction = remember { MutableInteractionSource() }
    BoompalaCardButton(
        onClick = onNavigateToSettings,
        modifier = Modifier
            .fillMaxWidth()
            .wearPressFeedback(toSettingsInteraction),
        interactionSource = toSettingsInteraction,
        colors = BoompalaButtonDefaults.buttonColors(),
    ) {
        Text("前往设置配置", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun IdleCardContent(
    selectedTopic: AiDivinationTopic,
    onTopicSelected: (AiDivinationTopic) -> Unit,
    userQuestion: String,
    onQuestionChange: (String) -> Unit,
    onCyclePreset: () -> Unit,
    onClearQuestion: () -> Unit,
    onStartClick: () -> Unit,
) {
    Text(
        text = "选择此次占问的主题",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
    )

    // 2 列网格紧凑主题选择胶囊（与用神选择保持一致的 Wear 安全宽度）
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        AiDivinationTopic.entries.chunked(2).forEach { rowTopics ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                rowTopics.forEach { topic ->
                    key(topic) {
                        val isSelected = topic == selectedTopic
                        val interaction = remember { MutableInteractionSource() }
                        SelectableCardButton(
                            selected = isSelected,
                            onClick = { onTopicSelected(topic) },
                            modifier = Modifier
                                .weight(1f)
                                .wearPressFeedback(interaction),
                            interactionSource = interaction,
                            contentPadding = BoompalaButtonDefaults.compactContentPadding,
                        ) {
                            Text(
                                text = topic.displayName,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }
    }

    Spacer(Modifier.height(4.dp))

    // 问题区域标题与字数统计
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = "占问问题",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (userQuestion.isNotBlank()) {
            Text(
                text = "${userQuestion.length}/100",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            )
        }
    }

    val focusManager = LocalFocusManager.current
    val isFilled = userQuestion.isNotBlank()
    val borderColor = if (isFilled) Color(0xFFCE93D8).copy(alpha = 0.7f) else Color(0x33FFFFFF)
    val bgColor = if (isFilled) Color(0x1FCE93D8) else Color(0x1AFFFFFF)

    // 输入框：支持点击调起输入法直接打字，也展示选取的推荐问法
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.weight(1f)) {
                BasicTextField(
                    value = userQuestion,
                    onValueChange = { input ->
                        if (input.length <= 100) {
                            onQuestionChange(input)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = TextStyle(
                        color = Color.White,
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                    ),
                    cursorBrush = SolidColor(Color(0xFFCE93D8)),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { focusManager.clearFocus() },
                    ),
                    maxLines = 3,
                    decorationBox = { innerTextField ->
                        if (userQuestion.isEmpty()) {
                            Text(
                                text = "点击输入自定义问题...",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp,
                                    color = Color(0xFF888888),
                                ),
                            )
                        }
                        innerTextField()
                    },
                )
            }
            if (userQuestion.isNotEmpty()) {
                Spacer(Modifier.width(4.dp))
                val clearInteraction = remember { MutableInteractionSource() }
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0x33FFFFFF))
                        .clickable(
                            interactionSource = clearInteraction,
                            indication = null,
                            onClick = onClearQuestion,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "✕",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            color = Color(0xFFBDBDBD),
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                }
            }
        }
    }

    Spacer(Modifier.height(4.dp))

    // 辅助问法操作行
    if (userQuestion.isBlank()) {
        val presetInteraction = remember { MutableInteractionSource() }
        BoompalaCardButton(
            onClick = onCyclePreset,
            modifier = Modifier
                .fillMaxWidth()
                .wearPressFeedback(presetInteraction),
            interactionSource = presetInteraction,
            colors = BoompalaButtonDefaults.outlinedButtonColors(),
            contentPadding = BoompalaButtonDefaults.compactContentPadding,
        ) {
            Text("💡 选取推荐问法", style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp))
        }
    } else {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            val presetInteraction = remember { MutableInteractionSource() }
            BoompalaCardButton(
                onClick = onCyclePreset,
                modifier = Modifier
                    .weight(1f)
                    .wearPressFeedback(presetInteraction),
                interactionSource = presetInteraction,
                colors = BoompalaButtonDefaults.outlinedButtonColors(),
                contentPadding = BoompalaButtonDefaults.compactContentPadding,
            ) {
                Text("💡 换个问法", style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp))
            }

            val clearInteraction = remember { MutableInteractionSource() }
            BoompalaCardButton(
                onClick = onClearQuestion,
                modifier = Modifier
                    .weight(0.6f)
                    .wearPressFeedback(clearInteraction),
                interactionSource = clearInteraction,
                colors = BoompalaButtonDefaults.outlinedButtonColors(),
                contentPadding = BoompalaButtonDefaults.compactContentPadding,
            ) {
                Text("清空", style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp))
            }
        }
    }

    Spacer(Modifier.height(4.dp))

    val startInteraction = remember { MutableInteractionSource() }
    BoompalaCardButton(
        onClick = onStartClick,
        modifier = Modifier
            .fillMaxWidth()
            .wearPressFeedback(startInteraction),
        interactionSource = startInteraction,
        colors = BoompalaButtonDefaults.buttonColors(),
        contentPadding = BoompalaButtonDefaults.compactContentPadding,
    ) {
        Text("⚡ 开始 SI 深度解卦", fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun LoadingCardContent(
    topic: AiDivinationTopic,
    providerName: String,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 180.dp)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "【${topic.displayName}】",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(10.dp))
        ExpressiveShapeMorphingLoader(size = 32.dp)
        Spacer(Modifier.height(10.dp))
        Text(
            text = "正在连线 $providerName 推演...",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = "依周易纳甲六亲严谨起局",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun StreamingCardContent(
    topic: AiDivinationTopic,
    question: String,
    streamingText: String,
    onCancel: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "【${topic.displayName}】· 解卦中",
            style = MaterialTheme.typography.labelMedium,
            color = Color(0xFFCE93D8),
            fontWeight = FontWeight.Bold,
        )
    }

    if (question.isNotBlank() && question != "请结合当前卦象综合解读。") {
        Text(
            text = "问：$question",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
        )
    }

    Spacer(Modifier.height(2.dp))

    // 流式打字文本内容
    Text(
        text = streamingText,
        style = MaterialTheme.typography.bodySmall.copy(
            fontSize = 12.sp,
            lineHeight = 17.sp,
            color = MaterialTheme.colorScheme.onSurface,
        ),
    )

    Spacer(Modifier.height(4.dp))

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = "⚡ 正在推演...",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = MaterialTheme.colorScheme.primary,
        )
    }

    Spacer(Modifier.height(4.dp))

    val cancelInteraction = remember { MutableInteractionSource() }
    BoompalaCardButton(
        onClick = onCancel,
        modifier = Modifier
            .fillMaxWidth()
            .wearPressFeedback(cancelInteraction),
        interactionSource = cancelInteraction,
        colors = BoompalaButtonDefaults.outlinedButtonColors(),
    ) {
        Text("停止生成", style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun CompletedCardContent(
    topic: AiDivinationTopic,
    question: String,
    fullText: String,
    onRestart: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "【${topic.displayName}】· 解卦完成",
            style = MaterialTheme.typography.labelMedium,
            color = Color(0xFF81C784),
            fontWeight = FontWeight.Bold,
        )
    }

    if (question.isNotBlank() && question != "请结合当前卦象综合解读。") {
        Text(
            text = "问：$question",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
        )
    }

    Spacer(Modifier.height(4.dp))

    // 格式化段落排版
    FormattedAiContent(fullText = fullText)

    Spacer(Modifier.height(6.dp))

    val restartInteraction = remember { MutableInteractionSource() }
    BoompalaCardButton(
        onClick = onRestart,
        modifier = Modifier
            .fillMaxWidth()
            .wearPressFeedback(restartInteraction),
        interactionSource = restartInteraction,
        colors = BoompalaButtonDefaults.outlinedButtonColors(),
    ) {
        Text("重新解读", style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun FormattedAiContent(fullText: String) {
    val sections = remember(fullText) {
        fullText.split("\n\n").map { it.trim() }.filter { it.isNotBlank() }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        sections.forEach { section ->
            val lines = section.lines()
            val firstLine = lines.firstOrNull().orEmpty().trim()
            val isHeader = firstLine.startsWith("【") && firstLine.contains("】")

            if (isHeader) {
                val headerTag = firstLine.substringBefore("】") + "】"
                val remainderOfFirstLine = firstLine.substringAfter("】").trim()
                val bodyLines = if (remainderOfFirstLine.isNotBlank()) {
                    listOf(remainderOfFirstLine) + lines.drop(1)
                } else {
                    lines.drop(1)
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = headerTag,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFBA68C8),
                        ),
                    )
                    if (bodyLines.isNotEmpty()) {
                        Text(
                            text = bodyLines.joinToString("\n"),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 12.sp,
                                lineHeight = 17.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                            ),
                        )
                    }
                }
            } else {
                Text(
                    text = section,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                )
            }
        }
    }
}

@Composable
private fun ErrorCardContent(
    topic: AiDivinationTopic,
    error: AiError,
    onRetry: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onBackToIdle: () -> Unit,
) {
    val friendlyMessage = getFriendlyErrorMessage(error)

    Text(
        text = "⚠️ 解卦异常",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.error,
        fontWeight = FontWeight.Bold,
    )
    Text(
        text = friendlyMessage,
        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, lineHeight = 15.sp),
        color = MaterialTheme.colorScheme.onSurface,
    )

    Spacer(Modifier.height(4.dp))

    val retryInteraction = remember { MutableInteractionSource() }
    BoompalaCardButton(
        onClick = onRetry,
        modifier = Modifier
            .fillMaxWidth()
            .wearPressFeedback(retryInteraction),
        interactionSource = retryInteraction,
        colors = BoompalaButtonDefaults.buttonColors(),
    ) {
        Text("重新尝试", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
    }

    if (error is AiError.InvalidApiKey || error is AiError.BadRequest) {
        val settingsInteraction = remember { MutableInteractionSource() }
        BoompalaCardButton(
            onClick = onNavigateToSettings,
            modifier = Modifier
                .fillMaxWidth()
                .wearPressFeedback(settingsInteraction),
            interactionSource = settingsInteraction,
            colors = BoompalaButtonDefaults.outlinedButtonColors(),
        ) {
            Text("SI 设置", style = MaterialTheme.typography.labelSmall)
        }
    }

    val backInteraction = remember { MutableInteractionSource() }
    BoompalaCardButton(
        onClick = onBackToIdle,
        modifier = Modifier
            .fillMaxWidth()
            .wearPressFeedback(backInteraction),
        interactionSource = backInteraction,
        colors = BoompalaButtonDefaults.outlinedButtonColors(),
    ) {
        Text("返回", style = MaterialTheme.typography.labelSmall)
    }
}

private fun getFriendlyErrorMessage(error: AiError): String = when (error) {
    is AiError.InvalidApiKey -> "SI 密钥无效，请检查配置。"
    is AiError.RateLimited -> "SI 服务当前繁忙或额度不足。"
    is AiError.NetworkTimeout -> "连接 SI 服务超时，请稍后再试。"
    is AiError.NetworkUnavailable -> "当前没有可用网络。"
    is AiError.BadRequest -> "SI 请求配置有误，请检查模型设置。"
    is AiError.ServerError -> "SI 服务暂时不可用。"
    is AiError.InvalidResponse -> "SI 服务返回的数据无法解析。"
    is AiError.Unknown -> "SI 解卦暂时失败，请稍后重试。"
}

private fun getPresetQuestionsForTopic(topic: AiDivinationTopic): List<String> = when (topic) {
    AiDivinationTopic.GENERAL -> listOf(
        "结合当前卦象推断近期运势走向",
        "近期需要注意哪些潜在隐患？",
        "当前所处阶段的转折契机在何处？",
    )
    AiDivinationTopic.CAREER -> listOf(
        "近期工作求职或升迁是否顺利？",
        "当前岗位是否有变动或发展机会？",
        "在团队合作中应如何把握进退？",
    )
    AiDivinationTopic.WEALTH -> listOf(
        "近期求财投资是否有收益？",
        "当前项目或生意财运走势如何？",
        "近期是否有破耗需注意防范？",
    )
    AiDivinationTopic.RELATIONSHIP -> listOf(
        "当前感情婚姻缘分发展如何？",
        "近期感情关系是否会有转机？",
        "彼此沟通应注意哪些心结？",
    )
    AiDivinationTopic.HEALTH -> listOf(
        "近期身体健康与平安运势如何？",
        "当下的不适何时能够调理好转？",
        "身心调理需要侧重哪些方面？",
    )
    AiDivinationTopic.STUDY -> listOf(
        "接下来的考试学业能否顺利通过？",
        "进修考证与答辩前景如何？",
        "备考阶段当如何保持心力？",
    )
}
