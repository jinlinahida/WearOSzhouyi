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
    val compactDisplayName: String = displayName,
) {
    SHI_YAO("世爻(自身)", null, "世爻·自身"),
    WEALTH("财运(妻财)", SixRelation.WEALTH, "妻财·求财"),
    OFFICER("事业(官鬼)", SixRelation.OFFICER_GHOST, "官鬼·事业"),
    PARENTS("文书/长辈(父母)", SixRelation.PARENTS, "父母·文书"),
    OFFSPRING("健康/解忧(子孙)", SixRelation.OFFSPRING, "子孙·健康"),
    SIBLINGS("同行/竞争(兄弟)", SixRelation.SIBLINGS, "兄弟·同行"),
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
    val shiYongRelation: String? = null,
    val originAndHostileInfo: String? = null,
    val timingClue: String? = null,
)

object YongShenEvaluator {

    fun FiveElement.generates(other: FiveElement): Boolean = when (this) {
        FiveElement.WOOD -> other == FiveElement.FIRE
        FiveElement.FIRE -> other == FiveElement.EARTH
        FiveElement.EARTH -> other == FiveElement.METAL
        FiveElement.METAL -> other == FiveElement.WATER
        FiveElement.WATER -> other == FiveElement.WOOD
    }

    fun FiveElement.overcomes(other: FiveElement): Boolean = when (this) {
        FiveElement.WOOD -> other == FiveElement.EARTH
        FiveElement.EARTH -> other == FiveElement.WATER
        FiveElement.WATER -> other == FiveElement.FIRE
        FiveElement.FIRE -> other == FiveElement.METAL
        FiveElement.METAL -> other == FiveElement.WOOD
    }

    fun EarthlyBranch.clashingBranch(): EarthlyBranch = when (this) {
        EarthlyBranch.ZI -> EarthlyBranch.WU
        EarthlyBranch.CHOU -> EarthlyBranch.WEI
        EarthlyBranch.YIN -> EarthlyBranch.SHEN
        EarthlyBranch.MAO -> EarthlyBranch.YOU
        EarthlyBranch.CHEN -> EarthlyBranch.XU
        EarthlyBranch.SI -> EarthlyBranch.HAI
        EarthlyBranch.WU -> EarthlyBranch.ZI
        EarthlyBranch.WEI -> EarthlyBranch.CHOU
        EarthlyBranch.SHEN -> EarthlyBranch.YIN
        EarthlyBranch.YOU -> EarthlyBranch.MAO
        EarthlyBranch.XU -> EarthlyBranch.CHEN
        EarthlyBranch.HAI -> EarthlyBranch.SI
    }

    fun SixRelation.originRelation(): SixRelation = when (this) {
        SixRelation.PARENTS -> SixRelation.OFFICER_GHOST
        SixRelation.OFFICER_GHOST -> SixRelation.WEALTH
        SixRelation.WEALTH -> SixRelation.OFFSPRING
        SixRelation.OFFSPRING -> SixRelation.SIBLINGS
        SixRelation.SIBLINGS -> SixRelation.PARENTS
    }

    fun SixRelation.hostileRelation(): SixRelation = when (this) {
        SixRelation.PARENTS -> SixRelation.WEALTH
        SixRelation.OFFICER_GHOST -> SixRelation.OFFSPRING
        SixRelation.WEALTH -> SixRelation.SIBLINGS
        SixRelation.OFFSPRING -> SixRelation.PARENTS
        SixRelation.SIBLINGS -> SixRelation.OFFICER_GHOST
    }

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

        // 世爻与用神生克
        val shiYao = original.yaoFromBottom.single { it.isShi }
        val shiElement = shiYao.element
        val shiYongRelation = if (category == YongShenCategory.SHI_YAO || targetPosition == shiYao.position) {
            "用神持世（近水楼台，所谋之事与自身紧密相连，自身掌握主动权）"
        } else {
            when {
                targetElement.generates(shiElement) -> "用神生世（用生世为大吉，事来就我，谋求顺遂易成）"
                shiElement.generates(targetElement) -> "世生用神（世生用为泄气，我去生事，多劳少逸或须经辛劳而后成）"
                shiElement.overcomes(targetElement) -> "世克用神（世克用为克制驾驭，事虽费力但终能克服拿下）"
                targetElement.overcomes(shiElement) -> "用克世爻（用克世为受阻受制，事来伤我或恐受拖累，大凶或阻力极大）"
                else -> "世用比和（五行同气相求，平稳合和）"
            }
        }

        // 原神与忌神辨析
        val targetRelation = category.targetRelation
        val originAndHostileInfo = if (targetRelation != null) {
            val originRel = targetRelation.originRelation()
            val hostileRel = targetRelation.hostileRelation()

            val movingYao = original.yaoFromBottom.filter { it.moving }
            val movingOrigin = movingYao.filter { it.sixRelation == originRel }
            val movingHostile = movingYao.filter { it.sixRelation == hostileRel }

            buildString {
                append("原神为【${originRel.displayName}】，忌神为【${hostileRel.displayName}】。")
                when {
                    movingHostile.isNotEmpty() && movingOrigin.isNotEmpty() ->
                        append("忌神与原神皆发动，形成生克混战，吉凶视变爻动化生克定夺。")
                    movingHostile.isNotEmpty() -> {
                        val posNames = movingHostile.joinToString("、") { it.position.displayName }
                        append("⚠️ 忌神在${posNames}发动克害用神，事必遇强劲阻碍或小人争夺！")
                    }
                    movingOrigin.isNotEmpty() -> {
                        val posNames = movingOrigin.joinToString("、") { it.position.displayName }
                        append("✨ 原神在${posNames}发动生助用神，有强大外力相扶或贵人暗助，凶可化吉！")
                    }
                    else ->
                        append("原神与忌神皆安静，事态平稳，吉凶主要依日月令及世用本身生克为凭。")
                }
            }
        } else {
            "世爻为主体，观世应生克与动爻吉凶。"
        }

        // 用神旬空、破散与应期线索
        val isVoid = targetBranch in result.voidBranches
        val clashingBranch = targetBranch.clashingBranch()
        val timingClue = buildString {
            if (isVoid) {
                append("用神逢空（${targetBranch.displayName}空亡）。六爻应期铁律：吉事逢空不就，凶事逢空不凶；断应期多在出空之日（临${targetBranch.displayName}之日月）或冲空之时（逢${clashingBranch.displayName}之日月），转机显现。")
            } else if (isMonthBroken) {
                append("用神遭月建冲破（月破休囚），当下阻力最深，断应期须待出月或实破之期（逢${targetBranch.displayName}、${clashingBranch.displayName}日月）。")
            } else if (yaoStatuses.contains(YaoStatus.DARK_MOVING)) {
                append("用神旺相逢日辰冲动为暗动，主事发迅速、暗中有动向，应期极为迅捷。")
            } else {
                append("用神安静，断应期多以逢冲（临${clashingBranch.displayName}日月）或逢值（临${targetBranch.displayName}日月）为事发契机。")
            }
        }

        val summary = buildString {
            append("用神：${category.displayName} · $ganzhiDisplayName$fuPrompt。$monthEffect，$dayEffect。结论：${strengthLevel.displayName}。")
            append(" 世用关系：$shiYongRelation。")
            if (targetRelation != null) {
                append(" $originAndHostileInfo")
            }
            append(" 应期线索：$timingClue")
        }

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
            shiYongRelation = shiYongRelation,
            originAndHostileInfo = originAndHostileInfo,
            timingClue = timingClue,
        )
    }
}
