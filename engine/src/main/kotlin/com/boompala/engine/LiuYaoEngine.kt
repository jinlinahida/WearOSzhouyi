package com.boompala.engine

import com.boompala.engine.calendar.GanzhiCalendar
import com.boompala.engine.data.EmptyLineTextRepository
import com.boompala.engine.data.LineTextRepository
import com.boompala.engine.model.DivinationResult
import com.boompala.engine.model.EarthlyBranch
import com.boompala.engine.model.Hexagram
import com.boompala.engine.model.HexagramInput
import com.boompala.engine.model.HexagramPattern
import com.boompala.engine.model.Yao
import com.boompala.engine.model.YaoPosition
import com.boompala.engine.rules.HexagramRules
import com.boompala.engine.rules.NajiaRules
import com.boompala.engine.rules.SixRelationRules
import com.boompala.engine.rules.SixSpiritRules
import com.boompala.engine.rules.VoidRules

class LiuYaoEngine(
    private val calendar: GanzhiCalendar,
    private val lineTexts: LineTextRepository = EmptyLineTextRepository,
) {
    fun calculate(input: HexagramInput): DivinationResult {
        val basic = BasicHexagramEngine.derive(input)
        val timeInfo = calendar.divinationTimeInfo(input.castAt, input.zoneId)
        val voidBranches = VoidRules.voidBranches(timeInfo.dayGanzhi)
        val spirits = SixSpiritRules.spiritsFromBottom(timeInfo.dayGanzhi.heavenlyStem)

        val rawOriginal = buildHexagram(
            pattern = basic.original,
            movingPositions = basic.changingPositions.toSet(),
            voidBranches = voidBranches.toSet(),
            spirits = spirits,
            includeMovingLineTexts = true,
        )
        val rawChanged = basic.changed?.let { changedPattern ->
            buildHexagram(
                pattern = changedPattern,
                movingPositions = emptySet(),
                voidBranches = voidBranches.toSet(),
                spirits = spirits,
                includeMovingLineTexts = false,
            )
        }

        val fuShenMap = com.boompala.engine.rules.FuShenRules.calculateFuShen(rawOriginal.palace, rawOriginal.yaoFromBottom)

        val finalOriginalYao = rawOriginal.yaoFromBottom.mapIndexed { index, yao ->
            val changedYao = rawChanged?.yaoFromBottom?.get(index)
            val statuses = com.boompala.engine.rules.LiuYaoStatusRules.evaluateYaoStatuses(
                yao = yao,
                monthGanzhi = timeInfo.monthGanzhi,
                dayGanzhi = timeInfo.dayGanzhi,
                changedYao = changedYao,
            )
            yao.copy(
                fuShen = fuShenMap[yao.position],
                statuses = statuses,
            )
        }

        val originalHexStatuses = com.boompala.engine.rules.LiuYaoStatusRules.evaluateHexagramStatuses(
            original = rawOriginal,
            changed = rawChanged,
        )

        val finalOriginal = rawOriginal.copy(
            yaoFromBottom = finalOriginalYao,
            statuses = originalHexStatuses,
        )

        val finalChanged = rawChanged?.let { changed ->
            val changedHexStatuses = com.boompala.engine.rules.LiuYaoStatusRules.evaluateHexagramStatuses(
                original = changed,
                changed = null,
            )
            val finalChangedYao = changed.yaoFromBottom.map { yao ->
                val statuses = com.boompala.engine.rules.LiuYaoStatusRules.evaluateYaoStatuses(
                    yao = yao,
                    monthGanzhi = timeInfo.monthGanzhi,
                    dayGanzhi = timeInfo.dayGanzhi,
                    changedYao = null,
                )
                yao.copy(statuses = statuses)
            }
            changed.copy(
                yaoFromBottom = finalChangedYao,
                statuses = changedHexStatuses,
            )
        }

        return DivinationResult(
            timeInfo = timeInfo,
            voidBranches = voidBranches,
            original = finalOriginal,
            changed = finalChanged,
        )
    }

    private fun buildHexagram(
        pattern: HexagramPattern,
        movingPositions: Set<YaoPosition>,
        voidBranches: Set<EarthlyBranch>,
        spirits: List<com.boompala.engine.model.SixSpirit>,
        includeMovingLineTexts: Boolean,
    ): Hexagram {
        val classification = HexagramRules.classify(pattern)
        val najia = NajiaRules.ganzhiFromBottom(pattern)
        val yao = YaoPosition.entries.mapIndexed { index, position ->
            val lineGanzhi = najia[index]
            val moving = position in movingPositions
            Yao(
                position = position,
                yinYang = pattern.linesFromBottom[index],
                moving = moving,
                heavenlyStem = lineGanzhi.heavenlyStem,
                earthlyBranch = lineGanzhi.earthlyBranch,
                element = lineGanzhi.earthlyBranch.element,
                sixRelation = SixRelationRules.relation(
                    palace = classification.palace,
                    lineElement = lineGanzhi.earthlyBranch.element,
                ),
                sixSpirit = spirits[index],
                isShi = position == classification.shiPosition,
                isYing = position == classification.yingPosition,
                isVoid = lineGanzhi.earthlyBranch in voidBranches,
                lineText = if (includeMovingLineTexts && moving) {
                    lineTexts.lineText(pattern.codeFromBottom, position)
                } else {
                    null
                },
            )
        }

        return Hexagram(
            pattern = pattern,
            name = classification.name,
            palace = classification.palace,
            element = classification.palace.element,
            palaceStage = classification.palaceStage,
            yaoFromBottom = yao,
        )
    }
}
