package com.boompala.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.OutlinedButton
import androidx.wear.compose.material3.Text
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
    isGenerating: Boolean,
    onBack: () -> Unit,
    onGenerate: (HexagramInput) -> Unit,
) {
    val metrics = LocalUiMetrics.current
    var inputMode by remember { mutableStateOf(DivinationInputMode.MANUAL_CAST) }
    var manualNextIndex by remember { mutableStateOf(5) }
    val selectedStates = remember {
        mutableStateListOf<YaoState?>().apply {
            repeat(YaoPosition.entries.size) { add(null) }
        }
    }
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
        DivinationInputMode.MANUAL_CAST -> selectedStates.all { it != null }
        DivinationInputMode.DIRECT_INPUT ->
            originalPolarities.all { it != null } && changedPolarities.all { it != null }
    }

    RotaryScrollColumn(
        rotaryEnabled = rotaryScrollingEnabled,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            horizontal = metrics.horizontalPadding,
            vertical = metrics.verticalPadding,
        ),
        itemSpacing = metrics.itemSpacing,
    ) {
        item(key = "title") {
            Text(
                text = "六爻排盘",
                modifier = Modifier.padding(bottom = metrics.itemSpacing / 4),
            )
        }
        item(key = "input-mode") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(metrics.itemSpacing),
            ) {
                ModeButton(
                    mode = DivinationInputMode.MANUAL_CAST,
                    selectedMode = inputMode,
                    onSelected = { inputMode = it },
                    modifier = Modifier.weight(1f),
                )
                ModeButton(
                    mode = DivinationInputMode.DIRECT_INPUT,
                    selectedMode = inputMode,
                    onSelected = { inputMode = it },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        when (inputMode) {
            DivinationInputMode.MANUAL_CAST -> {
                item(key = "manual-guidance") {
                    Text(
                        text = "从上爻开始依次向下填写至初爻；当前：${YaoPosition.entries[manualNextIndex].displayName}",
                        modifier = Modifier.padding(bottom = metrics.itemSpacing / 2),
                    )
                }
                itemsIndexed(
                    items = YaoPosition.entries.toList().asReversed(),
                    key = { _, position -> "manual-${position.name}" },
                ) { index, position ->
                    val internalIndex = position.indexFromBottom
                    val selected = selectedStates[internalIndex]
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(metrics.itemSpacing),
                    ) {
                        Text(
                            text = position.displayName,
                            modifier = Modifier.weight(0.8f),
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
                    Text("分别输入本卦与变卦，动爻由阴阳差异自动推导")
                }
                item(key = "direct-column-labels") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(metrics.itemSpacing),
                    ) {
                        Text("爻位", modifier = Modifier.weight(0.7f))
                        Text("本卦", modifier = Modifier.weight(1f))
                        Text("变卦", modifier = Modifier.weight(1f))
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
                    ) {
                        Text(position.displayName, modifier = Modifier.weight(0.7f))
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
        item {
            Button(
                onClick = {
                    val castAt = Instant.now()
                    val zoneId = ZoneId.systemDefault()
                    val input = when (inputMode) {
                        DivinationInputMode.MANUAL_CAST -> HexagramInput(
                            linesFromBottom = selectedStates.mapIndexed { index, state ->
                                YaoLineInput(YaoPosition.entries[index], requireNotNull(state))
                            },
                            castAt = castAt,
                            zoneId = zoneId,
                        )

                        DivinationInputMode.DIRECT_INPUT -> DirectHexagramInput(
                            originalLinesFromBottom =
                                originalPolarities.toPolarityInputs(),
                            changedLinesFromBottom =
                                changedPolarities.toPolarityInputs(),
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
                Text(if (isGenerating) "生成中…" else "生成卦盘")
            }
        }
        item {
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
