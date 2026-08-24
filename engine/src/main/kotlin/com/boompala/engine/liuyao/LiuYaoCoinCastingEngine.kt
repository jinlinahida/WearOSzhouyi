package com.boompala.engine.liuyao

import com.boompala.engine.model.HexagramInput
import com.boompala.engine.model.YaoLineInput
import com.boompala.engine.model.YaoPosition
import com.boompala.engine.model.YaoState
import java.time.Instant
import java.time.ZoneId
import kotlin.random.Random

/**
 * Represents one side of a traditional Chinese coin used in I Ching divination.
 * 字 (Heads / Inscription) = 3
 * 背 (Tails / Blank) = 2
 */
enum class CoinSide(
    val value: Int,
    val displayName: String,
) {
    HEADS(3, "字"),
    TAILS(2, "背"),
    ;

    companion object {
        fun fromValue(value: Int): CoinSide = when (value) {
            3 -> HEADS
            2 -> TAILS
            else -> throw IllegalArgumentException("Invalid coin value: $value. Must be 2 (背) or 3 (字).")
        }
    }
}

/**
 * Result of throwing three coins for a single Yao line.
 */
data class CoinTossResult(
    val coins: List<CoinSide>,
    val sum: Int,
    val state: YaoState,
) {
    init {
        require(coins.size == 3) { "A coin toss must consist of exactly 3 coins." }
        require(sum == coins.sumOf { it.value }) { "Sum must match the total of the three coins." }
        require(sum in 6..9) { "Sum of three coins must be 6, 7, 8, or 9." }
        require(state == YaoState.fromNumericValue(sum)) { "YaoState must match the coin sum." }
    }
}

/**
 * A recorded toss result associated with its position in the hexagram (from 初爻 to 上爻).
 */
data class CoinCastingRecord(
    val position: YaoPosition,
    val toss: CoinTossResult,
)

/**
 * Pure Kotlin engine for traditional three-coin I Ching casting (六爻三钱摇卦).
 *
 * Probabilities:
 * - 6 (老阴: 2+2+2): 1/8 (12.5%) -> 阴爻，动爻
 * - 7 (少阳: 2+2+3, 2+3+2, 3+2+2): 3/8 (37.5%) -> 阳爻，静爻
 * - 8 (少阴: 2+3+3, 3+2+3, 3+3+2): 3/8 (37.5%) -> 阴爻，静爻
 * - 9 (老阳: 3+3+3): 1/8 (12.5%) -> 阳爻，动爻
 */
object LiuYaoCoinCastingEngine {

    /**
     * Tosses three independent coins and computes the resulting YaoState.
     */
    fun castSingleLine(random: Random = Random.Default): CoinTossResult {
        val coins = List(3) {
            if (random.nextBoolean()) CoinSide.HEADS else CoinSide.TAILS
        }
        val sum = coins.sumOf { it.value }
        val state = YaoState.fromNumericValue(sum)
        return CoinTossResult(coins, sum, state)
    }

    /**
     * Creates a single line toss from deterministic coin values (each 2 or 3).
     */
    fun singleLineFromValues(values: List<Int>): CoinTossResult {
        require(values.size == 3) { "Must provide exactly 3 coin values." }
        val coins = values.map { CoinSide.fromValue(it) }
        val sum = coins.sumOf { it.value }
        val state = YaoState.fromNumericValue(sum)
        return CoinTossResult(coins, sum, state)
    }

    /**
     * Converts six sequential toss records (from 初爻 to 上爻) into a standard HexagramInput.
     */
    fun toHexagramInput(
        records: List<CoinCastingRecord>,
        castAt: Instant = Instant.now(),
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): HexagramInput {
        require(records.size == YaoPosition.entries.size) {
            "A complete hexagram casting must contain exactly 6 lines."
        }
        require(records.map { it.position } == YaoPosition.entries) {
            "Records must be ordered strictly from 初爻 (FIRST) to 上爻 (TOP)."
        }

        val linesFromBottom = records.map { record ->
            YaoLineInput(
                position = record.position,
                state = record.toss.state,
            )
        }

        return HexagramInput(
            linesFromBottom = linesFromBottom,
            castAt = castAt,
            zoneId = zoneId,
        )
    }

    /**
     * Deterministic generator for 6 full lines.
     * Each of the 6 sub-lists must contain 3 coin values (2 or 3).
     */
    fun castDeterministic(
        sixLineCoinValues: List<List<Int>>,
        castAt: Instant = Instant.now(),
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): HexagramInput {
        require(sixLineCoinValues.size == 6) { "Must provide coin values for all 6 lines." }
        val records = YaoPosition.entries.mapIndexed { index, position ->
            val toss = singleLineFromValues(sixLineCoinValues[index])
            CoinCastingRecord(position, toss)
        }
        return toHexagramInput(records, castAt, zoneId)
    }
}
