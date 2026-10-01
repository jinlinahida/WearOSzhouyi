package com.boompala.engine.ai

import com.boompala.engine.bazi.BaziGender
import com.boompala.engine.model.DivinationResult
import com.boompala.engine.model.HexagramStatus
import com.boompala.engine.model.Yao
import com.boompala.engine.model.YaoPosition
import com.boompala.engine.model.YaoStatus
import com.boompala.engine.rules.YongShenCategory
import com.boompala.engine.rules.YongShenEvaluator
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * 六爻排盘 AI Prompt 构建引擎。
 *
 * 严格按照传统六爻纳甲易理，将排盘结果转化为结构化易学上下文，
 * 并生成严谨、自洽、针对 Wear OS 小屏优化的 System Prompt 与 User Prompt。
 */
object AiPromptBuilder {

    private val solarDateFormatter = DateTimeFormatter.ofPattern("yyyy年M月d日 HH:mm", Locale.CHINA)

    fun buildPrompt(
        result: DivinationResult,
        topic: AiDivinationTopic = AiDivinationTopic.GENERAL,
        question: String? = null,
        userGender: BaziGender? = null,
    ): AiPrompt {
        val sanitizedQuestion = question?.trim()?.take(200)?.ifBlank { null }
        val context = extractDivinationContext(result, topic, sanitizedQuestion, userGender)

        val systemPrompt = buildSystemPrompt()
        val userPrompt = buildUserPrompt(context)

        return AiPrompt(
            systemPrompt = systemPrompt,
            userPrompt = userPrompt,
            context = context,
            metadata = mapOf(
                "topic" to topic.id,
                "originalHexagram" to result.original.name,
                "changedHexagram" to (result.changed?.name ?: "无"),
                "hasMovingLines" to result.yaoFromBottom.any { it.moving }.toString(),
            ),
        )
    }

    private fun extractDivinationContext(
        result: DivinationResult,
        topic: AiDivinationTopic,
        userQuestion: String?,
        userGender: BaziGender?,
    ): AiDivinationContext {
        val timeInfo = result.timeInfo
        val castDateSolar = solarDateFormatter.format(timeInfo.gregorianDateTime)
        val castDateLunar = timeInfo.lunarDate
        val ganzhiFourPillars = "${timeInfo.yearGanzhi.displayName}年 ${timeInfo.monthGanzhi.displayName}月 ${timeInfo.dayGanzhi.displayName}日 ${timeInfo.hourGanzhi.displayName}时"
        val monthBranch = result.monthGanzhi.earthlyBranch.displayName
        val dayBranch = result.dayGanzhi.earthlyBranch.displayName
        val voidBranches = result.voidBranches.joinToString("") { it.displayName } + "空"

        // 卦象基本信息
        val orig = result.original
        val originalHexagramName = orig.name
        val originalPalace = "${orig.palace.displayName}宫 (${orig.palace.element.displayName}) · ${orig.palaceStage.displayName}"
        val originalStatuses = orig.statuses.map { it.displayName }

        val changedHexagramName = result.changed?.name
        val changedPalace = result.changed?.let { "${it.palace.displayName}宫 (${it.palace.element.displayName})" }
        val changedStatuses = result.changed?.statuses?.map { it.displayName }.orEmpty()

        // 世应爻
        val shiYao = orig.yaoFromBottom.single { it.isShi }
        val yingYao = orig.yaoFromBottom.single { it.isYing }
        val shiYingDescription = buildString {
            append("世爻居【${shiYao.position.displayName}】：${shiYao.sixRelation.displayName}${shiYao.earthlyBranch.displayName}${shiYao.element.displayName}，临${shiYao.sixSpirit.displayName}")
            if (shiYao.isVoid) append("(旬空)")
            if (shiYao.statuses.isNotEmpty()) append("(${shiYao.statuses.joinToString("、") { it.displayName }})")
            append("；应爻居【${yingYao.position.displayName}】：${yingYao.sixRelation.displayName}${yingYao.earthlyBranch.displayName}${yingYao.element.displayName}，临${yingYao.sixSpirit.displayName}")
            if (yingYao.isVoid) append("(旬空)")
            if (yingYao.statuses.isNotEmpty()) append("(${yingYao.statuses.joinToString("、") { it.displayName }})")
        }

        // 用神评估
        val targetCategory = topic.toPrimaryYongShenCategory(userGender)
        val yongShenEval = YongShenEvaluator.evaluate(result, targetCategory)
        val shiEval = YongShenEvaluator.evaluate(result, YongShenCategory.SHI_YAO)
        val yongShenDescription = buildString {
            append("求测类别【${topic.displayName}】。")
            if (topic == AiDivinationTopic.RELATIONSHIP) {
                append("（注：男测以妻财为用神，女测以官鬼为用神。当前判定：${userGender?.displayNameZh ?: "未注明"}，主看${targetCategory.displayName}兼看应爻）")
            }
            if (yongShenEval != null) {
                append(yongShenEval.summary)
            } else {
                append("主用神未能明确取爻或伏神休囚无现。")
            }
            if (shiEval != null && targetCategory != YongShenCategory.SHI_YAO) {
                append(" 身世状态：${shiEval.summary}")
            }
        }

        // 动爻变爻
        val movingLines = orig.yaoFromBottom.filter { it.moving }
        val movingLinesSummary = if (movingLines.isEmpty()) {
            listOf("六爻安静（静卦），无动爻变爻。静卦以世应生克及各爻临月日衰旺为主导。")
        } else {
            movingLines.map { yao ->
                val changedYao = result.changed?.yaoFromBottom?.getOrNull(yao.position.indexFromBottom)
                val statusStr = if (yao.statuses.isNotEmpty()) " [${yao.statuses.joinToString("、") { it.displayName }}]" else ""
                val changedStr = if (changedYao != null) {
                    " -> 变出【${changedYao.sixRelation.displayName}${changedYao.earthlyBranch.displayName}${changedYao.element.displayName}】"
                } else ""
                "${yao.position.displayName}发动：${yao.sixRelation.displayName}${yao.earthlyBranch.displayName}${yao.element.displayName}，临${yao.sixSpirit.displayName}$statusStr$changedStr"
            }
        }

        // 初爻到上爻全部明细
        val lineDetails = orig.yaoFromBottom.map { yao ->
            val changedYao = result.changed?.yaoFromBottom?.getOrNull(yao.position.indexFromBottom)
            val fuShenStr = yao.fuShen?.let { fu ->
                "伏神：${fu.sixRelation.displayName}${fu.earthlyBranch.displayName}${fu.element.displayName} (${fu.feiFuRelation.displayName})"
            }
            AiLineDetail(
                position = yao.position,
                polarityName = yao.yinYang.displayName,
                isMoving = yao.moving,
                stemBranchName = yao.heavenlyStem.displayName + yao.earthlyBranch.displayName,
                elementName = yao.element.displayName,
                relationName = yao.sixRelation.displayName,
                spiritName = yao.sixSpirit.displayName,
                isShi = yao.isShi,
                isYing = yao.isYing,
                isVoid = yao.isVoid,
                statuses = yao.statuses.map { it.displayName },
                fuShenSummary = fuShenStr,
                changedStemBranchName = changedYao?.let { it.heavenlyStem.displayName + it.earthlyBranch.displayName },
                changedRelationName = changedYao?.sixRelation?.displayName,
                changedElementName = changedYao?.element?.displayName,
            )
        }

        return AiDivinationContext(
            topic = topic,
            userQuestion = userQuestion,
            userGender = userGender,
            castDateSolar = castDateSolar,
            castDateLunar = castDateLunar,
            ganzhiFourPillars = ganzhiFourPillars,
            monthBranch = monthBranch,
            dayBranch = dayBranch,
            voidBranches = voidBranches,
            originalHexagramName = originalHexagramName,
            originalPalace = originalPalace,
            originalStatuses = originalStatuses,
            changedHexagramName = changedHexagramName,
            changedPalace = changedPalace,
            changedStatuses = changedStatuses,
            shiYingDescription = shiYingDescription,
            yongShenDescription = yongShenDescription,
            movingLinesSummary = movingLinesSummary,
            lines = lineDetails,
        )
    }

    private fun buildSystemPrompt(): String {
        return """
你是一位兼具深厚传统易学功底与现代思辨智慧的资深六爻占断大师。你受命为智能手表（Wear OS）用户进行严谨、精炼、客观的六爻解卦。

【最高推导准则】
1. 只能依据用户输入的真实卦象数据推导，严禁虚构、篡改或杜撰不存在的卦名、爻位、六亲、干支、动爻或世应。
2. 卦象事实与主观解释必须严格区分，一切结论必须指出明确的卦爻依据（如月建生克、日辰冲合、旬空、动变进退等）。
3. 杜绝空洞套话或迷信恐吓，以求实、平和、启发性的口吻指引用户审时度势。
4. 明确易经占断揭示的是当下的事物运动趋势与吉凶转化规律，提供战略与心态参考，不具备现代实证科学的宿命确定性。

【排版与字数规范】
为适应 Wear OS 手表小屏阅读，解卦全文总字数请控制在 300～500 中文字内，语言凝练精准，必须严格采用以下四段式结构（每段包含对应小标题）：

【核心判断】
一针见血指出所测事项的核心定性与吉凶顺逆走向（1-2句话）。

【卦象依据】
陈述本卦变卦格局、世应生克关系、用神在月日令下的旺衰生克及动爻变爻的关键指征。

【趋势分析】
阐述事态发展的阶段演变、转折契机或潜在隐患。

【建议】
给出务实、理性、符合事理人情的行动指引与心性调节策略。
""".trimIndent()
    }

    private fun buildUserPrompt(context: AiDivinationContext): String {
        return buildString {
            appendLine("【求测信息】")
            appendLine("• 求测事项：${context.topic.displayName} (${context.topic.description})")
            if (context.userQuestion != null) {
                appendLine("• 具体问事：${context.userQuestion}")
            }
            if (context.userGender != null) {
                appendLine("• 求测者性别：${context.userGender.titleZh}")
            }
            appendLine("• 起卦时间：公历 ${context.castDateSolar} · 农历 ${context.castDateLunar}")
            appendLine("• 四柱干支：${context.ganzhiFourPillars}")
            appendLine("• 旬空地支：${context.voidBranches}")
            appendLine()

            appendLine("【卦象大局】")
            appendLine("• 本卦：【${context.originalHexagramName}】（${context.originalPalace}）${if (context.originalStatuses.isNotEmpty()) "，卦格：" + context.originalStatuses.joinToString("、") else ""}")
            if (context.changedHexagramName != null) {
                appendLine("• 变卦：【${context.changedHexagramName}】（${context.changedPalace}）${if (context.changedStatuses.isNotEmpty()) "，卦格：" + context.changedStatuses.joinToString("、") else ""}")
            } else {
                appendLine("• 变卦：无（六爻安静之静卦）")
            }
            appendLine("• 世应配置：${context.shiYingDescription}")
            appendLine("• 用神旺衰分析：${context.yongShenDescription}")
            appendLine()

            appendLine("【动变指征】")
            context.movingLinesSummary.forEach { appendLine("• $it") }
            appendLine()

            appendLine("【全盘六爻排盘明细（注：从上爻至初爻呈现）】")
            context.lines.asReversed().forEach { line ->
                val posTag = "[${line.position.displayName}]"
                val movingTag = if (line.isMoving) "(动)" else "(静)"
                val shiYingTag = when {
                    line.isShi -> "[世]"
                    line.isYing -> "[应]"
                    else -> ""
                }
                val voidTag = if (line.isVoid) "[空]" else ""
                val statusTag = if (line.statuses.isNotEmpty()) "[${line.statuses.joinToString(",")}]" else ""
                val changedTag = if (line.isMoving && line.changedRelationName != null) {
                    " -> 变${line.changedRelationName}${line.changedStemBranchName}${line.changedElementName}"
                } else ""
                val fuTag = if (line.fuShenSummary != null) " {${line.fuShenSummary}}" else ""

                appendLine("$posTag ${line.polarityName}爻 ${line.relationName}${line.stemBranchName}${line.elementName} · ${line.spiritName} $shiYingTag$voidTag$statusTag$movingTag$changedTag$fuTag".replace("\\s+".toRegex(), " ").trim())
            }
            appendLine()
            appendLine("请严格依据上述客观卦爻事实，按照【核心判断】、【卦象依据】、【趋势分析】、【建议】四段式，为我进行严谨精炼的解卦分析。")
        }
    }
}
