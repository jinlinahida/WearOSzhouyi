package com.boompala.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.boompala.engine.tarot.DeckType
import com.boompala.engine.tarot.DrawnTarotCard
import com.boompala.engine.tarot.TarotEngine
import com.boompala.engine.tarot.TarotOrientation
import com.boompala.engine.tarot.TarotReading
import com.boompala.engine.tarot.TarotSpread
import kotlin.math.absoluteValue
import kotlinx.coroutines.launch

@Composable
fun TarotCelticCrossScreen(
    engine: TarotEngine,
    reading: TarotReading?,
    rotaryScrollingEnabled: Boolean,
    animationsEnabled: Boolean = true,
    onReadingChanged: (TarotReading?) -> Unit,
    onBack: () -> Unit,
    onInnerBackAvailabilityChanged: (Boolean) -> Unit = {},
    onArchive: (TarotReading) -> Unit = { },
) {
    val metrics = LocalUiMetrics.current
    val hapticContext = androidx.compose.ui.platform.LocalContext.current
    val hapticEnabled = LocalHapticFeedbackEnabled.current
    val hapticIntensity = LocalHapticIntensity.current
    val spread = TarotSpread.CELTIC_CROSS
    var deckType by remember { mutableStateOf(DeckType.FULL_78) }
    var allowReversed by remember { mutableStateOf(true) }

    // Sequential step during flipping: 0..9 for slots 0..9, 10 for full results
    var currentStep by remember(reading) { mutableIntStateOf(0) }
    val flippedList = remember(reading) {
        mutableStateListOf<Boolean>().apply {
            repeat(10) { add(false) }
        }
    }

    // Inform outer host whether inner back is available (to suspend swipe-to-dismiss)
    LaunchedEffect(currentStep, reading) {
        val hasInnerBack = reading != null && currentStep > 0
        onInnerBackAvailabilityChanged(hasInnerBack)
    }

    // Inner back handler: step back to previous card during reveal or results
    BackHandler(enabled = reading != null && currentStep > 0) {
        if (currentStep == 10) {
            currentStep = 9 // Return to 10th card revealing step
        } else {
            currentStep--   // Return to previous card
        }
    }

    if (reading == null) {
        // Setup Stage: Streamlined without redundant lengthy description
        RotaryScrollColumn(
            rotaryEnabled = rotaryScrollingEnabled,
            modifier = Modifier.fillMaxSize(),
            contentPadding = metrics.screenPadding,
            itemSpacing = metrics.itemSpacing,
        ) {
            item(key = "celtic-cross-title") {
                Text(
                    text = spread.name,
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            item(key = "celtic-cross-deck-selection") {
                ResultCard {
                    DetailField("牌组类型", deckType.displayName)
                    val deckInteraction = remember { MutableInteractionSource() }
                    BoompalaCardButton(
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
                        colors = BoompalaButtonDefaults.outlinedButtonColors(),
                    ) {
                        Text(if (deckType == DeckType.FULL_78) "切换为仅大牌 (22张)" else "切换为全牌组 (78张)")
                    }
                }
            }

            item(key = "celtic-cross-reversed-selection") {
                ResultCard {
                    DetailField("逆位规则", if (allowReversed) "允许逆位" else "仅正位")
                    val reversedInteraction = remember { MutableInteractionSource() }
                    BoompalaCardButton(
                        onClick = { allowReversed = !allowReversed },
                        modifier = Modifier
                            .fillMaxWidth()
                            .wearPressFeedback(reversedInteraction),
                        interactionSource = reversedInteraction,
                        colors = BoompalaButtonDefaults.outlinedButtonColors(),
                    ) {
                        Text(if (allowReversed) "切换为仅正位" else "切换为允许逆位")
                    }
                }
            }

            item(key = "celtic-cross-draw-action") {
                val drawInteraction = remember { MutableInteractionSource() }
                BoompalaCardButton(
                    onClick = {
                        val newReading = engine.cast(
                            spread = spread,
                            deckType = deckType,
                            allowReversed = allowReversed,
                        )
                        currentStep = 0
                        flippedList.clear()
                        repeat(10) { flippedList.add(false) }
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

            item(key = "celtic-cross-back-home") {
                val backInteraction = remember { MutableInteractionSource() }
                BoompalaCardButton(
                    onClick = onBack,
                    modifier = Modifier
                        .fillMaxWidth()
                        .wearPressFeedback(backInteraction),
                    interactionSource = backInteraction,
                    colors = BoompalaButtonDefaults.outlinedButtonColors(),
                ) {
                    Text("返回首页")
                }
            }
        }
    } else if (currentStep < 10 && reading.drawnCards.size >= 10) {
        // Step-by-Step Card Revealing Stage (Slot 0..9) with optimized typography and inner back
        val drawnCard = reading.drawnCards[currentStep]
        val isCardFlipped = flippedList.getOrElse(currentStep) { false }
        val isReversed = drawnCard.orientation == TarotOrientation.REVERSED

        RotaryScrollColumn(
            rotaryEnabled = rotaryScrollingEnabled,
            modifier = Modifier.fillMaxSize(),
            contentPadding = metrics.screenPadding,
            itemSpacing = metrics.itemSpacing,
        ) {
            item(key = "step-header-$currentStep") {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "第 ${currentStep + 1} / 10 张",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "【${drawnCard.slot.name}】",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            item(key = "step-card-$currentStep") {
                TarotCelticCrossFlipItem(
                    drawnCard = drawnCard,
                    isFlipped = isCardFlipped,
                    animationsEnabled = animationsEnabled,
                    onFlip = {
                        if (!isCardFlipped) {
                            AppHaptics.cardFlip(hapticContext, intensity = hapticIntensity, enabled = hapticEnabled)
                            if (currentStep < flippedList.size) {
                                flippedList[currentStep] = true
                            }
                        }
                    },
                )
            }

            if (!isCardFlipped) {
                item(key = "step-hint-$currentStep") {
                    Text(
                        text = "▲ 点击牌背揭晓",
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
                    BoompalaCardButton(
                        onClick = {
                            if (currentStep < 9) {
                                currentStep++
                            } else {
                                currentStep = 10 // enter full interpretation view
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .wearPressFeedback(nextInteraction),
                        interactionSource = nextInteraction,
                    ) {
                        Text(
                            if (currentStep < 9) {
                                "揭晓下一张 (${reading.drawnCards[currentStep + 1].slot.name})"
                            } else {
                                "查看十牌完整解读"
                            },
                        )
                    }
                }
            }

            // Return to previous card action if not at first card
            if (currentStep > 0) {
                item(key = "step-prev-action-$currentStep") {
                    val prevInteraction = remember { MutableInteractionSource() }
                    BoompalaCardButton(
                        onClick = { currentStep-- },
                        modifier = Modifier
                            .fillMaxWidth()
                            .wearPressFeedback(prevInteraction),
                        interactionSource = prevInteraction,
                        colors = BoompalaButtonDefaults.outlinedButtonColors(),
                    ) {
                        Text("返回上一张")
                    }
                }
            }

            // Quick skip to full reading option
            item(key = "step-skip-all-$currentStep") {
                val skipInteraction = remember { MutableInteractionSource() }
                BoompalaCardButton(
                    onClick = {
                        for (i in 0 until 10) {
                            if (i < flippedList.size) flippedList[i] = true
                        }
                        currentStep = 10
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .wearPressFeedback(skipInteraction),
                    interactionSource = skipInteraction,
                    colors = BoompalaButtonDefaults.outlinedButtonColors(),
                ) {
                    Text("直接查看完整解读")
                }
            }
        }
    } else {
        // Full Results Stage: Card Stack with Horizontal Pager + Linked Detailed Interpretation
        val pagerState = rememberPagerState(initialPage = 0, pageCount = { 10 })
        val coroutineScope = rememberCoroutineScope()
        val selectedCard = reading.drawnCards[pagerState.currentPage]
        val isSelectedReversed = selectedCard.orientation == TarotOrientation.REVERSED
        val selectedMeanings = if (isSelectedReversed) selectedCard.card.reversedMeanings else selectedCard.card.uprightMeanings

        RotaryScrollColumn(
            rotaryEnabled = rotaryScrollingEnabled,
            modifier = Modifier.fillMaxSize(),
            contentPadding = metrics.screenPadding,
            itemSpacing = metrics.itemSpacing,
        ) {
            item(key = "results-title") {
                Text(
                    text = "凯尔特十字结果",
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // Persistent Overview Section 1: 核心十字区 (Slots 0..5) with clickable pills
            item(key = "results-overview-cross") {
                ResultCard {
                    Text(
                        text = "核心十字区 (1~6)",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    reading.drawnCards.take(6).forEachIndexed { idx, drawn ->
                        val isCurrent = pagerState.currentPage == idx
                        val isRev = drawn.orientation == TarotOrientation.REVERSED
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isCurrent) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else Color.Transparent)
                                .clickable {
                                    coroutineScope.launch { pagerState.animateScrollToPage(idx) }
                                }
                                .padding(horizontal = 4.dp, vertical = 3.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "${idx + 1}.${drawn.slot.name}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1.1f, fill = false),
                            )
                            Text(
                                text = "${drawn.card.nameZh}(${if (isRev) "逆" else "正"})",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }

            // Persistent Overview Section 2: 权杖柱区 (Slots 6..9) with clickable pills
            item(key = "results-overview-staff") {
                ResultCard {
                    Text(
                        text = "权杖柱区 (7~10)",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    reading.drawnCards.drop(6).take(4).forEachIndexed { dropIdx, drawn ->
                        val actualIdx = dropIdx + 6
                        val isCurrent = pagerState.currentPage == actualIdx
                        val isRev = drawn.orientation == TarotOrientation.REVERSED
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isCurrent) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else Color.Transparent)
                                .clickable {
                                    coroutineScope.launch { pagerState.animateScrollToPage(actualIdx) }
                                }
                                .padding(horizontal = 4.dp, vertical = 3.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "${actualIdx + 1}.${drawn.slot.name}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1.1f, fill = false),
                            )
                            Text(
                                text = "${drawn.card.nameZh}(${if (isRev) "逆" else "正"})",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }

            // Card Stack Carousel using HorizontalPager
            item(key = "results-card-stack-carousel") {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(176.dp),
                        contentPadding = PaddingValues(horizontal = 48.dp),
                        pageSpacing = 8.dp,
                    ) { page ->
                        val drawn = reading.drawnCards[page]
                        val card = drawn.card
                        val isRev = drawn.orientation == TarotOrientation.REVERSED
                        val pageOffset = (pagerState.currentPage - page + pagerState.currentPageOffsetFraction).absoluteValue
                        val scale = lerp(0.82f, 1.0f, 1f - pageOffset.coerceIn(0f, 1f))
                        val alpha = lerp(0.55f, 1.0f, 1f - pageOffset.coerceIn(0f, 1f))

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                    this.alpha = alpha
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Image(
                                painter = painterResource(TarotImageAssets.cardDrawableRes(card)),
                                contentDescription = card.nameZh,
                                modifier = Modifier
                                    .size(width = 90.dp, height = 158.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(
                                        width = if (page == pagerState.currentPage) 1.5.dp else 0.dp,
                                        color = if (page == pagerState.currentPage) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        shape = RoundedCornerShape(8.dp),
                                    )
                                    .graphicsLayer {
                                        if (isRev) {
                                            rotationZ = 180f
                                        }
                                    },
                            )
                        }
                    }

                    Text(
                        text = "◄ 左右滑动切牌 (${pagerState.currentPage + 1}/10) ►",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }

            // Linked Detailed Interpretation Section (Dynamically reflects pagerState.currentPage)
            item(key = "card-detail-header-${pagerState.currentPage}") {
                ResultCard {
                    Text(
                        text = "【${pagerState.currentPage + 1}. ${selectedCard.slot.name}】",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "${selectedCard.card.nameZh} (${selectedCard.card.nameEn})",
                        style = MaterialTheme.typography.labelMedium,
                    )
                    DetailField(
                        "状态",
                        if (isSelectedReversed) "逆位 (Reversed)" else "正位 (Upright)",
                    )
                    DetailField("牌位意义", selectedCard.slot.description)
                    DetailField("属性", "${selectedCard.card.arcana.displayName} · ${selectedCard.card.element.displayName}")
                }
            }

            item(key = "card-detail-keywords-${pagerState.currentPage}") {
                ResultCard {
                    Text(
                        text = "关键词",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = selectedCard.card.keywordsZh.joinToString(" · "),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            item(key = "card-detail-meaning-${pagerState.currentPage}") {
                ResultCard {
                    Text(
                        text = if (isSelectedReversed) "逆位核心释义" else "正位核心释义",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    selectedMeanings.forEach { meaning ->
                        Text(
                            text = "• $meaning",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }

            if (selectedCard.card.fortuneTelling.isNotEmpty()) {
                item(key = "card-detail-fortune-${pagerState.currentPage}") {
                    ResultCard {
                        Text(
                            text = "指引断语",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                        selectedCard.card.fortuneTelling.forEach { line ->
                            Text(
                                text = "• $line",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }

            item(key = "results-archive") {
                val archiveInteraction = remember { MutableInteractionSource() }
                BoompalaCardButton(
                    onClick = { onArchive(reading) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .wearPressFeedback(archiveInteraction),
                    interactionSource = archiveInteraction,
                    colors = BoompalaButtonDefaults.outlinedButtonColors(),
                ) {
                    Text("归档此结果")
                }
            }

            item(key = "results-recast") {
                val recastInteraction = remember { MutableInteractionSource() }
                BoompalaCardButton(
                    onClick = {
                        currentStep = 0
                        flippedList.clear()
                        repeat(10) { flippedList.add(false) }
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

            item(key = "results-back-home") {
                val finishInteraction = remember { MutableInteractionSource() }
                BoompalaCardButton(
                    onClick = onBack,
                    modifier = Modifier
                        .fillMaxWidth()
                        .wearPressFeedback(finishInteraction),
                    interactionSource = finishInteraction,
                    colors = BoompalaButtonDefaults.outlinedButtonColors(),
                ) {
                    Text("返回首页")
                }
            }
        }
    }
}

@Composable
private fun TarotCelticCrossFlipItem(
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
        label = "TarotCelticCrossFlipAnimation",
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
