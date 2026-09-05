package com.boompala.engine.rules

import com.boompala.engine.model.EarthlyBranch
import com.boompala.engine.model.FiveElement
import com.boompala.engine.model.Ganzhi
import com.boompala.engine.model.Hexagram
import com.boompala.engine.model.HexagramStatus
import com.boompala.engine.model.Yao
import com.boompala.engine.model.YaoStatus

object LiuYaoStatusRules {

    fun isClash(b1: EarthlyBranch, b2: EarthlyBranch): Boolean =
        (b1.index % 6) == ((b2.index + 6) % 6) && b1 != b2

    fun isCombine(b1: EarthlyBranch, b2: EarthlyBranch): Boolean = when (b1) {
        EarthlyBranch.ZI -> b2 == EarthlyBranch.CHOU
        EarthlyBranch.CHOU -> b2 == EarthlyBranch.ZI
        EarthlyBranch.YIN -> b2 == EarthlyBranch.HAI
        EarthlyBranch.HAI -> b2 == EarthlyBranch.YIN
        EarthlyBranch.MAO -> b2 == EarthlyBranch.XU
        EarthlyBranch.XU -> b2 == EarthlyBranch.MAO
        EarthlyBranch.CHEN -> b2 == EarthlyBranch.YOU
        EarthlyBranch.YOU -> b2 == EarthlyBranch.CHEN
        EarthlyBranch.SI -> b2 == EarthlyBranch.SHEN
        EarthlyBranch.SHEN -> b2 == EarthlyBranch.SI
        EarthlyBranch.WU -> b2 == EarthlyBranch.WEI
        EarthlyBranch.WEI -> b2 == EarthlyBranch.WU
    }

    fun isProsperousByMonth(yaoBranch: EarthlyBranch, monthBranch: EarthlyBranch): Boolean {
        if (yaoBranch == monthBranch) return true
        val monthElement = monthBranch.element
        val yaoElement = yaoBranch.element
        if (monthElement == yaoElement) return true
        if (monthElement.generates(yaoElement)) return true
        return false
    }

    private fun FiveElement.generates(other: FiveElement): Boolean = when (this) {
        FiveElement.WOOD -> other == FiveElement.FIRE
        FiveElement.FIRE -> other == FiveElement.EARTH
        FiveElement.EARTH -> other == FiveElement.METAL
        FiveElement.METAL -> other == FiveElement.WATER
        FiveElement.WATER -> other == FiveElement.WOOD
    }

    fun evaluateYaoStatuses(
        yao: Yao,
        monthGanzhi: Ganzhi,
        dayGanzhi: Ganzhi,
        changedYao: Yao? = null,
    ): Set<YaoStatus> {
        val statuses = mutableSetOf<YaoStatus>()
        val monthBranch = monthGanzhi.earthlyBranch
        val dayBranch = dayGanzhi.earthlyBranch
        val yaoBranch = yao.earthlyBranch

        // 1. 月破
        if (isClash(yaoBranch, monthBranch)) {
            statuses.add(YaoStatus.MONTH_BROKEN)
        }

        // 2. 日冲 / 暗动 / 日破
        val isDayClashed = isClash(yaoBranch, dayBranch)
        if (isDayClashed) {
            if (yao.moving) {
                statuses.add(YaoStatus.DAY_CLASHED)
            } else {
                val isProsperous = isProsperousByMonth(yaoBranch, monthBranch)
                if (isProsperous) {
                    statuses.add(YaoStatus.DARK_MOVING)
                } else {
                    statuses.add(YaoStatus.DAY_BROKEN)
                }
            }
        }

        // 3. 进神 / 退神 / 伏吟 / 反吟 (仅当有变爻且为动爻时)
        if (yao.moving && changedYao != null) {
            val changedBranch = changedYao.earthlyBranch
            if (yaoBranch == changedBranch) {
                statuses.add(YaoStatus.LINE_FU_YIN)
            } else if (isClash(yaoBranch, changedBranch)) {
                statuses.add(YaoStatus.LINE_FAN_YIN)
            }

            if (isAdvancing(yaoBranch, changedBranch)) {
                statuses.add(YaoStatus.ADVANCING)
            } else if (isRetreating(yaoBranch, changedBranch)) {
                statuses.add(YaoStatus.RETREATING)
            }
        }

        return statuses
    }

    fun evaluateHexagramStatuses(
        original: Hexagram,
        changed: Hexagram? = null,
    ): Set<HexagramStatus> {
        val statuses = mutableSetOf<HexagramStatus>()
        val originalBranches = original.yaoFromBottom.map { it.earthlyBranch }

        // 六冲卦 (1-4, 2-5, 3-6 相冲)
        val isSixClash = isClash(originalBranches[0], originalBranches[3]) &&
            isClash(originalBranches[1], originalBranches[4]) &&
            isClash(originalBranches[2], originalBranches[5])
        if (isSixClash) {
            statuses.add(HexagramStatus.SIX_CLASH)
        }

        // 六合卦 (1-4, 2-5, 3-6 六合)
        val isSixCombine = isCombine(originalBranches[0], originalBranches[3]) &&
            isCombine(originalBranches[1], originalBranches[4]) &&
            isCombine(originalBranches[2], originalBranches[5])
        if (isSixCombine) {
            statuses.add(HexagramStatus.SIX_COMBINE)
        }

        if (changed != null) {
            val changedBranches = changed.yaoFromBottom.map { it.earthlyBranch }
            if (originalBranches == changedBranches) {
                statuses.add(HexagramStatus.HEX_FU_YIN)
            } else if (originalBranches.zip(changedBranches).all { (o, c) -> isClash(o, c) }) {
                statuses.add(HexagramStatus.HEX_FAN_YIN)
            }
        }

        return statuses
    }

    private fun isAdvancing(from: EarthlyBranch, to: EarthlyBranch): Boolean = when (from) {
        EarthlyBranch.HAI -> to == EarthlyBranch.ZI
        EarthlyBranch.YIN -> to == EarthlyBranch.MAO
        EarthlyBranch.SI -> to == EarthlyBranch.WU
        EarthlyBranch.SHEN -> to == EarthlyBranch.YOU
        EarthlyBranch.CHOU -> to == EarthlyBranch.CHEN
        EarthlyBranch.CHEN -> to == EarthlyBranch.WEI
        EarthlyBranch.WEI -> to == EarthlyBranch.XU
        EarthlyBranch.XU -> to == EarthlyBranch.CHOU
        else -> false
    }

    private fun isRetreating(from: EarthlyBranch, to: EarthlyBranch): Boolean = when (from) {
        EarthlyBranch.ZI -> to == EarthlyBranch.HAI
        EarthlyBranch.MAO -> to == EarthlyBranch.YIN
        EarthlyBranch.WU -> to == EarthlyBranch.SI
        EarthlyBranch.YOU -> to == EarthlyBranch.SHEN
        EarthlyBranch.CHEN -> to == EarthlyBranch.CHOU
        EarthlyBranch.WEI -> to == EarthlyBranch.CHEN
        EarthlyBranch.XU -> to == EarthlyBranch.WEI
        EarthlyBranch.CHOU -> to == EarthlyBranch.XU
        else -> false
    }
}
