package com.boompala.engine.meihua

import com.boompala.engine.model.YaoPolarity
import com.boompala.engine.rules.Trigram

/** Mei Hua adapters over the single shared trigram mapping. */
internal object TrigramRules {
    fun fromCastingSum(sum: Int): Trigram =
        Trigram.fromNumber(positiveRemainder(sum, 8))

    fun fromLines(linesFromBottom: List<YaoPolarity>): Trigram {
        require(linesFromBottom.size == 3)
        return Trigram.fromBits(linesFromBottom.joinToString("") {
            if (it == YaoPolarity.YANG) "1" else "0"
        })
    }
}

/** A zero remainder is represented by the divisor, as required by this rule. */
internal fun positiveRemainder(value: Int, divisor: Int): Int {
    require(divisor > 0)
    val remainder = value % divisor
    return if (remainder == 0) divisor else remainder
}
