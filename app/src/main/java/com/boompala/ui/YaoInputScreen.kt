package com.boompala.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.OutlinedButton
import androidx.wear.compose.material3.Text
import com.boompala.engine.liuyao.CoinCastingRecord
import com.boompala.engine.liuyao.CoinSide
import com.boompala.engine.liuyao.CoinTossResult
import com.boompala.engine.liuyao.LiuYaoCoinCastingEngine
import com.boompala.engine.model.DirectHexagramInput
import com.boompala.engine.model.DivinationInputMode
import com.boompala.engine.model.HexagramInput
import com.boompala.engine.model.YaoLineInput
import com.boompala.engine.model.YaoPolarity
import com.boompala.engine.model.YaoPolarityInput
import com.boompala.engine.model.YaoPosition
import com.boompala.engine.model.YaoState
import java.time.Instant
import java.time.ZoneId

@Composable
fun YaoInputScreen(
    rotaryScrollingEnabled: Boolean,
    animationsEnabled: Boolean = true,
    isGenerating: Boolean,
    onBack: () -> Unit,
    onGenerate: (HexagramInput) -> Unit,
) {
    val metrics = LocalUiMetrics.current
    val haptic = LocalHapticFeedback.current

    var inputMode by remember { mutableStateOf(DivinationInputMode.COIN_CAST) }

    // Coin cast state
    val coinRecords = remember {
        mutableStateListOf<CoinCastingRecord?>().apply {
            repeat(YaoPosition.entries.size) { add(null) }
        }
    }
    var lastToss by remember { mutableStateOf<CoinTossResult?>(null) }
    var isTossing by remember { mutableStateOf(false) }
    val tossAnimProgress = remember { androidx.compose.animation.core.Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()

    // Manual cast state
    var manualNextIndex by remember { mutableStateOf(5) }
    val selectedStates = remember {
        mutableStateListOf<YaoState?>().apply {
            repeat(YaoPosition.entries.size) { add(null) }
        }
    }

    // Direct input state
    val originalPolarities = remember {
        mutableStateListOf<YaoPolarity?>().apply {
            repeat(YaoPosition.entries.size) { add(null) }
        }
    }
    val changedPolarities = remember {
        mutableStateListOf<YaoPolarity?>().apply {
            repeat(YaoPosition.entries.size) { add(null) }
        }
    }

    val canGenerate = when (inputMode) {
        DivinationInputMode.COIN_CAST -> coinRecords.all { it != null }
        DivinationInputMode.MANUAL_CAST -> selectedStates.all { it != null }
        DivinationInputMode.DIRECT_INPUT ->
            originalPolarities.all { it != null } && changedPolarities.all { it != null }
    }

    RotaryScrollColumn(
        rotaryEnabled = rotaryScrollingEnabled,
        modifier = Modifier.fillMaxSize(),
        contentPadding = metrics.screenPadding,
        itemSpacing = metrics.itemSpacing,
    ) {
        item(key = "title") {
            Text(
                text = "六爻排盘",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = metrics.itemSpacing / 4),
            )
        }

        item(key = "input-mode") {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    ModeButton(
                        mode = DivinationInputMode.COIN_CAST,
                        selectedMode = inputMode,
                        onSelected = { inputMode = it },
                        modifier = Modifier.weight(1f),
                    )
                    ModeButton(
                        mode = DivinationInputMode.MANUAL_CAST,
                        selectedMode = inputMode,
                        onSelected = { inputMode = it },
                        modifier = Modifier.weight(1f),
                    )
                }
                ModeButton(
                    mode = DivinationInputMode.DIRECT_INPUT,
                    selectedMode = inputMode,
                    onSelected = { inputMode = it },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        when (inputMode) {
            DivinationInputMode.COIN_CAST -> {
                val currentLineIndex = coinRecords.indexOfFirst { it == null }
                val allCoinsCast = currentLineIndex == -1

                item(key = "coin-cast-guidance") {
                    ResultCard {
                        if (!allCoinsCast) {
                            if (currentLineIndex == 0 && lastToss == null) {
                                Text(
                                    text = "三枚铜钱 · 连续摇六次",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                )
                            } else {
                                Text(
                                    text = "【第 ${currentLineIndex + 1} / 6 爻】· ${YaoPosition.entries[currentLineIndex].displayName}",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    text = "投掷三枚铜钱：字(3) / 背(2)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        } else {
                            Text(
                                text = "六爻已全部摇出",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                text = "可点击下方按钮生成完整卦盘。",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                item(key = "coin-visuals") {
                    ResultCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            val coins = lastToss?.coins ?: listOf(null, null, null)
                            coins.forEach { coinSide ->
                                CoinDisplayChip(
                                    side = coinSide,
                                    animProgress = tossAnimProgress.value,
                                )
                            }
                        }

                        if (lastToss != null) {
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "点数：${lastToss!!.coins.joinToString(" + ") { "${it.displayName}(${it.value})" }} = ${lastToss!!.sum}",
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            val stateDesc = when (lastToss!!.sum) {
                                6 -> "老阴（阴爻·动）"
                                7 -> "少阳（阳爻·静）"
                                8 -> "少阴（阴爻·静）"
                                9 -> "老阳（阳爻·动）"
                                else -> lastToss!!.state.displayName
                            }
                            Text(
                                text = "结论：${lastToss!!.sum} $stateDesc",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (lastToss!!.state.isChanging) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }

                item(key = "coin-action-button") {
                    if (!allCoinsCast) {
                        Button(
                            onClick = {
                                if (!isTossing) {
                                    isTossing = true
                                    haptic.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
                                    coroutineScope.launch {
                                        val toss = LiuYaoCoinCastingEngine.castSingleLine()
                                        lastToss = toss
                                        tossAnimProgress.snapTo(0f)
                                        if (animationsEnabled) {
                                            tossAnimProgress.animateTo(
                                                targetValue = 1f,
                                                animationSpec = androidx.compose.animation.core.tween(
                                                    durationMillis = 320,
                                                    easing = androidx.compose.animation.core.FastOutSlowInEasing,
                                                ),
                                            )
                                        } else {
                                            tossAnimProgress.snapTo(1f)
                                        }
                                        coinRecords[currentLineIndex] = CoinCastingRecord(
                                            position = YaoPosition.entries[currentLineIndex],
                                            toss = toss,
                                        )
                                        isTossing = false
                                    }
                                }
                            },
                            enabled = !isTossing,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                if (isTossing) "正在摇卦…"
                                else "摇第 ${currentLineIndex + 1} 爻（${YaoPosition.entries[currentLineIndex].displayName}）"
                            )
                        }
                    } else {
                        OutlinedButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
                                for (i in coinRecords.indices) {
                                    coinRecords[i] = null
                                }
                                lastToss = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("重新摇卦")
                        }
                    }
                }

                item(key = "coin-preview-card") {
                    ResultCard {
                        Text(
                            text = "卦象实时预览",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.height(4.dp))
                        YaoPosition.entries.reversed().forEach { position ->
                            PreviewYaoRow(
                                position = position,
                                record = coinRecords[position.indexFromBottom],
                                isCurrentTarget = position.indexFromBottom == currentLineIndex,
                            )
                        }
                    }
                }
            }

            DivinationInputMode.MANUAL_CAST -> {
                item(key = "manual-guidance") {
                    val allFilled = selectedStates.all { it != null }
                    Text(
                        text = if (allFilled) "六爻已全部填写，可排盘" else "当前待选：${YaoPosition.entries[manualNextIndex].displayName}",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = metrics.itemSpacing / 2),
                    )
                }
                itemsIndexed(
                    items = YaoPosition.entries.toList().asReversed(),
                    key = { _, position -> "manual-${position.name}" },
                ) { _, position ->
                    val internalIndex = position.indexFromBottom
                    val selected = selectedStates[internalIndex]
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(metrics.itemSpacing),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = position.displayName,
                            modifier = Modifier.weight(0.8f),
                            style = MaterialTheme.typography.labelSmall,
                        )
                        Button(
                            onClick = {
                                selectedStates[internalIndex] = nextState(selected)
                                manualNextIndex = selectedStates.indexOfFirst { it == null }.takeIf { it >= 0 } ?: 0
                            },
                            modifier = Modifier
                                .weight(1.2f)
                                .semantics {
                                    contentDescription =
                                        "${position.displayName}，当前${selected?.displayName ?: "未选择"}"
                                },
                        ) {
                            Text(selected?.displayName ?: "未选择")
                        }
                    }
                }
            }

            DivinationInputMode.DIRECT_INPUT -> {
                item(key = "direct-guidance") {
                    Text(
                        text = "分别输入本卦与变卦，动爻由阴阳差异自动推导",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                item(key = "direct-column-labels") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(metrics.itemSpacing),
                    ) {
                        Text("爻位", modifier = Modifier.weight(0.7f), style = MaterialTheme.typography.labelSmall)
                        Text("本卦", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall)
                        Text("变卦", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall)
                    }
                }
                itemsIndexed(
                    items = YaoPosition.entries.toList().asReversed(),
                    key = { _, position -> "direct-${position.name}" },
                ) { _, position ->
                    val internalIndex = position.indexFromBottom
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(metrics.itemSpacing),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            position.displayName,
                            modifier = Modifier.weight(0.7f),
                            style = MaterialTheme.typography.labelSmall,
                        )
                        PolarityButton(
                            position = position,
                            hexagramLabel = "本卦",
                            polarity = originalPolarities[internalIndex],
                            onClick = {
                                originalPolarities[internalIndex] = nextPolarity(originalPolarities[internalIndex])
                            },
                            modifier = Modifier.weight(1f),
                        )
                        PolarityButton(
                            position = position,
                            hexagramLabel = "变卦",
                            polarity = changedPolarities[internalIndex],
                            onClick = {
                                changedPolarities[internalIndex] = nextPolarity(changedPolarities[internalIndex])
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }

        item(key = "generate-button") {
            Button(
                onClick = {
                    val castAt = Instant.now()
                    val zoneId = ZoneId.systemDefault()
                    val input = when (inputMode) {
                        DivinationInputMode.COIN_CAST -> LiuYaoCoinCastingEngine.toHexagramInput(
                            records = coinRecords.filterNotNull(),
                            castAt = castAt,
                            zoneId = zoneId,
                        )

                        DivinationInputMode.MANUAL_CAST -> HexagramInput(
                            linesFromBottom = selectedStates.mapIndexed { index, state ->
                                YaoLineInput(YaoPosition.entries[index], requireNotNull(state))
                            },
                            castAt = castAt,
                            zoneId = zoneId,
                        )

                        DivinationInputMode.DIRECT_INPUT -> DirectHexagramInput(
                            originalLinesFromBottom = originalPolarities.toPolarityInputs(),
                            changedLinesFromBottom = changedPolarities.toPolarityInputs(),
                            castAt = castAt,
                            zoneId = zoneId,
                        ).toEngineInput()
                    }
                    onGenerate(input)
                },
                enabled = canGenerate && !isGenerating,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = metrics.itemSpacing / 2),
            ) {
                Text(if (isGenerating) "排盘中…" else "生成卦盘")
            }
        }

        item(key = "back-button") {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("返回")
            }
        }
    }
}

@Composable
private fun PreviewYaoRow(
    position: YaoPosition,
    record: CoinCastingRecord?,
    isCurrentTarget: Boolean,
) {
    // 注意：不能以 record 是否为空作为 remember 键，否则 record 出现时
    // Animatable 会以 1f 重建，渐显动画永远不会播放。
    val appearProgress = remember {
        androidx.compose.animation.core.Animatable(0f)
    }
    androidx.compose.runtime.LaunchedEffect(record) {
        if (record != null) {
            if (appearProgress.value < 1f) {
                appearProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = androidx.compose.animation.core.spring(
                        dampingRatio = 0.72f,
                        stiffness = 380f,
                    ),
                )
            }
        } else {
            appearProgress.snapTo(0f)
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = if (isCurrentTarget) "▶ ${position.displayName}" else "  ${position.displayName}",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isCurrentTarget) FontWeight.Bold else FontWeight.Normal,
            color = if (isCurrentTarget) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        )
        if (record != null) {
            val state = record.toss.state
            val lineDisplay = YaoLineDisplay(
                polarity = if (state.isYang) YaoPolarity.YANG else YaoPolarity.YIN,
                shape = if (state.isYang) YaoLineShape.SOLID else YaoLineShape.BROKEN,
                isMoving = state.isChanging,
            )
            Box(
                modifier = Modifier.graphicsLayer {
                    alpha = appearProgress.value
                    scaleX = 0.8f + 0.2f * appearProgress.value
                    scaleY = 0.8f + 0.2f * appearProgress.value
                },
            ) {
                HexagramLine(line = lineDisplay)
            }
            Text(
                text = "${record.toss.sum} ${state.displayName}",
                style = MaterialTheme.typography.labelSmall,
                color = if (state.isChanging) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.graphicsLayer {
                    alpha = appearProgress.value
                },
            )
        } else {
            Box(
                modifier = Modifier.width(88.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                Box(
                    modifier = Modifier
                        .width(64.dp)
                        .height(3.dp)
                        .clip(RoundedCornerShape(1.dp))
                        .background(
                            if (isCurrentTarget) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                            else MaterialTheme.colorScheme.surfaceContainerHigh,
                        ),
                )
            }
            Text(
                text = if (isCurrentTarget) "正在摇…" else "待摇",
                style = MaterialTheme.typography.labelSmall,
                color = if (isCurrentTarget) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CoinDisplayChip(
    side: CoinSide?,
    animProgress: Float = 0f,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current.density
    val rotationY = animProgress * 720f
    val tiltX = kotlin.math.sin(animProgress * Math.PI.toFloat()) * 28f
    val scale = 1f + (kotlin.math.sin(animProgress * Math.PI.toFloat()) * 0.22f)

    Box(
        modifier = modifier
            .size(44.dp)
            .graphicsLayer {
                cameraDistance = 16f * density
                this.rotationY = rotationY
                this.rotationX = tiltX
                this.scaleX = scale
                this.scaleY = scale
            }
            .clip(CircleShape)
            .background(
                if (side != null) {
                    if (side == CoinSide.HEADS) Color(0xFF2C2416) else Color(0xFF1E1E1E)
                } else {
                    MaterialTheme.colorScheme.surfaceContainer
                }
            )
            .border(
                width = 1.5.dp,
                color = if (side != null) Color(0xFFC5A059) else MaterialTheme.colorScheme.outline,
                shape = CircleShape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (side != null) {
            Box(
                modifier = Modifier
                    .size(11.dp)
                    .background(Color(0xFF141414))
                    .border(0.8.dp, Color(0xFFC5A059)),
            )

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = side.displayName,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFDFC488),
                )
                Text(
                    text = "${side.value}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFDFC488),
                )
            }
        } else {
            Text(
                text = "●",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ModeButton(
    mode: DivinationInputMode,
    selectedMode: DivinationInputMode,
    onSelected: (DivinationInputMode) -> Unit,
    modifier: Modifier,
) {
    if (mode == selectedMode) {
        Button(onClick = { onSelected(mode) }, modifier = modifier) {
            Text(mode.displayName)
        }
    } else {
        OutlinedButton(onClick = { onSelected(mode) }, modifier = modifier) {
            Text(mode.displayName)
        }
    }
}

@Composable
private fun PolarityButton(
    position: YaoPosition,
    hexagramLabel: String,
    polarity: YaoPolarity?,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.semantics {
            contentDescription =
                "$hexagramLabel${position.displayName}，当前${polarity?.displayName ?: "未选择"}"
        },
    ) {
        Text(polarity?.displayName ?: "未选")
    }
}

private fun List<YaoPolarity?>.toPolarityInputs(): List<YaoPolarityInput> =
    mapIndexed { index, polarity ->
        YaoPolarityInput(
            position = YaoPosition.entries[index],
            polarity = requireNotNull(polarity),
        )
    }

private fun nextState(current: YaoState?): YaoState {
    val states = YaoState.entries
    val nextIndex = if (current == null) 0 else (states.indexOf(current) + 1) % states.size
    return states[nextIndex]
}

private fun nextPolarity(current: YaoPolarity?): YaoPolarity? =
    when (current) {
        null -> YaoPolarity.YANG
        YaoPolarity.YANG -> YaoPolarity.YIN
        YaoPolarity.YIN -> null
    }
