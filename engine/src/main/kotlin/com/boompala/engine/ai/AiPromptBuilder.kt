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
        val yongTargetElement = if (targetCategory == YongShenCategory.SHI_YAO) {
            orig.yaoFromBottom.single { it.isShi }.element
        } else {
            orig.yaoFromBottom.firstOrNull { it.sixRelation == targetCategory.targetRelation }?.element
                ?: orig.yaoFromBottom.firstOrNull { it.fuShen?.sixRelation == targetCategory.targetRelation }?.fuShen?.element
        }

        val movingLines = orig.yaoFromBottom.filter { it.moving }
        val movingLinesSummary = if (movingLines.isEmpty()) {
            listOf("六爻安静（静卦），无动爻变爻。静卦以世应生克及各爻临月日衰旺为主导。")
        } else {
            movingLines.map { yao ->
                val changedYao = result.changed?.yaoFromBottom?.getOrNull(yao.position.indexFromBottom)
                val statusStr = if (yao.statuses.isNotEmpty()) " [${yao.statuses.joinToString("、") { it.displayName }}]" else ""
                val changedStr = if (changedYao != null) {
                    val dongEle = yao.element
                    val bianEle = changedYao.element
                    val bianDesc = when {
                        with(YongShenEvaluator) { bianEle.generates(dongEle) } -> "回头生(大吉增力)"
                        with(YongShenEvaluator) { bianEle.overcomes(dongEle) } -> "回头克(受挫自损)"
                        with(YongShenEvaluator) { dongEle.generates(bianEle) } -> "动化泄气"
                        yao.statuses.contains(YaoStatus.ADVANCING) -> "化进神(力量倍增)"
                        yao.statuses.contains(YaoStatus.RETREATING) -> "化退神(渐行消退)"
                        else -> "动化比和"
                    }
                    " -> 变出【${changedYao.sixRelation.displayName}${changedYao.earthlyBranch.displayName}${changedYao.element.displayName}】($bianDesc)"
                } else ""

                val effectOnYong = if (yongTargetElement != null) {
                    when {
                        yao.isShi && (targetCategory == YongShenCategory.SHI_YAO || yao.sixRelation == targetCategory.targetRelation) -> "【用神持世发动】自身谋求主动"
                        yao.isShi -> "【世爻发动】求测者自身有重大动向或主动作为"
                        targetCategory.targetRelation != null && yao.sixRelation == targetCategory.targetRelation -> "【用神自身发动】事态已有明确动向"
                        targetCategory.targetRelation != null && with(YongShenEvaluator) { yao.element.generates(yongTargetElement) } -> "【原神发动】生助用神，有外力贵人相助"
                        targetCategory.targetRelation != null && with(YongShenEvaluator) { yao.element.overcomes(yongTargetElement) } -> "【忌神发动】克伐用神，构成核心阻力源"
                        else -> ""
                    }
                } else ""

                val effectStr = if (effectOnYong.isNotBlank()) " | 作用：$effectOnYong" else ""

                "${yao.position.displayName}发动：${yao.sixRelation.displayName}${yao.earthlyBranch.displayName}${yao.element.displayName}，临${yao.sixSpirit.displayName}$statusStr$changedStr$effectStr"
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
你是一位深谙周易纳甲六爻精髓的资深解卦宗师。你受命为智能手表用户进行精炼、深刻、直击本质的六爻实战断卦。

【核心推演原则】
1. 实证推断，吉凶直给：严禁虚构、篡改或杜撰不存在的卦象数据；依据客观卦爻生克推演，严禁模棱两可或泛泛而谈的端水废话；成败顺逆必须有一针见血的明确倾向断语。
2. 卦爻紧扣，因果清晰：每个结论必须指出明确的卦爻推导链条（世用生克、动爻生克、原神与忌神动静、月建日辰旺衰、动变回头生克进退）。
3. 时间节点，必断应期：根据用神旬空出空冲空、月破逢实、动变合冲规律，在趋势中明确指出事态发展与转折的关键时间节点（应期）。
4. 因卦制宜，拒绝鸡汤：杜绝“保持心态、审慎沟通”等放之四海而皆准的通识废话；建议必须根据卦中生克机制（如借助某六亲贵人、避开相冲时辰、守正或主动出击）给出具象务实的现实破局行动。

【排版与字数规范】
为适应 Wear OS 手表小屏滚动阅读，全文总字数请控制在 400～650 中文字内，语言简练老道、信息密度极高，必须严格采用以下四段式结构（每段必须包含对应标题）：

【核心判断】
一针见血指出所问事项的吉凶走向（大吉/小吉/平稳/受阻/凶险）、事情成败定性及当下求测者所处的战略态势（1-2句明确断语，绝不模棱两可）。

【卦象依据】
深入阐述推导链条：①世爻与用神的生克合冲关系（是用生世、世克用、还是用克世？）；②用神在月令日辰下的旺衰休囚；③动爻发动的作用（是原神生用还是忌神克用？是回头生还是回头克？动化进神还是退神？）；④六神情境象征。

【趋势分析】
阐述事态发展的阶段演变、转折契机与关键隐患；重点给出【应期推断】（如用神何时出空冲空、何时逢冲逢合、事态在何月何日见分晓或迎来转机）。

【建议】
因卦制宜给出 2～3 条具象、有操作性的现实行动策略与避坑抓手（例如：借助长辈/中间人撮合、避开特定日期、主动出击或暂时隐忍等），坚决拒绝空泛的心态鸡汤。
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
            appendLine("请严格依据上述客观卦爻事实与生克依据，拒绝泛泛而谈的心态鸡汤，按照【核心判断】、【卦象依据】、【趋势分析】、【建议】四段式，为我进行断语明确、论据扎实、包含明确应期节点与务实破局动作的深度解卦分析。")
        }
    }
}
