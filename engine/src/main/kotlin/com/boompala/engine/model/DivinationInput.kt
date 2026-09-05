package com.boompala.engine.model

import java.time.Instant
import java.time.ZoneId

enum class DivinationInputMode(
    val displayName: String,
) {
    COIN_CAST("铜钱摇卦"),
    MANUAL_CAST("手动排爻"),
    DIRECT_INPUT("已知卦象"),
    NUMBER_CAST("数字起卦"),
}

data class YaoPolarityInput(
    val position: YaoPosition,
    val polarity: YaoPolarity,
)

/**
 * A known original/changed hexagram pair, stored in the same stable
 * 初爻 -> 上爻 position order as manual casting.
 */
data class DirectHexagramInput(
    val originalLinesFromBottom: List<YaoPolarityInput>,
    val changedLinesFromBottom: List<YaoPolarityInput>,
    val castAt: Instant,
    val zoneId: ZoneId,
) {
    init {
        requireOrderedLines(originalLinesFromBottom, "Original")
        requireOrderedLines(changedLinesFromBottom, "Changed")
    }

    /**
     * Converts direct input to the existing engine contract. A polarity
     * difference becomes an old (moving) line; equality becomes a young
     * (static) line, so the engine retains one calculation path.
     */
    fun toEngineInput(): HexagramInput = HexagramInput(
        linesFromBottom = originalLinesFromBottom.zip(changedLinesFromBottom)
            .map { (original, changed) ->
                YaoLineInput(
                    position = original.position,
                    state = original.polarity.toYaoState(
                        isChanging = original.polarity != changed.polarity,
                    ),
                )
            },
        castAt = castAt,
        zoneId = zoneId,
    )

    private fun requireOrderedLines(
        lines: List<YaoPolarityInput>,
        label: String,
    ) {
        require(lines.map { it.position } == YaoPosition.entries) {
            "$label lines must contain exactly six positions ordered from 初爻 to 上爻."
        }
    }
}

private fun YaoPolarity.toYaoState(isChanging: Boolean): YaoState =
    when (this) {
        YaoPolarity.YANG -> if (isChanging) YaoState.OLD_YANG else YaoState.YOUNG_YANG
        YaoPolarity.YIN -> if (isChanging) YaoState.OLD_YIN else YaoState.YOUNG_YIN
    }
