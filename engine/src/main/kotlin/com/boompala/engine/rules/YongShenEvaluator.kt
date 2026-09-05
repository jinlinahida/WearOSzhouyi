package com.boompala.engine.rules

import com.boompala.engine.model.DivinationResult
import com.boompala.engine.model.EarthlyBranch
import com.boompala.engine.model.FiveElement
import com.boompala.engine.model.SixRelation
import com.boompala.engine.model.YaoPosition
import com.boompala.engine.model.YaoStatus

enum class YongShenCategory(
    val displayName: String,
    val targetRelation: SixRelation?,
) {
    SHI_YAO("世爻(自身)", null),
    WEALTH("财运(妻财)", SixRelation.WEALTH),
    OFFICER("事业(官鬼)", SixRelation.OFFICER_GHOST),
    PARENTS("文书/长辈(父母)", SixRelation.PARENTS),
    OFFSPRING("健康/解忧(子孙)", SixRelation.OFFSPRING),
    SIBLINGS("同行/竞争(兄弟)", SixRelation.SIBLINGS),
}

enum class YongShenStrengthLevel(
    val displayName: String,
) {
    VERY_STRONG("旺相极佳"),
    STRONG("旺相得用"),
    NEUTRAL("平稳无虞"),
    WEAK("休囚衰弱"),
    BROKEN("破散休囚"),
}

data class YongShenEvaluation(
    val category: YongShenCategory,
    val targetPosition: YaoPosition,
    val relationDisplayName: String,
    val ganzhiDisplayName: String,
    val isFuShen: Boolean,
    val strengthLevel: YongShenStrengthLevel,
    val monthEffect: String,
    val dayEffect: String,
    val summary: String,
)

object YongShenEvaluator {

    fun evaluate(
        result: DivinationResult,
        category: YongShenCategory,
    ): YongShenEvaluation? {
        val original = result.original
        val monthBranch = result.monthGanzhi.earthlyBranch
        val dayBranch = result.dayGanzhi.earthlyBranch

        var targetPosition: YaoPosition? = null
        var relationDisplayName = ""
        var ganzhiDisplayName = ""
        var targetElement: FiveElement? = null
        var targetBranch: EarthlyBranch? = null
        var isFuShen = false
        var feiFuInfo: com.boompala.engine.model.FuShenInfo? = null
        var yaoStatuses: Set<YaoStatus> = emptySet()

        if (category.targetRelation == null) {
            // 世爻
            val shiYao = original.yaoFromBottom.single { it.isShi }
            targetPosition = shiYao.position
            relationDisplayName = shiYao.sixRelation.displayName
            ganzhiDisplayName = shiYao.heavenlyStem.displayName + shiYao.earthlyBranch.displayName
            targetElement = shiYao.element
            targetBranch = shiYao.earthlyBranch
            yaoStatuses = shiYao.statuses
        } else {
            val matchingYao = original.yaoFromBottom.firstOrNull { it.sixRelation == category.targetRelation }
            if (matchingYao != null) {
                targetPosition = matchingYao.position
                relationDisplayName = matchingYao.sixRelation.displayName
                ganzhiDisplayName = matchingYao.heavenlyStem.displayName + matchingYao.earthlyBranch.displayName
                targetElement = matchingYao.element
                targetBranch = matchingYao.earthlyBranch
                yaoStatuses = matchingYao.statuses
            } else {
                // 查找伏神
                val fuShenYao = original.yaoFromBottom.firstOrNull { it.fuShen?.sixRelation == category.targetRelation }
                if (fuShenYao != null && fuShenYao.fuShen != null) {
                    val fu = fuShenYao.fuShen!!
                    targetPosition = fu.position
                    relationDisplayName = fu.sixRelation.displayName + "(伏)"
                    ganzhiDisplayName = fu.heavenlyStem.displayName + fu.earthlyBranch.displayName
                    targetElement = fu.element
                    targetBranch = fu.earthlyBranch
                    feiFuInfo = fu
                    isFuShen = true

                    val mockFuYao = com.boompala.engine.model.Yao(
                        position = fu.position,
                        yinYang = com.boompala.engine.model.YaoPolarity.YANG,
                        moving = false,
                        heavenlyStem = fu.heavenlyStem,
                        earthlyBranch = fu.earthlyBranch,
                        element = fu.element,
                        sixRelation = fu.sixRelation,
                        sixSpirit = com.boompala.engine.model.SixSpirit.AZURE_DRAGON,
                        isShi = false,
                        isYing = false,
                        isVoid = fu.earthlyBranch in result.voidBranches,
                    )
                    yaoStatuses = LiuYaoStatusRules.evaluateYaoStatuses(
                        yao = mockFuYao,
                        monthGanzhi = result.monthGanzhi,
                        dayGanzhi = result.dayGanzhi,
                    )
                }
            }
        }

        if (targetPosition == null || targetElement == null || targetBranch == null) {
            return null
        }

        // 月令关系
        val isMonthBroken = yaoStatuses.contains(YaoStatus.MONTH_BROKEN)
        val monthEffect = when {
            isMonthBroken -> "月破(休囚大凶)"
            targetBranch == monthBranch -> "临月建(大旺)"
            monthBranch.element.generates(targetElement) -> "得月建生助(相)"
            monthBranch.element == targetElement -> "与月建比和(旺)"
            targetElement.generates(monthBranch.element) -> "生月建耗泄(休)"
            targetElement.overcomes(monthBranch.element) -> "克月建耗气(囚)"
            else -> "受月建克制(死/衰)"
        }

        // 日辰关系
        val dayEffect = when {
            yaoStatuses.contains(YaoStatus.DARK_MOVING) -> "日辰暗动(动而有效)"
            yaoStatuses.contains(YaoStatus.DAY_BROKEN) -> "日辰冲散(日破)"
            yaoStatuses.contains(YaoStatus.DAY_CLASHED) -> "日冲动爻"
            targetBranch == dayBranch -> "临日建(大旺)"
            dayBranch.element.generates(targetElement) -> "得日辰相生(旺)"
            dayBranch.element == targetElement -> "与日辰比和(旺)"
            targetElement.generates(dayBranch.element) -> "生日辰耗泄"
            targetElement.overcomes(dayBranch.element) -> "克日辰耗气"
            else -> "受日辰克制(衰)"
        }

        // 综合旺衰评级
        val isMonthStrong = monthEffect.contains("旺") || monthEffect.contains("生助") || monthEffect.contains("比和")
        val isDayStrong = dayEffect.contains("旺") || dayEffect.contains("暗动") || dayEffect.contains("相生") || dayEffect.contains("比和")

        val strengthLevel = when {
            isMonthBroken || yaoStatuses.contains(YaoStatus.DAY_BROKEN) -> YongShenStrengthLevel.BROKEN
            isMonthStrong && isDayStrong -> YongShenStrengthLevel.VERY_STRONG
            isMonthStrong || isDayStrong -> YongShenStrengthLevel.STRONG
            monthEffect.contains("克制") && dayEffect.contains("克制") -> YongShenStrengthLevel.WEAK
            else -> YongShenStrengthLevel.NEUTRAL
        }

        val fuPrompt = if (isFuShen && feiFuInfo != null) {
            " 【伏神·${feiFuInfo.feiFuRelation.displayName}】"
        } else if (isFuShen) {
            " 【伏神伏藏】"
        } else ""

        val summary = "用神：${category.displayName} · $ganzhiDisplayName$fuPrompt。$monthEffect，$dayEffect。结论：${strengthLevel.displayName}。"

        return YongShenEvaluation(
            category = category,
            targetPosition = targetPosition,
            relationDisplayName = relationDisplayName,
            ganzhiDisplayName = ganzhiDisplayName,
            isFuShen = isFuShen,
            strengthLevel = strengthLevel,
            monthEffect = monthEffect,
            dayEffect = dayEffect,
            summary = summary,
        )
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
