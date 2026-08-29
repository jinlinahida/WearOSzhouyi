package com.boompala.engine.dailyfortune

import com.boompala.engine.bazi.BaziProfile
import com.boompala.engine.model.EarthlyBranch
import com.boompala.engine.model.FiveElement
import com.boompala.engine.model.HeavenlyStem
import com.boompala.engine.model.HexagramPattern
import com.boompala.engine.rules.HexagramRules

/**
 * 纯函数计算器：推导个人生辰八字在特定自然日的专属流日运势。
 *
 * 遵循子平命理与传统干支神煞口径，纯 Kotlin 实现，零外部网络依赖，严格可复现。
 */
object PersonalFortuneEvaluator {

    fun evaluate(
        profile: BaziProfile,
        reading: DailyFortuneReading,
    ): PersonalDailyFortune {
        val dayMaster = profile.dayMaster
        val todayStem = reading.dayGanzhi.heavenlyStem
        val todayBranch = reading.dayGanzhi.earthlyBranch
        val userDayBranch = profile.dayPillar.ganzhi.earthlyBranch
        val userYearBranch = profile.yearPillar.ganzhi.earthlyBranch

        // 1. 推导流日十神
        val shiShen = calculateShiShen(dayMaster, todayStem)
        val (themeTitle, themeAdvice) = shiShenThemeAndAdvice(shiShen)

        // 2. 收集干支合冲与神煞事件
        val events = mutableListOf<FortuneEvent>()

        // 2.1 天干五合（天干相合）
        if (isStemCombine(dayMaster, todayStem)) {
            events.add(
                FortuneEvent(
                    title = "天干相合 · 日主逢合",
                    description = "今日流日天干与日元相合，主诸事顺遂、人缘和美、得道多助",
                    isAuspicious = true,
                )
            )
        }

        // 2.2 地支六冲（逢冲防范）
        if (isBranchClash(todayBranch, userDayBranch)) {
            events.add(
                FortuneEvent(
                    title = "日支相冲 · 步调宜缓",
                    description = "今日日支与自身日支相冲，气场略显动荡，宜保持沉稳，防急躁冒进",
                    isAuspicious = false,
                )
            )
        }
        if (isBranchClash(todayBranch, userYearBranch)) {
            events.add(
                FortuneEvent(
                    title = "生肖逢冲 · 以稳为主",
                    description = "今日流日冲本命属相${profile.shengXiao}，出行留意安全，重要决策多斟酌",
                    isAuspicious = false,
                )
            )
        }

        // 2.3 地支六合（暗合助力）
        if (isBranchCombine(todayBranch, userDayBranch)) {
            events.add(
                FortuneEvent(
                    title = "日支六合 · 暗合得助",
                    description = "今日日支与自身日支六合，主暗中有助、和美顺畅、凡谋易就",
                    isAuspicious = true,
                )
            )
        } else if (isBranchCombine(todayBranch, userYearBranch)) {
            events.add(
                FortuneEvent(
                    title = "生肖逢合 · 贵人相照",
                    description = "今日地支与本命属相六合，人脉亨通，诸事顺利",
                    isAuspicious = true,
                )
            )
        }

        // 2.4 本命值日
        if (todayBranch == userYearBranch) {
            events.add(
                FortuneEvent(
                    title = "本命值日 · 气场饱满",
                    description = "今日地支与本命生肖主气相同，精力充沛，从容定夺",
                    isAuspicious = true,
                )
            )
        }

        // 2.5 神煞：天乙贵人
        if (isTianYiGuiRen(dayMaster, todayBranch)) {
            events.add(
                FortuneEvent(
                    title = "天乙贵人值日",
                    description = "天乙贵人乃逢凶化吉第一吉神，今日易得贵人照拂与化解助益",
                    isAuspicious = true,
                )
            )
        }

        // 2.6 神煞：文昌贵人
        if (isWenChang(dayMaster, todayBranch)) {
            events.add(
                FortuneEvent(
                    title = "文昌吉星照应",
                    description = "文昌主管学业考运与灵感，今日才思敏捷、理解透彻，利于创作与钻研",
                    isAuspicious = true,
                )
            )
        }

        // 2.7 神煞：驿马星
        if (isYiMa(userDayBranch, userYearBranch, todayBranch)) {
            events.add(
                FortuneEvent(
                    title = "驿马动星值日",
                    description = "驿马主奔波拓展，今日适宜差旅出行、拜访交流，忌墨守成规",
                    isAuspicious = true,
                )
            )
        }

        // 2.8 神煞：桃花吉星
        if (isTaoHua(userDayBranch, userYearBranch, todayBranch)) {
            events.add(
                FortuneEvent(
                    title = "天喜桃花照耀",
                    description = "桃花主情缘与人脉感染力，今日沟通如沐春风，人际亲和力极佳",
                    isAuspicious = true,
                )
            )
        }

        // 3. 专属平衡幸运色与数字（针对日主五行补益）
        val balanceElement = when {
            // 今日五行克制日主 -> 取印星五行通关生身
            DailyFortuneRules.elementOvercoming(dayMaster.element) == todayStem.element ->
                DailyFortuneRules.elementGenerating(dayMaster.element)
            // 今日五行泄耗日主 -> 取生我者印星
            DailyFortuneRules.elementGenerating(todayStem.element) == dayMaster.element ->
                DailyFortuneRules.elementGenerating(dayMaster.element)
            // 平和状态 -> 取日主自身五行或印星五行生旺
            else -> DailyFortuneRules.elementGenerating(dayMaster.element)
        }
        val balanceColor = DailyFortuneRules.colorFor(balanceElement)
        val balanceNumbers = DailyFortuneRules.luckyNumbers(balanceElement)

        // 4. 周易值日卦五行与日主感应
        val hexPattern = HexagramPattern.fromCode(reading.dayHexagramCode)
        val hexClassification = HexagramRules.classify(hexPattern)
        val hexElement = hexClassification.palace.element
        val hexResonance = calculateHexResonance(dayMaster.element, hexElement)

        return PersonalDailyFortune(
            dayMaster = dayMaster,
            dayMasterElement = dayMaster.element,
            shiShenName = shiShen,
            themeTitle = themeTitle,
            themeAdvice = themeAdvice,
            events = events,
            balanceColor = balanceColor,
            balanceNumbers = balanceNumbers,
            hexagramResonance = hexResonance,
        )
    }

    private fun calculateShiShen(dayMaster: HeavenlyStem, target: HeavenlyStem): String {
        val samePolarity = (dayMaster.index % 2) == (target.index % 2)
        val masterEl = dayMaster.element
        val targetEl = target.element

        return when {
            masterEl == targetEl -> if (samePolarity) "比肩" else "劫财"
            DailyFortuneRules.elementGenerating(targetEl) == masterEl -> if (samePolarity) "食神" else "伤官"
            DailyFortuneRules.elementOvercoming(targetEl) == masterEl -> if (samePolarity) "偏财" else "正财"
            DailyFortuneRules.elementOvercoming(masterEl) == targetEl -> if (samePolarity) "七杀" else "正官"
            DailyFortuneRules.elementGenerating(masterEl) == targetEl -> if (samePolarity) "偏印" else "正印"
            else -> "比肩"
        }
    }

    private fun shiShenThemeAndAdvice(shiShen: String): Pair<String, String> = when (shiShen) {
        "比肩" -> "同道共勉 · 信心充沛" to "利团队研讨与朋友聚首，自我认同感高，宜从容决断"
        "劫财" -> "社交活跃 · 豪爽大度" to "人脉往来频繁，宜放宽心量，消费注意节制，防冲动开支"
        "食神" -> "才思舒展 · 福气安康" to "灵感迸发、心态闲适，利文创构思、艺术表达与美食品鉴"
        "伤官" -> "锋芒创新 · 敢为人先" to "破局意识强、执行力卓越，注意谦逊沟通，凡事留有余地"
        "偏财" -> "商业机缘 · 视野开阔" to "嗅觉敏锐、思路活跃，利拓展人脉、发现新机，忌急功近利"
        "正财" -> "务实稳健 · 本职精进" to "脚踏实地、有条不紊，利工作推进、财务盘点与契约达成"
        "七杀" -> "魄力攻坚 · 迎难而上" to "雷厉风行、直面挑战，宜多一分沉着冷静，注意劳逸均衡"
        "正官" -> "自律威仪 · 贵人垂青" to "责任感强、条理分明，利对接长辈权威、整顿规划与公事办理"
        "偏印" -> "敏锐洞见 · 探索新知" to "直觉通透、钻研专精，利技术攻坚与深层思考，防胡思乱想"
        "正印" -> "温润涵养 · 润泽身心" to "心态祥和、得助提携，利学习深造、阅读休整与身心充能"
        else -> "从容自得 · 顺遂安泰" to "保持内心宁静，行事顺势而为，自能万物化生"
    }

    /** 天干五合：甲己、乙庚、丙辛、丁壬、戊癸 */
    private fun isStemCombine(s1: HeavenlyStem, s2: HeavenlyStem): Boolean {
        val i1 = s1.index
        val i2 = s2.index
        return (i1 == 0 && i2 == 5) || (i1 == 5 && i2 == 0) ||
               (i1 == 1 && i2 == 6) || (i1 == 6 && i2 == 1) ||
               (i1 == 2 && i2 == 7) || (i1 == 7 && i2 == 2) ||
               (i1 == 3 && i2 == 8) || (i1 == 8 && i2 == 3) ||
               (i1 == 4 && i2 == 9) || (i1 == 9 && i2 == 4)
    }

    /** 地支六冲：子午、丑未、寅申、卯酉、辰戌、巳亥 (相隔6位) */
    private fun isBranchClash(b1: EarthlyBranch, b2: EarthlyBranch): Boolean =
        (b1.index - b2.index + 12) % 12 == 6

    /** 地支六合：子丑(0-1)、寅亥(2-11)、卯戌(3-10)、辰酉(4-9)、巳申(5-8)、午未(6-7) */
    private fun isBranchCombine(b1: EarthlyBranch, b2: EarthlyBranch): Boolean {
        val sum = b1.index + b2.index
        return when {
            (b1.index == 0 && b2.index == 1) || (b1.index == 1 && b2.index == 0) -> true
            sum == 13 -> true
            else -> false
        }
    }

    /** 天乙贵人：甲戊庚见丑未，乙己见子申，丙丁见亥酉，六辛逢马虎，壬癸见兔蛇 */
    private fun isTianYiGuiRen(dayMaster: HeavenlyStem, branch: EarthlyBranch): Boolean {
        val targets = when (dayMaster) {
            HeavenlyStem.JIA, HeavenlyStem.WU, HeavenlyStem.GENG -> listOf(EarthlyBranch.CHOU, EarthlyBranch.WEI)
            HeavenlyStem.YI, HeavenlyStem.JI -> listOf(EarthlyBranch.ZI, EarthlyBranch.SHEN)
            HeavenlyStem.BING, HeavenlyStem.DING -> listOf(EarthlyBranch.HAI, EarthlyBranch.YOU)
            HeavenlyStem.XIN -> listOf(EarthlyBranch.WU, EarthlyBranch.YIN)
            HeavenlyStem.REN, HeavenlyStem.GUI -> listOf(EarthlyBranch.MAO, EarthlyBranch.SI)
        }
        return branch in targets
    }

    /** 文昌贵人：甲巳、乙午、丙戊申、丁己酉、庚亥、辛子、壬寅、癸卯 */
    private fun isWenChang(dayMaster: HeavenlyStem, branch: EarthlyBranch): Boolean {
        val target = when (dayMaster) {
            HeavenlyStem.JIA -> EarthlyBranch.SI
            HeavenlyStem.YI -> EarthlyBranch.WU
            HeavenlyStem.BING, HeavenlyStem.WU -> EarthlyBranch.SHEN
            HeavenlyStem.DING, HeavenlyStem.JI -> EarthlyBranch.YOU
            HeavenlyStem.GENG -> EarthlyBranch.HAI
            HeavenlyStem.XIN -> EarthlyBranch.ZI
            HeavenlyStem.REN -> EarthlyBranch.YIN
            HeavenlyStem.GUI -> EarthlyBranch.MAO
        }
        return branch == target
    }

    /** 驿马星：申子辰马在寅，寅午戌马在申，巳酉丑马在亥，亥卯未马在巳 */
    private fun isYiMa(dayBranch: EarthlyBranch, yearBranch: EarthlyBranch, todayBranch: EarthlyBranch): Boolean {
        fun yiMaTarget(b: EarthlyBranch): EarthlyBranch = when (b) {
            EarthlyBranch.SHEN, EarthlyBranch.ZI, EarthlyBranch.CHEN -> EarthlyBranch.YIN
            EarthlyBranch.YIN, EarthlyBranch.WU, EarthlyBranch.XU -> EarthlyBranch.SHEN
            EarthlyBranch.SI, EarthlyBranch.YOU, EarthlyBranch.CHOU -> EarthlyBranch.HAI
            EarthlyBranch.HAI, EarthlyBranch.MAO, EarthlyBranch.WEI -> EarthlyBranch.SI
        }
        return todayBranch == yiMaTarget(dayBranch) || todayBranch == yiMaTarget(yearBranch)
    }

    /** 桃花星：申子辰在酉，寅午戌在卯，巳酉丑在午，亥卯未在子 */
    private fun isTaoHua(dayBranch: EarthlyBranch, yearBranch: EarthlyBranch, todayBranch: EarthlyBranch): Boolean {
        fun taoHuaTarget(b: EarthlyBranch): EarthlyBranch = when (b) {
            EarthlyBranch.SHEN, EarthlyBranch.ZI, EarthlyBranch.CHEN -> EarthlyBranch.YOU
            EarthlyBranch.YIN, EarthlyBranch.WU, EarthlyBranch.XU -> EarthlyBranch.MAO
            EarthlyBranch.SI, EarthlyBranch.YOU, EarthlyBranch.CHOU -> EarthlyBranch.WU
            EarthlyBranch.HAI, EarthlyBranch.MAO, EarthlyBranch.WEI -> EarthlyBranch.ZI
        }
        return todayBranch == taoHuaTarget(dayBranch) || todayBranch == taoHuaTarget(yearBranch)
    }

    private fun calculateHexResonance(masterElement: FiveElement, hexElement: FiveElement): String = when {
        DailyFortuneRules.elementGenerating(masterElement) == hexElement ->
            "今日值日卦象生旺日元，顺风扬帆，大有可为"
        masterElement == hexElement ->
            "今日值日卦象与日主同气连枝，干劲充沛，笃定前行"
        DailyFortuneRules.elementGenerating(hexElement) == masterElement ->
            "日元生助值日卦气，才思广进，宜积极主动施展才华"
        DailyFortuneRules.elementOvercoming(masterElement) == hexElement ->
            "借卦象修身内省，遇事沉着包容，以柔克刚"
        else ->
            "卦气与命盘平和相映，心定神凝，万物自得其所"
    }
}
