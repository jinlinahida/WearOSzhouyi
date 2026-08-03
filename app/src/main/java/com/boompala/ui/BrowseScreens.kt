package com.boompala.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.Card
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.OutlinedButton
import androidx.wear.compose.material3.Text
import com.boompala.engine.data.*

@Immutable
data class BrowserData(val hexagrams: List<HexagramReference>, val lines: LineTextRepository, val classics: ClassicalTextRepository, val interpretations: HexagramInterpretationRepository, val knowledge: List<KnowledgeArticle>)

@Composable
fun BrowseHomeScreen(data: BrowserData, rotary: Boolean, onHexagrams: () -> Unit, onKnowledge: () -> Unit, onBack: () -> Unit) {
    val m = LocalUiMetrics.current
    RotaryScrollColumn(rotary, contentPadding = PaddingValues(m.horizontalPadding, m.verticalPadding), itemSpacing = m.itemSpacing) {
        item { Text("浏览", style = MaterialTheme.typography.titleLarge) }
        item { Button(onClick = onHexagrams, modifier = Modifier.fillMaxWidth()) { Text("六十四卦"); Text("查看全部卦象与爻辞", style = MaterialTheme.typography.labelSmall) } }
        item { Button(onClick = onKnowledge, modifier = Modifier.fillMaxWidth()) { Text("道教知识"); Text("了解传统文化体系", style = MaterialTheme.typography.labelSmall) } }
        item { OutlinedButton(onClick = onKnowledge, modifier = Modifier.fillMaxWidth()) { Text("易学基础"); Text("阴阳五行、八卦、干支", style = MaterialTheme.typography.labelSmall) } }
        item { OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("返回") } }
    }
}

@Composable
fun HexagramBrowserScreen(data: BrowserData, rotary: Boolean, onOpen: (HexagramReference) -> Unit, onBack: () -> Unit) {
    val m = LocalUiMetrics.current
    RotaryScrollColumn(rotary, contentPadding = PaddingValues(m.horizontalPadding, m.verticalPadding), itemSpacing = m.itemSpacing) {
        item { Text("六十四卦", style = MaterialTheme.typography.titleMedium) }
        items(data.hexagrams, key = { it.codeFromBottom }) { h -> OutlinedButton(onClick = { onOpen(h) }, modifier = Modifier.fillMaxWidth()) { Text("${h.order}. ${h.name}"); Text("${h.upperTrigram.displayName}上${h.lowerTrigram.displayName}下 · ${data.interpretations.interpretationFor(h.codeFromBottom)?.coreMeaning.orEmpty()}", style = MaterialTheme.typography.labelSmall) } }
        item { OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("返回") } }
    }
}

@Composable
fun HexagramDetailScreen(hex: HexagramReference, data: BrowserData, rotary: Boolean, onBack: () -> Unit) {
    val m = LocalUiMetrics.current
    val interpretation = data.interpretations.interpretationFor(hex.codeFromBottom)
    val classics = data.classics.textsFor(hex.codeFromBottom)
    RotaryScrollColumn(rotary, contentPadding = PaddingValues(m.horizontalPadding, m.verticalPadding), itemSpacing = m.itemSpacing) {
        item { Text("${hex.order}. ${hex.name}", style = MaterialTheme.typography.titleMedium) }
        item { ReferenceCard("卦象 · ${hex.upperTrigram.displayName}上${hex.lowerTrigram.displayName}下") { hex.linePolaritiesFromBottom().asReversed().forEachIndexed { i, _ -> HexagramLine(HexagramDisplayModel(hex.name, hex.linePolaritiesFromBottom()).lineDisplayAt(5 - i)) } } }
        item { ReferenceCard("卦辞与简介") { Text(classics?.guaText ?: "暂无可靠卦辞"); Text(interpretation?.coreMeaning ?: "暂无离线解释"); Text("卦序：${hex.order}；卦宫资料沿用现有排盘规则。") } }
        item { ReferenceCard("彖传") { Text(classics?.tuanText ?: "暂无可靠彖传") } }
        item { ReferenceCard("象传") { Text(classics?.imageText ?: "暂无可靠象传") } }
        hex.linePositions().asReversed().forEach { p -> item(key = "line-${p.indexFromBottom}") { ReferenceCard(p.displayName) { Text(data.lines.lineText(hex.codeFromBottom, p) ?: "爻辞数据不可用"); Text("白话：${linePlainText(p.indexFromBottom)}") } } }
        item { ReferenceCard("基础解释") { Text(interpretation?.advice ?: "暂无基础解释"); interpretation?.keywords?.let { Text("关键词：${it.joinToString("、")}") } } }
        item { ReferenceCard("相关知识") { Text("上卦取象：${hex.upperTrigram.displayName}；下卦取象：${hex.lowerTrigram.displayName}。") } }
        item { OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("返回卦列表") } }
    }
}

@Composable private fun ReferenceCard(title: String, content: @Composable () -> Unit) { Card(onClick = {}, modifier = Modifier.fillMaxWidth()) { Text(title, style = MaterialTheme.typography.labelLarge); content() } }
private fun linePlainText(i: Int) = listOf("事情初起，先看基础。", "逐步推进，重视回应。", "中段需谨慎，避免过满。", "转换关口，调整方法。", "接近成果，保持谦抑。", "事情收束，回看全局。")[i]

@Composable
fun KnowledgeListScreen(articles: List<KnowledgeArticle>, rotary: Boolean, onOpen: (KnowledgeArticle) -> Unit, onBack: () -> Unit) {
    val m = LocalUiMetrics.current
    RotaryScrollColumn(rotary, contentPadding = PaddingValues(m.horizontalPadding, m.verticalPadding), itemSpacing = m.itemSpacing) {
        item { Text("道教知识", style = MaterialTheme.typography.titleMedium) }
        articles.groupBy { it.category }.forEach { (category, values) -> item(key = "cat-$category") { Text(category, style = MaterialTheme.typography.labelLarge) }; items(values, key = { it.id }) { a -> OutlinedButton(onClick = { onOpen(a) }, modifier = Modifier.fillMaxWidth()) { Text(a.title); Text(a.summary, style = MaterialTheme.typography.labelSmall) } } }
        item { OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("返回") } }
    }
}

@Composable fun KnowledgeDetailScreen(article: KnowledgeArticle, rotary: Boolean, onBack: () -> Unit) {
    val m = LocalUiMetrics.current
    RotaryScrollColumn(rotary, contentPadding = PaddingValues(m.horizontalPadding, m.verticalPadding), itemSpacing = m.itemSpacing) { item { Text(article.title, style = MaterialTheme.typography.titleMedium) }; item { ReferenceCard("简介") { Text(article.summary) } }; item { ReferenceCard("内容") { Text(article.body) } }; item { OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("返回知识列表") } } }
}
