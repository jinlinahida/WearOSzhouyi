package com.boompala.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
fun TarotOneCardScreen(
    engine: TarotEngine,
    reading: TarotReading?,
    rotaryScrollingEnabled: Boolean,
    onReadingChanged: (TarotReading?) -> Unit,
    onBack: () -> Unit,
    onArchive: (TarotReading) -> Unit = { },
) {
    val metrics = LocalUiMetrics.current
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    var deckType by remember { mutableStateOf(DeckType.FULL_78) }
    var allowReversed by remember { mutableStateOf(true) }
    var isFlipped by remember(reading) { mutableStateOf(false) }

    RotaryScrollColumn(
        rotaryEnabled = rotaryScrollingEnabled,
        modifier = Modifier.fillMaxSize(),
        contentPadding = metrics.screenPadding,
        itemSpacing = metrics.itemSpacing,
    ) {
        if (reading == null) {
            // Setup & Cast Stage
            item(key = "tarot-title") {
                Text(
                    text = "单牌塔罗",
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            item(key = "tarot-guidance") {
                ResultCard {
                    Text(
                        text = "静心凝神，思考你心中的问题或当下处境，随后点击抽取一张指引之牌。",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            item(key = "tarot-deck-selection") {
                ResultCard {
                    DetailField("牌组类型", deckType.displayName)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(metrics.itemSpacing),
                    ) {
                        OutlinedButton(
                            onClick = {
                                deckType = if (deckType == DeckType.FULL_78) {
                                    DeckType.MAJOR_22
                                } else {
                                    DeckType.FULL_78
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(if (deckType == DeckType.FULL_78) "切换为仅大牌 (22张)" else "切换为全牌组 (78张)")
                        }
                    }
                }
            }

            item(key = "tarot-reversed-selection") {
                ResultCard {
                    DetailField("逆位规则", if (allowReversed) "允许逆位" else "仅正位")
                    OutlinedButton(
                        onClick = { allowReversed = !allowReversed },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(if (allowReversed) "切换为仅正位" else "切换为允许逆位")
                    }
                }
            }

            item(key = "tarot-draw-action") {
                Button(
                    onClick = {
                        val newReading = engine.cast(
                            spread = TarotSpread.ONE_CARD,
                            deckType = deckType,
                            allowReversed = allowReversed,
                        )
                        isFlipped = false
                        onReadingChanged(newReading)
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("洗牌并抽牌")
                }
            }

            item(key = "tarot-back-home") {
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("返回首页")
                }
            }
        } else {
            // Reading Dealt / Revealed Stage
            val drawnCard = reading.drawnCards.firstOrNull()
            if (drawnCard != null) {
                val card = drawnCard.card
                val isReversed = drawnCard.orientation == TarotOrientation.REVERSED

                item(key = "tarot-card-flip") {
                    TarotCardFlipView(
                        drawnCard = drawnCard,
                        isFlipped = isFlipped,
                        onFlip = {
                            if (!isFlipped) {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.GestureThresholdActivate)
                                isFlipped = true
                            }
                        },
                    )
                }

                if (!isFlipped) {
                    item(key = "tarot-flip-hint") {
                        Text(
                            text = "▲ 点击牌背翻开牌面",
                            style = MaterialTheme.typography.labelSmall,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                } else {
                    // Result Details
                    item(key = "tarot-card-header") {
                        ResultCard {
                            Text(
                                text = "${card.nameZh} · ${card.nameEn}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            DetailField(
                                "状态",
                                if (isReversed) "逆位 (Reversed)" else "正位 (Upright)",
                            )
                            DetailField("类型", "${card.arcana.displayName} · ${card.element.displayName}")
                        }
                    }

                    item(key = "tarot-keywords") {
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

                    item(key = "tarot-meaning") {
                        val meanings = if (isReversed) card.reversedMeanings else card.uprightMeanings
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
                        item(key = "tarot-fortune") {
                            ResultCard {
                                Text(
                                    text = "占卜指引",
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

                    item(key = "tarot-archive") {
                        OutlinedButton(
                            onClick = { onArchive(reading) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("归档此结果")
                        }
                    }

                    item(key = "tarot-recast") {
                        Button(
                            onClick = {
                                isFlipped = false
                                onReadingChanged(null)
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("重新抽牌")
                        }
                    }

                    item(key = "tarot-finish-back") {
                        OutlinedButton(
                            onClick = onBack,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("返回首页")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TarotCardFlipView(
    drawnCard: DrawnTarotCard,
    isFlipped: Boolean,
    onFlip: () -> Unit,
) {
    val card = drawnCard.card
    val isReversed = drawnCard.orientation == TarotOrientation.REVERSED

    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label = "TarotFlipAnimation",
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
