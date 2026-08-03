package com.boompala.engine.rules

import com.boompala.engine.model.EarthlyBranch
import com.boompala.engine.model.FiveElement
import com.boompala.engine.model.Ganzhi
import com.boompala.engine.model.HeavenlyStem
import com.boompala.engine.model.HexagramPattern
import com.boompala.engine.model.Palace
import com.boompala.engine.model.SixRelation
import com.boompala.engine.model.SixSpirit

/**
 * Traditional 京房纳甲 table, cross-checked against bopo/najia const.py at
 * commit 9cf119169d7eb8e48febc05274aebf3f7106d647.
 */
internal object NajiaRules {
    fun ganzhiFromBottom(pattern: HexagramPattern): List<Ganzhi> {
        val code = pattern.codeFromBottom
        val lower = Trigram.fromBits(code.substring(0, 3))
        val upper = Trigram.fromBits(code.substring(3, 6))
        val lowerRule = rules.getValue(lower).lower
        val upperRule = rules.getValue(upper).upper

        return lowerRule.branches.map { branch ->
            Ganzhi(lowerRule.stem, branch)
        } + upperRule.branches.map { branch ->
            Ganzhi(upperRule.stem, branch)
        }
    }

    private data class HalfHexagramRule(
        val stem: HeavenlyStem,
        val branches: List<EarthlyBranch>,
    )

    private data class TrigramNajiaRule(
        val lower: HalfHexagramRule,
        val upper: HalfHexagramRule,
    )

    private val rules = mapOf(
        Trigram.QIAN to TrigramNajiaRule(
            lower = half(HeavenlyStem.JIA, EarthlyBranch.ZI, EarthlyBranch.YIN, EarthlyBranch.CHEN),
            upper = half(HeavenlyStem.REN, EarthlyBranch.WU, EarthlyBranch.SHEN, EarthlyBranch.XU),
        ),
        Trigram.DUI to TrigramNajiaRule(
            lower = half(HeavenlyStem.DING, EarthlyBranch.SI, EarthlyBranch.MAO, EarthlyBranch.CHOU),
            upper = half(HeavenlyStem.DING, EarthlyBranch.HAI, EarthlyBranch.YOU, EarthlyBranch.WEI),
        ),
        Trigram.LI to TrigramNajiaRule(
            lower = half(HeavenlyStem.JI, EarthlyBranch.MAO, EarthlyBranch.CHOU, EarthlyBranch.HAI),
            upper = half(HeavenlyStem.JI, EarthlyBranch.YOU, EarthlyBranch.WEI, EarthlyBranch.SI),
        ),
        Trigram.ZHEN to TrigramNajiaRule(
            lower = half(HeavenlyStem.GENG, EarthlyBranch.ZI, EarthlyBranch.YIN, EarthlyBranch.CHEN),
            upper = half(HeavenlyStem.GENG, EarthlyBranch.WU, EarthlyBranch.SHEN, EarthlyBranch.XU),
        ),
        Trigram.XUN to TrigramNajiaRule(
            lower = half(HeavenlyStem.XIN, EarthlyBranch.CHOU, EarthlyBranch.HAI, EarthlyBranch.YOU),
            upper = half(HeavenlyStem.XIN, EarthlyBranch.WEI, EarthlyBranch.SI, EarthlyBranch.MAO),
        ),
        Trigram.KAN to TrigramNajiaRule(
            lower = half(HeavenlyStem.WU, EarthlyBranch.YIN, EarthlyBranch.CHEN, EarthlyBranch.WU),
            upper = half(HeavenlyStem.WU, EarthlyBranch.SHEN, EarthlyBranch.XU, EarthlyBranch.ZI),
        ),
        Trigram.GEN to TrigramNajiaRule(
            lower = half(HeavenlyStem.BING, EarthlyBranch.CHEN, EarthlyBranch.WU, EarthlyBranch.SHEN),
            upper = half(HeavenlyStem.BING, EarthlyBranch.XU, EarthlyBranch.ZI, EarthlyBranch.YIN),
        ),
        Trigram.KUN to TrigramNajiaRule(
            lower = half(HeavenlyStem.YI, EarthlyBranch.WEI, EarthlyBranch.SI, EarthlyBranch.MAO),
            upper = half(HeavenlyStem.GUI, EarthlyBranch.CHOU, EarthlyBranch.HAI, EarthlyBranch.YOU),
        ),
    )

    private fun half(
        stem: HeavenlyStem,
        first: EarthlyBranch,
        second: EarthlyBranch,
        third: EarthlyBranch,
    ) = HalfHexagramRule(stem, listOf(first, second, third))
}

internal object SixRelationRules {
    fun relation(
        palace: Palace,
        lineElement: FiveElement,
    ): SixRelation = when {
        lineElement == palace.element -> SixRelation.SIBLINGS
        lineElement.generates(palace.element) -> SixRelation.PARENTS
        palace.element.generates(lineElement) -> SixRelation.OFFSPRING
        palace.element.controls(lineElement) -> SixRelation.WEALTH
        lineElement.controls(palace.element) -> SixRelation.OFFICER_GHOST
        else -> error("Unhandled five-element relationship.")
    }

    private fun FiveElement.generates(other: FiveElement): Boolean =
        when (this) {
            FiveElement.WOOD -> other == FiveElement.FIRE
            FiveElement.FIRE -> other == FiveElement.EARTH
            FiveElement.EARTH -> other == FiveElement.METAL
            FiveElement.METAL -> other == FiveElement.WATER
            FiveElement.WATER -> other == FiveElement.WOOD
        }

    private fun FiveElement.controls(other: FiveElement): Boolean =
        when (this) {
            FiveElement.WOOD -> other == FiveElement.EARTH
            FiveElement.FIRE -> other == FiveElement.METAL
            FiveElement.EARTH -> other == FiveElement.WATER
            FiveElement.METAL -> other == FiveElement.WOOD
            FiveElement.WATER -> other == FiveElement.FIRE
        }
}

internal object SixSpiritRules {
    fun spiritsFromBottom(dayStem: HeavenlyStem): List<SixSpirit> {
        val startIndex = when (dayStem) {
            HeavenlyStem.JIA, HeavenlyStem.YI -> 0
            HeavenlyStem.BING, HeavenlyStem.DING -> 1
            HeavenlyStem.WU -> 2
            HeavenlyStem.JI -> 3
            HeavenlyStem.GENG, HeavenlyStem.XIN -> 4
            HeavenlyStem.REN, HeavenlyStem.GUI -> 5
        }
        val spirits = SixSpirit.entries
        return List(spirits.size) { offset ->
            spirits[(startIndex + offset) % spirits.size]
        }
    }
}

internal object VoidRules {
    fun voidBranches(dayGanzhi: Ganzhi): List<EarthlyBranch> {
        val cycleStartBranch = Math.floorMod(
            dayGanzhi.earthlyBranch.index - dayGanzhi.heavenlyStem.index,
            EarthlyBranch.entries.size,
        )
        val branches = EarthlyBranch.entries
        return listOf(
            branches[(cycleStartBranch + 10) % branches.size],
            branches[(cycleStartBranch + 11) % branches.size],
        )
    }
}
