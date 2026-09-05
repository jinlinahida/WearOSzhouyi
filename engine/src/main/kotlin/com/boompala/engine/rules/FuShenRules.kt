package com.boompala.engine.rules

import com.boompala.engine.model.FeiFuRelation
import com.boompala.engine.model.FiveElement
import com.boompala.engine.model.FuShenInfo
import com.boompala.engine.model.HexagramPattern
import com.boompala.engine.model.Palace
import com.boompala.engine.model.SixRelation
import com.boompala.engine.model.Yao
import com.boompala.engine.model.YaoPosition

object FuShenRules {
    /**
     * Deduces FuShen for lines where original hexagram is missing six relations.
     * Returns a map from YaoPosition to FuShenInfo.
     */
    fun calculateFuShen(
        palace: Palace,
        yaoFromBottom: List<Yao>,
    ): Map<YaoPosition, FuShenInfo> {
        val presentRelations = yaoFromBottom.map { it.sixRelation }.toSet()
        val missingRelations = SixRelation.entries.filter { it !in presentRelations }
        if (missingRelations.isEmpty()) return emptyMap()

        // Pure hexagram pattern for the palace (e.g. QIAN -> 111111)
        val trigram = Trigram.entries.first { it.palace == palace }
        val pureBits = trigram.bitsFromBottom + trigram.bitsFromBottom
        val purePattern = HexagramPattern.fromCode(pureBits)
        val pureNajia = NajiaRules.ganzhiFromBottom(purePattern)

        val result = mutableMapOf<YaoPosition, FuShenInfo>()
        for (missing in missingRelations) {
            // Find corresponding line in pure hexagram that has the missing relation
            val pureIndex = pureNajia.indexOfFirst { lineGanzhi ->
                SixRelationRules.relation(palace, lineGanzhi.earthlyBranch.element) == missing
            }
            if (pureIndex != -1) {
                val position = YaoPosition.entries[pureIndex]
                val fuGanzhi = pureNajia[pureIndex]
                val fuElement = fuGanzhi.earthlyBranch.element
                val feiElement = yaoFromBottom[pureIndex].element
                val feiFuRelation = calculateFeiFuRelation(feiElement = feiElement, fuElement = fuElement)

                result[position] = FuShenInfo(
                    position = position,
                    heavenlyStem = fuGanzhi.heavenlyStem,
                    earthlyBranch = fuGanzhi.earthlyBranch,
                    element = fuElement,
                    sixRelation = missing,
                    feiFuRelation = feiFuRelation,
                )
            }
        }
        return result
    }

    private fun calculateFeiFuRelation(feiElement: FiveElement, fuElement: FiveElement): FeiFuRelation = when {
        feiElement == fuElement -> FeiFuRelation.SAME
        feiElement.generates(fuElement) -> FeiFuRelation.FEI_SHENG_FU
        feiElement.overcomes(fuElement) -> FeiFuRelation.FEI_KE_FU
        fuElement.generates(feiElement) -> FeiFuRelation.FU_SHENG_FEI
        fuElement.overcomes(feiElement) -> FeiFuRelation.FU_KE_FEI
        else -> FeiFuRelation.SAME
    }

    private fun FiveElement.generates(other: FiveElement): Boolean = when (this) {
        FiveElement.WOOD -> other == FiveElement.FIRE
        FiveElement.FIRE -> other == FiveElement.EARTH
        FiveElement.EARTH -> other == FiveElement.METAL
        FiveElement.METAL -> other == FiveElement.WATER
        FiveElement.WATER -> other == FiveElement.WOOD
    }

    private fun FiveElement.overcomes(other: FiveElement): Boolean = when (this) {
        FiveElement.WOOD -> other == FiveElement.EARTH
        FiveElement.EARTH -> other == FiveElement.WATER
        FiveElement.WATER -> other == FiveElement.FIRE
        FiveElement.FIRE -> other == FiveElement.METAL
        FiveElement.METAL -> other == FiveElement.WOOD
    }
}
