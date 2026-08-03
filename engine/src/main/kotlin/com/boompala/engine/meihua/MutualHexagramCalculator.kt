package com.boompala.engine.meihua

import com.boompala.engine.model.YaoPolarity
import com.boompala.engine.rules.HexagramCatalog
import com.boompala.engine.rules.Trigram

/**
 * 互卦 takes lines 2-4 as the new lower trigram and lines 3-5 as the new upper
 * trigram. Input and output are both bottom-to-top, matching the shared model.
 */
object MutualHexagramCalculator {
    fun calculate(originalLinesFromBottom: List<YaoPolarity>): MeiHuaHexagram {
        require(originalLinesFromBottom.size == 6)
        val lower = TrigramRules.fromLines(originalLinesFromBottom.subList(1, 4))
        val upper = TrigramRules.fromLines(originalLinesFromBottom.subList(2, 5))
        return hexagram(upper, lower)
    }

    internal fun hexagram(upper: Trigram, lower: Trigram): MeiHuaHexagram {
        val lines = lower.linesFromBottom + upper.linesFromBottom
        val code = lines.joinToString("") { if (it == YaoPolarity.YANG) "1" else "0" }
        return MeiHuaHexagram(
            name = HexagramCatalog.nameFor(code),
            upperTrigram = upper,
            lowerTrigram = lower,
            linesFromBottom = lines,
        )
    }
}
