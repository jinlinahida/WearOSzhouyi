package com.boompala.engine.data

import com.boompala.engine.model.YaoPosition
import com.boompala.engine.rules.HexagramCatalog
import com.boompala.engine.rules.Trigram

/** Lightweight, calculation-free reference record used by the offline browser. */
data class HexagramReference(
    val order: Int,
    val codeFromBottom: String,
    val name: String,
    val upperTrigram: Trigram,
    val lowerTrigram: Trigram,
)

fun hexagramReferences(): List<HexagramReference> = HexagramCatalog.zhouOrderCodes.mapIndexed { index, code ->
    HexagramReference(
        order = index + 1,
        codeFromBottom = code,
        name = HexagramCatalog.nameFor(code),
        upperTrigram = Trigram.fromBits(code.substring(3, 6)),
        lowerTrigram = Trigram.fromBits(code.substring(0, 3)),
    )
}

fun HexagramReference.linePolaritiesFromBottom(): List<Boolean> =
    codeFromBottom.map { it == '1' }

fun HexagramReference.linePositions(): List<YaoPosition> = YaoPosition.entries
