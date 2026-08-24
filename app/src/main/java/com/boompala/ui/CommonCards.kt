package com.boompala.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text

private val ResultCardShape = RoundedCornerShape(12.dp)
private val ResultCardColor = Color(0xFF1B1B1B)

@Composable
fun ResultCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val metrics = LocalUiMetrics.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(ResultCardShape)
            .background(ResultCardColor)
            .padding(
                horizontal = metrics.horizontalPadding / 2,
                vertical = metrics.cardVerticalPadding,
            ),
        verticalArrangement = Arrangement.spacedBy(metrics.itemSpacing / 2),
        content = content,
    )
}

@Composable
fun DetailField(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}
