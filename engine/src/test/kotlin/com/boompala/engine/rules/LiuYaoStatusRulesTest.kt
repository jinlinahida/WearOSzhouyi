package com.boompala.engine.rules

import com.boompala.engine.model.EarthlyBranch
import com.boompala.engine.model.Ganzhi
import com.boompala.engine.model.HeavenlyStem
import com.boompala.engine.model.HexagramStatus
import com.boompala.engine.model.SixRelation
import com.boompala.engine.model.SixSpirit
import com.boompala.engine.model.Yao
import com.boompala.engine.model.YaoPolarity
import com.boompala.engine.model.YaoPosition
import com.boompala.engine.model.YaoStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LiuYaoStatusRulesTest {

    @Test
    fun testClashAndCombine() {
        assertTrue(LiuYaoStatusRules.isClash(EarthlyBranch.ZI, EarthlyBranch.WU))
        assertTrue(LiuYaoStatusRules.isClash(EarthlyBranch.YIN, EarthlyBranch.SHEN))
        assertTrue(LiuYaoStatusRules.isCombine(EarthlyBranch.ZI, EarthlyBranch.CHOU))
        assertTrue(LiuYaoStatusRules.isCombine(EarthlyBranch.YIN, EarthlyBranch.HAI))
    }

    @Test
    fun testMonthBrokenAndDarkMoving() {
        val darkMovingMonth = Ganzhi(HeavenlyStem.GUI, EarthlyBranch.HAI) // 亥水月生寅木(相)，旺相逢日冲为暗动
        val dayGanzhi = Ganzhi(HeavenlyStem.WU, EarthlyBranch.SHEN) // 日支为申，与寅相冲

        val staticYao = Yao(
            position = YaoPosition.FIRST,
            yinYang = YaoPolarity.YANG,
            moving = false,
            heavenlyStem = HeavenlyStem.JIA,
            earthlyBranch = EarthlyBranch.YIN,
            element = EarthlyBranch.YIN.element,
            sixRelation = SixRelation.PARENTS,
            sixSpirit = SixSpirit.AZURE_DRAGON,
            isShi = false,
            isYing = false,
            isVoid = false,
        )

        val darkMovingStatuses = LiuYaoStatusRules.evaluateYaoStatuses(
            yao = staticYao,
            monthGanzhi = darkMovingMonth,
            dayGanzhi = dayGanzhi,
        )
        assertTrue(darkMovingStatuses.contains(YaoStatus.DARK_MOVING))

        // 午火月：木生火(休)，休囚之爻逢日支申冲，为日破而非暗动
        val dayBrokenMonth = Ganzhi(HeavenlyStem.BING, EarthlyBranch.WU)
        val dayBrokenStatuses = LiuYaoStatusRules.evaluateYaoStatuses(
            yao = staticYao,
            monthGanzhi = dayBrokenMonth,
            dayGanzhi = dayGanzhi,
        )
        assertTrue(dayBrokenStatuses.contains(YaoStatus.DAY_BROKEN))
    }

    @Test
    fun testSixClashHexagram() {
        // 乾为天 111111 1-4, 2-5, 3-6 地支分别为 子午、寅申、辰戌，均为六冲
        val pattern = com.boompala.engine.model.HexagramPattern.fromCode("111111")
        val classification = HexagramRules.classify(pattern)
        val najia = NajiaRules.ganzhiFromBottom(pattern)
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
                sixSpirit = SixSpirit.AZURE_DRAGON,
                isShi = position == classification.shiPosition,
                isYing = position == classification.yingPosition,
                isVoid = false,
            )
        }
        val hex = com.boompala.engine.model.Hexagram(
            pattern = pattern,
            name = classification.name,
            palace = classification.palace,
            element = classification.palace.element,
            palaceStage = classification.palaceStage,
            yaoFromBottom = yaoList,
        )

        val statuses = LiuYaoStatusRules.evaluateHexagramStatuses(hex)
        assertTrue(statuses.contains(HexagramStatus.SIX_CLASH))
    }
}
