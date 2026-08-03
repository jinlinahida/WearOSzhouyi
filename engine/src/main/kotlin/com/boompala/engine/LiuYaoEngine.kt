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

        val original = buildHexagram(
            pattern = basic.original,
            movingPositions = basic.changingPositions.toSet(),
            voidBranches = voidBranches.toSet(),
            spirits = spirits,
            includeMovingLineTexts = true,
        )
        val changed = basic.changed?.let { changedPattern ->
            buildHexagram(
                pattern = changedPattern,
                movingPositions = emptySet(),
                voidBranches = voidBranches.toSet(),
                spirits = spirits,
                includeMovingLineTexts = false,
            )
        }

        return DivinationResult(
            timeInfo = timeInfo,
            voidBranches = voidBranches,
            original = original,
            changed = changed,
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
