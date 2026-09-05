package com.boompala.engine.rules

import com.boompala.engine.model.HexagramPattern
import com.boompala.engine.model.Palace
import com.boompala.engine.model.SixRelation
import com.boompala.engine.model.Yao
import com.boompala.engine.model.YaoPosition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FuShenRulesTest {

    @Test
    fun testPureHexagramHasNoFuShen() {
        // 乾为天 111111 属于乾宫八纯卦，包含全部五种六亲，无伏神
        val pattern = HexagramPattern.fromCode("111111")
        val classification = HexagramRules.classify(pattern)
        val najia = NajiaRules.ganzhiFromBottom(pattern)
        val spirits = SixSpiritRules.spiritsFromBottom(com.boompala.engine.model.HeavenlyStem.JIA)

        val yaoList = YaoPosition.entries.mapIndexed { index, position ->
            val gz = najia[index]
            Yao(
                position = position,
                yinYang = pattern.linesFromBottom[index],
                moving = false,
                heavenlyStem = gz.heavenlyStem,
                earthlyBranch = gz.earthlyBranch,
                element = gz.earthlyBranch.element,
                sixRelation = SixRelationRules.relation(classification.palace, gz.earthlyBranch.element),
                sixSpirit = spirits[index],
                isShi = position == classification.shiPosition,
                isYing = position == classification.yingPosition,
                isVoid = false,
            )
        }

        val fuShenMap = FuShenRules.calculateFuShen(classification.palace, yaoList)
        assertTrue(fuShenMap.isEmpty())
    }

    @Test
    fun testHexagramWithMissingRelationDeducesFuShen() {
        // 天风姤 011111 属于乾宫一世卦，装卦为：初爻丑土(父母)、二爻亥水(子孙)、三爻酉金(兄弟)、四爻午火(官鬼)、五爻申金(兄弟)、上爻戌土(父母)
        // 乾金宫缺“妻财”（木），乾为天二爻为寅木(妻财)。因此伏神在二爻。
        val pattern = HexagramPattern.fromCode("011111")
        val classification = HexagramRules.classify(pattern)
        assertEquals(Palace.QIAN, classification.palace)
        val najia = NajiaRules.ganzhiFromBottom(pattern)
        val spirits = SixSpiritRules.spiritsFromBottom(com.boompala.engine.model.HeavenlyStem.JIA)

        val yaoList = YaoPosition.entries.mapIndexed { index, position ->
            val gz = najia[index]
            Yao(
                position = position,
                yinYang = pattern.linesFromBottom[index],
                moving = false,
                heavenlyStem = gz.heavenlyStem,
                earthlyBranch = gz.earthlyBranch,
                element = gz.earthlyBranch.element,
                sixRelation = SixRelationRules.relation(classification.palace, gz.earthlyBranch.element),
                sixSpirit = spirits[index],
                isShi = position == classification.shiPosition,
                isYing = position == classification.yingPosition,
                isVoid = false,
            )
        }

        val fuShenMap = FuShenRules.calculateFuShen(classification.palace, yaoList)
        assertEquals(1, fuShenMap.size)
        val fuShenSecond = fuShenMap[YaoPosition.SECOND]
        assertNotNull(fuShenSecond)
        assertEquals(SixRelation.WEALTH, fuShenSecond!!.sixRelation)
        assertEquals(com.boompala.engine.model.EarthlyBranch.YIN, fuShenSecond.earthlyBranch)
    }
}
