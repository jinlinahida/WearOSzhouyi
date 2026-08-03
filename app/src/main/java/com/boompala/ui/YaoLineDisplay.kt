package com.boompala.ui

import androidx.compose.runtime.Immutable
import com.boompala.engine.model.Yao
import com.boompala.engine.model.YaoPolarity

/**
 * UI-only projection of a yao's shape. Motion is intentionally a separate
 * property: old yin must remain broken and old yang must remain solid.
 */
internal enum class YaoLineShape {
    SOLID,
    BROKEN,
}

@Immutable
internal data class YaoLineDisplay(
    val polarity: YaoPolarity,
    val shape: YaoLineShape,
    val isMoving: Boolean,
)

internal fun Yao.toLineDisplay(): YaoLineDisplay = YaoLineDisplay(
    polarity = yinYang,
    shape = when (yinYang) {
        YaoPolarity.YANG -> YaoLineShape.SOLID
        YaoPolarity.YIN -> YaoLineShape.BROKEN
    },
    isMoving = moving,
)
