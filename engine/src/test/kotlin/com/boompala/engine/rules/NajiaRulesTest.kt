package com.boompala.engine.rules

import com.boompala.engine.model.EarthlyBranch
import com.boompala.engine.model.Ganzhi
import com.boompala.engine.model.HeavenlyStem
import com.boompala.engine.model.HexagramPattern
import com.boompala.engine.model.Palace
import com.boompala.engine.model.SixRelation
import com.boompala.engine.model.SixSpirit
import com.boompala.engine.model.YaoPolarity
import org.junit.Assert.assertEquals
import org.junit.Test

class NajiaRulesTest {
    @Test
    fun `fire over water has traditional jingfang najia`() {
        val najia = NajiaRules.ganzhiFromBottom(pattern("010101"))

        assertEquals(
            listOf("戊寅", "戊辰", "戊午", "己酉", "己未", "己巳"),
            najia.map { it.displayName },
        )
    }

    @Test
    fun `six relations use palace element as self`() {
        val lineElements = NajiaRules.ganzhiFromBottom(pattern("010101"))
            .map { it.earthlyBranch.element }

        assertEquals(
            listOf(
                SixRelation.PARENTS,
                SixRelation.OFFSPRING,
                SixRelation.SIBLINGS,
                SixRelation.WEALTH,
                SixRelation.OFFSPRING,
                SixRelation.SIBLINGS,
            ),
            lineElements.map { SixRelationRules.relation(Palace.LI, it) },
        )
    }

    @Test
    fun `jia day starts azure dragon at first line`() {
        assertEquals(SixSpirit.entries, SixSpiritRules.spiritsFromBottom(HeavenlyStem.JIA))
    }

    @Test
    fun `jia zi day is void at xu and hai`() {
        assertEquals(
            listOf(EarthlyBranch.XU, EarthlyBranch.HAI),
            VoidRules.voidBranches(Ganzhi(HeavenlyStem.JIA, EarthlyBranch.ZI)),
        )
    }

    private fun pattern(codeFromBottom: String): HexagramPattern =
        HexagramPattern(
            codeFromBottom.map { bit ->
                if (bit == '1') YaoPolarity.YANG else YaoPolarity.YIN
            },
        )
}
