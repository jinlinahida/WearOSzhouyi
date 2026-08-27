@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.boompala.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.OutlinedButton
import androidx.wear.compose.material3.Text
import com.boompala.engine.data.ClassicalTextRepository
import com.boompala.engine.data.EmptyTarotCardRepository
import com.boompala.engine.data.HexagramInterpretationRepository
import com.boompala.engine.data.HexagramReference
import com.boompala.engine.data.KnowledgeArticle
import com.boompala.engine.data.LineTextRepository
import com.boompala.engine.data.TarotCardRepository
import com.boompala.engine.data.linePolaritiesFromBottom
import com.boompala.engine.data.linePositions
import com.boompala.engine.model.YaoPosition
import com.boompala.engine.tarot.ArcanaType
import com.boompala.engine.tarot.TarotCard
import com.boompala.engine.tarot.TarotSuit

@Immutable
data class BrowserData(
    val hexagrams: List<HexagramReference>,
    val lines: LineTextRepository,
    val classics: ClassicalTextRepository,
    val interpretations: HexagramInterpretationRepository,
    val knowledge: List<KnowledgeArticle>,
    val tarotCards: TarotCardRepository = EmptyTarotCardRepository,
)

@Composable
fun BrowseHomeScreen(
    data: BrowserData,
    rotary: Boolean,
    onHexagrams: () -> Unit,
    onKnowledge: () -> Unit,
    onTarot: () -> Unit,
    onBack: () -> Unit,
) {
    val m = LocalUiMetrics.current
    RotaryScrollColumn(
        rotaryEnabled = rotary,
        contentPadding = m.screenPadding,
        itemSpacing = m.itemSpacing,
    ) {
        item {
            Text(
                text = "浏览",
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            val hexInteraction = remember { MutableInteractionSource() }
            OutlinedButton(
                onClick = onHexagrams,
                modifier = Modifier
                    .fillMaxWidth()
                    .wearPressFeedback(hexInteraction),
                interactionSource = hexInteraction,
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = "六十四卦",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "查看全部卦象与爻辞",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        item {
            val tarotInteraction = remember { MutableInteractionSource() }
            OutlinedButton(
                onClick = onTarot,
                modifier = Modifier
                    .fillMaxWidth()
                    .wearPressFeedback(tarotInteraction),
                interactionSource = tarotInteraction,
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = "塔罗牌库",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "78张大/小阿卡纳与牌义",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        item {
            val daoInteraction = remember { MutableInteractionSource() }
            OutlinedButton(
                onClick = onKnowledge,
                modifier = Modifier
                    .fillMaxWidth()
                    .wearPressFeedback(daoInteraction),
                interactionSource = daoInteraction,
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = "道教知识",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "了解传统文化体系",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        item {
            val yiInteraction = remember { MutableInteractionSource() }
            OutlinedButton(
                onClick = onKnowledge,
                modifier = Modifier
                    .fillMaxWidth()
                    .wearPressFeedback(yiInteraction),
                interactionSource = yiInteraction,
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = "易学基础",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "阴阳五行、八卦、干支",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        item {
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
}

@Composable
fun HexagramBrowserScreen(
    data: BrowserData,
    rotary: Boolean,
    onOpen: (HexagramReference) -> Unit,
    onBack: () -> Unit,
) {
    val m = LocalUiMetrics.current
    RotaryScrollColumn(
        rotaryEnabled = rotary,
        contentPadding = m.screenPadding,
        itemSpacing = m.itemSpacing,
    ) {
        item {
            Text(
                text = "六十四卦",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        items(data.hexagrams, key = { it.codeFromBottom }) { h ->
            val meaning = data.interpretations.interpretationFor(h.codeFromBottom)?.coreMeaning.orEmpty()
            val pressInteraction = remember { MutableInteractionSource() }
            OutlinedButton(
                onClick = { onOpen(h) },
                modifier = Modifier
                    .fillMaxWidth()
                    .wearPressFeedback(pressInteraction),
                interactionSource = pressInteraction,
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = "${h.order}. ${h.name}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "${h.upperTrigram.displayName}上${h.lowerTrigram.displayName}下 · $meaning",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        item {
            val pressInteraction = remember { MutableInteractionSource() }
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier
                    .fillMaxWidth()
                    .wearPressFeedback(pressInteraction),
                interactionSource = pressInteraction,
            ) {
                Text("返回")
            }
        }
    }
}

@Composable
fun HexagramDetailScreen(
    hex: HexagramReference,
    data: BrowserData,
    rotary: Boolean,
    animationsEnabled: Boolean = true,
    onBack: () -> Unit,
) {
    val m = LocalUiMetrics.current
    val interpretation = data.interpretations.interpretationFor(hex.codeFromBottom)
    val classics = data.classics.textsFor(hex.codeFromBottom)
    RotaryScrollColumn(
        rotaryEnabled = rotary,
        contentPadding = m.screenPadding,
        itemSpacing = m.itemSpacing,
    ) {
        item {
            Text(
                text = "${hex.order}. ${hex.name}",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (animationsEnabled) Modifier.basicMarquee() else Modifier),
            )
        }

        item {
            ResultCard {
                Text("卦象结构", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                DetailField("上下取象", "${hex.upperTrigram.displayName}上 · ${hex.lowerTrigram.displayName}下")
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        hex.linePolaritiesFromBottom().asReversed().forEachIndexed { i, _ ->
                            HexagramLine(HexagramDisplayModel(hex.name, hex.linePolaritiesFromBottom()).lineDisplayAt(5 - i))
                        }
                    }
                }
            }
        }

        item {
            ResultCard {
                Text("卦辞与释义", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                DetailField("经典卦辞", classics?.guaText ?: "暂无可靠卦辞")
                DetailField("核心释义", interpretation?.coreMeaning ?: "暂无离线解释")
                DetailField("卦序与排盘", "卦序：第 ${hex.order} 卦 · 卦宫资料沿用纳甲排盘规则")
            }
        }

        item {
            ResultCard {
                Text("彖传与象传", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                DetailField("彖传", classics?.tuanText ?: "暂无可靠彖传")
                DetailField("象传", classics?.imageText ?: "暂无可靠象传")
            }
        }

        hex.linePositions().asReversed().forEach { p ->
            item(key = "line-${p.indexFromBottom}") {
                ResultCard {
                    Text(p.displayName, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    DetailField("爻辞原文", data.lines.lineText(hex.codeFromBottom, p) ?: "爻辞数据不可用")
                    DetailField("白话注解", linePlainText(p.indexFromBottom))
                }
            }
        }

        item {
            ResultCard {
                Text("断易参考", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                DetailField("决策建议", interpretation?.advice ?: "暂无基础解释")
                interpretation?.keywords?.let {
                    DetailField("核心关键词", it.joinToString("、"))
                }
            }
        }

        item {
            ResultCard {
                Text("相关知识", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                DetailField("八卦取象", "上卦：${hex.upperTrigram.displayName}卦（${hex.upperTrigram.palace.displayName}）；下卦：${hex.lowerTrigram.displayName}卦（${hex.lowerTrigram.palace.displayName}）")
            }
        }

        item {
            val pressInteraction = remember { MutableInteractionSource() }
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier
                    .fillMaxWidth()
                    .wearPressFeedback(pressInteraction),
                interactionSource = pressInteraction,
            ) {
                Text("返回卦列表")
            }
        }
    }
}

private fun linePlainText(i: Int) = listOf(
    "事情初起，先看基础。",
    "逐步推进，重视回应。",
    "中段需谨慎，避免过满。",
    "转换关口，调整方法。",
    "接近成果，保持谦抑。",
    "事情收束，回看全局。",
)[i]

@Composable
fun TarotBrowserScreen(
    cards: List<TarotCard>,
    rotary: Boolean,
    onOpen: (TarotCard) -> Unit,
    onBack: () -> Unit,
) {
    val m = LocalUiMetrics.current
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    val categories = listOf(
        null to "全部 (78)",
        "MAJOR" to "大阿卡纳 (22)",
        "WANDS" to "权杖 (14)",
        "CUPS" to "圣杯 (14)",
        "SWORDS" to "宝剑 (14)",
        "PENTACLES" to "星币 (14)",
    )
    val filteredCards = remember(selectedCategory, cards) {
        when (selectedCategory) {
            null -> cards
            "MAJOR" -> cards.filter { it.arcana == ArcanaType.MAJOR }
            "WANDS" -> cards.filter { it.suit == TarotSuit.WANDS }
            "CUPS" -> cards.filter { it.suit == TarotSuit.CUPS }
            "SWORDS" -> cards.filter { it.suit == TarotSuit.SWORDS }
            "PENTACLES" -> cards.filter { it.suit == TarotSuit.PENTACLES }
            else -> cards
        }
    }

    RotaryScrollColumn(
        rotaryEnabled = rotary,
        contentPadding = m.screenPadding,
        itemSpacing = m.itemSpacing,
    ) {
        item {
            Text(
                text = "塔罗牌库",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            Text("分类筛选", style = MaterialTheme.typography.labelSmall)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                categories.forEach { (cat, title) ->
                    val pressInteraction = remember { MutableInteractionSource() }
                    OutlinedButton(
                        onClick = { selectedCategory = cat },
                        modifier = Modifier
                            .fillMaxWidth()
                            .wearPressFeedback(pressInteraction),
                        interactionSource = pressInteraction,
                    ) {
                        Text(if (selectedCategory == cat) "● $title" else title)
                    }
                }
            }
        }
        items(filteredCards, key = { it.code }) { card ->
            val pressInteraction = remember { MutableInteractionSource() }
            OutlinedButton(
                onClick = { onOpen(card) },
                modifier = Modifier
                    .fillMaxWidth()
                    .wearPressFeedback(pressInteraction),
                interactionSource = pressInteraction,
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = "${card.nameZh} · ${card.nameEn}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "${card.arcana.displayName} · ${card.keywordsZh.take(3).joinToString(" · ")}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        item {
            val pressInteraction = remember { MutableInteractionSource() }
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier
                    .fillMaxWidth()
                    .wearPressFeedback(pressInteraction),
                interactionSource = pressInteraction,
            ) {
                Text("返回浏览首页")
            }
        }
    }
}

@Composable
fun TarotCardDetailScreen(
    card: TarotCard,
    rotary: Boolean,
    animationsEnabled: Boolean = true,
    onBack: () -> Unit,
) {
    val m = LocalUiMetrics.current
    RotaryScrollColumn(
        rotaryEnabled = rotary,
        contentPadding = m.screenPadding,
        itemSpacing = m.itemSpacing,
    ) {
        item {
            Text(
                text = "${card.nameZh} · ${card.nameEn}",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (animationsEnabled) Modifier.basicMarquee() else Modifier),
            )
        }

        item {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(TarotImageAssets.cardDrawableRes(card)),
                    contentDescription = card.nameZh,
                    modifier = Modifier
                        .size(width = 80.dp, height = 140.dp)
                        .clip(RoundedCornerShape(8.dp)),
                )
            }
        }

        item {
            ResultCard {
                Text("卡牌属性", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                DetailField("位阶编号", card.rankName)
                DetailField("大/小阿卡纳", card.arcana.displayName)
                DetailField("所属花色", card.suit.displayName)
                DetailField("对应元素", card.element.displayName)
            }
        }

        item {
            ResultCard {
                Text("核心关键词", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                DetailField("中文关键词", card.keywordsZh.joinToString(" · "))
                DetailField("英文关键词", card.keywordsEn.joinToString(" · "))
            }
        }

        item {
            ResultCard {
                Text("正位核心释义 (Upright)", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                card.uprightMeaningsZh.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
            }
        }

        item {
            ResultCard {
                Text("逆位核心释义 (Reversed)", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                card.reversedMeaningsZh.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
            }
        }

        if (card.fortuneTellingZh.isNotEmpty()) {
            item {
                ResultCard {
                    Text("占卜指引断语", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    card.fortuneTellingZh.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
                }
            }
        }

        item {
            val backInteraction = remember { MutableInteractionSource() }
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier
                    .fillMaxWidth()
                    .wearPressFeedback(backInteraction),
                interactionSource = backInteraction,
            ) {
                Text("返回塔罗列表")
            }
        }
    }
}

@Composable
fun KnowledgeListScreen(
    articles: List<KnowledgeArticle>,
    rotary: Boolean,
    onOpen: (KnowledgeArticle) -> Unit,
    onBack: () -> Unit,
) {
    val m = LocalUiMetrics.current
    RotaryScrollColumn(
        rotaryEnabled = rotary,
        contentPadding = m.screenPadding,
        itemSpacing = m.itemSpacing,
    ) {
        item {
            Text(
                text = "道教知识",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        articles.groupBy { it.category }.forEach { (category, values) ->
            item(key = "cat-$category") {
                Text(
                    text = category,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
            items(values, key = { it.id }) { a ->
                val pressInteraction = remember { MutableInteractionSource() }
                OutlinedButton(
                    onClick = { onOpen(a) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .wearPressFeedback(pressInteraction),
                    interactionSource = pressInteraction,
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.Start,
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            text = a.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = a.summary,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        item {
            val pressInteraction = remember { MutableInteractionSource() }
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier
                    .fillMaxWidth()
                    .wearPressFeedback(pressInteraction),
                interactionSource = pressInteraction,
            ) {
                Text("返回")
            }
        }
    }
}

@Composable
fun KnowledgeDetailScreen(
    article: KnowledgeArticle,
    rotary: Boolean,
    animationsEnabled: Boolean = true,
    onBack: () -> Unit,
) {
    val m = LocalUiMetrics.current
    RotaryScrollColumn(
        rotaryEnabled = rotary,
        contentPadding = m.screenPadding,
        itemSpacing = m.itemSpacing,
    ) {
        item {
            Text(
                text = article.title,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (animationsEnabled) Modifier.basicMarquee() else Modifier),
            )
        }
        item {
            ResultCard {
                Text("核心概要", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                DetailField("简介", article.summary)
            }
        }
        item {
            ResultCard {
                Text("详细内容", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                DetailField("正文", article.body)
            }
        }
        item {
            val pressInteraction = remember { MutableInteractionSource() }
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier
                    .fillMaxWidth()
                    .wearPressFeedback(pressInteraction),
                interactionSource = pressInteraction,
            ) {
                Text("返回知识列表")
            }
        }
    }
}
