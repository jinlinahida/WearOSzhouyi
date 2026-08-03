package com.boompala.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text

/**
 * Shared Wear result-page shell. Feature-specific content supplies its time,
 * hexagram, interpretation, and specialized sections as stable Lazy items;
 * this component keeps the rotary, haptic, and position-indicator pipeline
 * identical for six-yao and Mei Hua.
 */
@Composable
fun CommonDivinationResultScreen(
    title: String,
    rotaryEnabled: Boolean,
    contentPadding: PaddingValues,
    itemSpacing: androidx.compose.ui.unit.Dp,
    content: LazyListScope.() -> Unit,
) {
    RotaryScrollColumn(
        rotaryEnabled = rotaryEnabled,
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding,
        itemSpacing = itemSpacing,
    ) {
        item(key = "common-result-title") {
            Text(title, style = MaterialTheme.typography.titleMedium)
        }
        content()
    }
}

/** UI-only, engine-neutral representation shared by both divination systems. */
@Immutable
data class HexagramDisplayModel(
    val name: String,
    val linesFromBottom: List<Boolean>,
    val movingPositions: Set<Int> = emptySet(),
) {
    init {
        require(linesFromBottom.size == 6)
        require(movingPositions.all { it in 0..5 })
    }
}

internal fun HexagramDisplayModel.lineDisplayAt(indexFromBottom: Int): YaoLineDisplay {
    require(indexFromBottom in linesFromBottom.indices)
    val isYang = linesFromBottom[indexFromBottom]
    return YaoLineDisplay(
        polarity = if (isYang) com.boompala.engine.model.YaoPolarity.YANG else com.boompala.engine.model.YaoPolarity.YIN,
        shape = if (isYang) YaoLineShape.SOLID else YaoLineShape.BROKEN,
        isMoving = indexFromBottom in movingPositions,
    )
}

/** A trigram has three lines and must not be passed through the six-line model. */
internal fun trigramLineDisplayAt(
    linesFromBottom: List<Boolean>,
    indexFromBottom: Int,
): YaoLineDisplay {
    require(linesFromBottom.size == 3)
    require(indexFromBottom in linesFromBottom.indices)
    val isYang = linesFromBottom[indexFromBottom]
    return YaoLineDisplay(
        polarity = if (isYang) com.boompala.engine.model.YaoPolarity.YANG else com.boompala.engine.model.YaoPolarity.YIN,
        shape = if (isYang) YaoLineShape.SOLID else YaoLineShape.BROKEN,
        isMoving = false,
    )
}
