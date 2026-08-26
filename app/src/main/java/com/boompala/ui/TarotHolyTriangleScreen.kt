package com.boompala.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.OutlinedButton
import androidx.wear.compose.material3.Text
import com.boompala.engine.tarot.DeckType
import com.boompala.engine.tarot.DrawnTarotCard
import com.boompala.engine.tarot.TarotEngine
import com.boompala.engine.tarot.TarotOrientation
import com.boompala.engine.tarot.TarotReading
import com.boompala.engine.tarot.TarotSpread

@Composable
fun TarotHolyTriangleScreen(
    engine: TarotEngine,
    reading: TarotReading?,
    rotaryScrollingEnabled: Boolean,
    animationsEnabled: Boolean = true,
    onReadingChanged: (TarotReading?) -> Unit,
    onBack: () -> Unit,
    onArchive: (TarotReading) -> Unit = { },
) {
    val metrics = LocalUiMetrics.current
    val hapticContext = androidx.compose.ui.platform.LocalContext.current
    val spread = TarotSpread.HOLY_TRIANGLE
    var deckType by remember { mutableStateOf(DeckType.FULL_78) }
    var allowReversed by remember { mutableStateOf(true) }

    // Step state during reveal: 0 = slot 0, 1 = slot 1, 2 = slot 2, 3 = full results
    var currentStep by remember(reading) { mutableIntStateOf(0) }
    val flippedList = remember(reading) {
        mutableStateListOf(false, false, false)
    }

    if (reading == null) {
        // Setup Stage
        RotaryScrollColumn(
            rotaryEnabled = rotaryScrollingEnabled,
            modifier = Modifier.fillMaxSize(),
            contentPadding = metrics.screenPadding,
            itemSpacing = metrics.itemSpacing,
        ) {
            item(key = "holy-triangle-title") {
                Text(
                    text = spread.name,
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            item(key = "holy-triangle-guidance") {
                ResultCard {
                    Text(
                        text = spread.description,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            item(key = "holy-triangle-slots-preview") {
                ResultCard {
                    Text(
                        text = "牌位结构",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    spread.slots.forEach { slot ->
                        DetailField(slot.name, slot.description)
                    }
                }
            }

            item(key = "holy-triangle-deck-selection") {
                ResultCard {
                    DetailField("牌组类型", deckType.displayName)
                    val deckInteraction = remember { MutableInteractionSource() }
                    OutlinedButton(
                        onClick = {
                            deckType = if (deckType == DeckType.FULL_78) {
                                DeckType.MAJOR_22
                            } else {
                                DeckType.FULL_78
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .wearPressFeedback(deckInteraction),
                        interactionSource = deckInteraction,
                    ) {
                        Text(if (deckType == DeckType.FULL_78) "切换为仅大牌 (22张)" else "切换为全牌组 (78张)")
                    }
                }
            }

            item(key = "holy-triangle-reversed-selection") {
                ResultCard {
                    DetailField("逆位规则", if (allowReversed) "允许逆位" else "仅正位")
                    val reversedInteraction = remember { MutableInteractionSource() }
                    OutlinedButton(
                        onClick = { allowReversed = !allowReversed },
                        modifier = Modifier
                            .fillMaxWidth()
                            .wearPressFeedback(reversedInteraction),
                        interactionSource = reversedInteraction,
                    ) {
                        Text(if (allowReversed) "切换为仅正位" else "切换为允许逆位")
                    }
                }
            }

            item(key = "holy-triangle-draw-action") {
                val drawInteraction = remember { MutableInteractionSource() }
                Button(
                    onClick = {
                        val newReading = engine.cast(
                            spread = spread,
                            deckType = deckType,
                            allowReversed = allowReversed,
                        )
                        currentStep = 0
                        flippedList.clear()
                        repeat(3) { flippedList.add(false) }
                        onReadingChanged(newReading)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .wearPressFeedback(drawInteraction),
                    interactionSource = drawInteraction,
                ) {
                    Text("洗牌并抽牌")
                }
            }

            item(key = "holy-triangle-back-home") {
                val backInteraction = remember { MutableInteractionSource() }
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier
                        .fillMaxWidth()
                        .wearPressFeedback(backInteraction),
                    interactionSource = backInteraction,
                ) {
                    Text("返回首页")
                }
            }
        }
    } else if (currentStep < 3 && reading.drawnCards.size >= 3) {
        // Step-by-Step Reveal Stage
        val drawnCard = reading.drawnCards[currentStep]
        val isCardFlipped = flippedList[currentStep]
        val isReversed = drawnCard.orientation == TarotOrientation.REVERSED

        RotaryScrollColumn(
            rotaryEnabled = rotaryScrollingEnabled,
            modifier = Modifier.fillMaxSize(),
            contentPadding = metrics.screenPadding,
            itemSpacing = metrics.itemSpacing,
        ) {
            item(key = "step-header-$currentStep") {
                Text(
                    text = "【${drawnCard.slot.name}】（第 ${currentStep + 1}/3 张）",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            item(key = "step-card-$currentStep") {
                TarotHolyTriangleFlipItem(
                    drawnCard = drawnCard,
                    isFlipped = isCardFlipped,
                    animationsEnabled = animationsEnabled,
                    onFlip = {
                        if (!isCardFlipped) {
                            AppHaptics.click(hapticContext)
                            flippedList[currentStep] = true
                        }
                    },
                )
            }

            if (!isCardFlipped) {
                item(key = "step-hint-$currentStep") {
                    Text(
                        text = "▲ 点击牌背揭晓「${drawnCard.slot.name}」",
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            } else {
                item(key = "step-revealed-summary-$currentStep") {
                    ResultCard {
                        Text(
                            text = "${drawnCard.card.nameZh} · ${if (isReversed) "逆位" else "正位"}",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = drawnCard.slot.description,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }

                item(key = "step-next-action-$currentStep") {
                    val nextInteraction = remember { MutableInteractionSource() }
                    Button(
                        onClick = {
                            if (currentStep < 2) {
                                currentStep++
                            } else {
                                currentStep = 3 // enter full interpretation view
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .wearPressFeedback(nextInteraction),
                        interactionSource = nextInteraction,
                    ) {
                        Text(if (currentStep < 2) "揭晓下一张 (${reading.drawnCards[currentStep + 1].slot.name})" else "查看圣三角完整解读")
                    }
                }
            }
        }
    } else {
        // Full Results Stage: RotaryScrollColumn showing all 3 cards with comprehensive interpretation
        RotaryScrollColumn(
            rotaryEnabled = rotaryScrollingEnabled,
            modifier = Modifier.fillMaxSize(),
            contentPadding = metrics.screenPadding,
            itemSpacing = metrics.itemSpacing,
        ) {
            item(key = "holy-triangle-results-title") {
                Text(
                    text = "圣三角完整解读",
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // Overview Card of the 3 slots
            item(key = "holy-triangle-overview") {
                ResultCard {
                    Text(
                        text = "圣三角概览",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        reading.drawnCards.forEach { drawn ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.weight(1f),
                            ) {
                                Text(
                                    text = drawn.slot.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    text = drawn.card.nameZh,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                                Text(
                                    text = if (drawn.orientation == TarotOrientation.REVERSED) "逆位" else "正位",
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            }
                        }
                    }
                }
            }

            // Detailed Cards for Slot 0 (现状), Slot 1 (阻碍与根源), Slot 2 (对策与建议)
            reading.drawnCards.forEachIndexed { index, drawnCard ->
                val card = drawnCard.card
                val isReversed = drawnCard.orientation == TarotOrientation.REVERSED
                val meanings = if (isReversed) card.reversedMeanings else card.uprightMeanings

                item(key = "card-header-$index") {
                    ResultCard {
                        Text(
                            text = "【${drawnCard.slot.name}】· ${card.nameZh} (${card.nameEn})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        DetailField(
                            "状态",
                            if (isReversed) "逆位 (Reversed)" else "正位 (Upright)",
                        )
                        DetailField("牌位意义", drawnCard.slot.description)
                        DetailField("卡牌属性", "${card.arcana.displayName} · ${card.element.displayName}")
                    }
                }

                item(key = "card-image-$index") {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Image(
                            painter = painterResource(TarotImageAssets.cardDrawableRes(card)),
                            contentDescription = card.nameZh,
                            modifier = Modifier
                                .size(width = 80.dp, height = 140.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .graphicsLayer {
                                    if (isReversed) {
                                        rotationZ = 180f
                                    }
                                },
                        )
                    }
                }

                item(key = "card-keywords-$index") {
                    ResultCard {
                        Text(
                            text = "关键词",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = card.keywordsZh.joinToString(" · "),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }

                item(key = "card-meaning-$index") {
                    ResultCard {
                        Text(
                            text = if (isReversed) "逆位核心释义" else "正位核心释义",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                        meanings.forEach { meaning ->
                            Text(
                                text = "• $meaning",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }

                if (card.fortuneTelling.isNotEmpty()) {
                    item(key = "card-fortune-$index") {
                        ResultCard {
                            Text(
                                text = "指引断语",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                            )
                            card.fortuneTelling.forEach { line ->
                                Text(
                                    text = "• $line",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    }
                }
            }

            item(key = "holy-triangle-archive") {
                val archiveInteraction = remember { MutableInteractionSource() }
                OutlinedButton(
                    onClick = { onArchive(reading) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .wearPressFeedback(archiveInteraction),
                    interactionSource = archiveInteraction,
                ) {
                    Text("归档此结果")
                }
            }

            item(key = "holy-triangle-recast") {
                val recastInteraction = remember { MutableInteractionSource() }
                Button(
                    onClick = {
                        currentStep = 0
                        flippedList.clear()
                        repeat(3) { flippedList.add(false) }
                        onReadingChanged(null)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .wearPressFeedback(recastInteraction),
                    interactionSource = recastInteraction,
                ) {
                    Text("重新抽牌")
                }
            }

            item(key = "holy-triangle-back-home") {
                val finishInteraction = remember { MutableInteractionSource() }
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier
                        .fillMaxWidth()
                        .wearPressFeedback(finishInteraction),
                    interactionSource = finishInteraction,
                ) {
                    Text("返回首页")
                }
            }
        }
    }
}

@Composable
private fun TarotHolyTriangleFlipItem(
    drawnCard: DrawnTarotCard,
    isFlipped: Boolean,
    animationsEnabled: Boolean = true,
    onFlip: () -> Unit,
) {
    val card = drawnCard.card
    val isReversed = drawnCard.orientation == TarotOrientation.REVERSED

    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = if (animationsEnabled) {
            tween(durationMillis = 500, easing = FastOutSlowInEasing)
        } else {
            snap()
        },
        label = "TarotHolyTriangleFlipAnimation",
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(width = 96.dp, height = 168.dp)
                .graphicsLayer {
                    rotationY = rotation
                    cameraDistance = 12f * density
                }
                .clip(RoundedCornerShape(8.dp))
                .clickable(enabled = !isFlipped) { onFlip() },
            contentAlignment = Alignment.Center,
        ) {
            if (rotation <= 90f) {
                // Card Back
                Image(
                    painter = painterResource(TarotImageAssets.cardBackResId),
                    contentDescription = "塔罗牌背",
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                // Card Face (with mirror flip cancellation and orientation rotation)
                Image(
                    painter = painterResource(TarotImageAssets.cardDrawableRes(card)),
                    contentDescription = card.nameZh,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            rotationY = 180f
                            if (isReversed) {
                                rotationZ = 180f
                            }
                        },
                )
            }
        }
    }
}
