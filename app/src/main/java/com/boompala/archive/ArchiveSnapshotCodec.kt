package com.boompala.archive

import com.boompala.engine.data.HexagramInterpretation
import com.boompala.engine.data.HexagramInterpretationRepository
import com.boompala.engine.meihua.MeiHuaTimeReading
import com.boompala.engine.model.DivinationResult
import com.boompala.engine.tarot.TarotReading
import com.boompala.engine.tarot.TarotOrientation
import com.boompala.engine.xiaoliuren.XiaoLiuRenReading
import com.google.gson.Gson
import java.time.format.DateTimeFormatter
import java.util.Locale

object ArchiveSnapshotCodec {
    private val gson = Gson()
    private val timeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.getDefault())

    fun encode(result: DivinationResult): String =
        encode(result, null)

    fun encode(result: DivinationResult, interpretations: HexagramInterpretationRepository?): String {
        val castTime = runCatching { timeFormatter.format(result.timeInfo.gregorianDateTime) }
            .getOrDefault(result.timeInfo.gregorianDateTime.toString())
        val originalInterp = interpretations?.interpretationFor(result.original.pattern.codeFromBottom)
        val changed = result.changed
        val changedInterp = changed?.let { interpretations?.interpretationFor(it.pattern.codeFromBottom) }

        val sections = buildMap {
            put(
                "时间与四柱",
                listOf(
                    "公历：$castTime",
                    "农历：${result.timeInfo.lunarDate}",
                    "四柱：${result.timeInfo.yearGanzhi.displayName}年 ${result.timeInfo.monthGanzhi.displayName}月 ${result.timeInfo.dayGanzhi.displayName}日 ${result.timeInfo.hourGanzhi.displayName}时",
                ),
            )
            put(
                "本卦",
                buildList {
                    add("卦名：${result.original.name}")
                    add("卦宫：${result.original.palace.displayName} · ${result.original.element.displayName}（世爻${result.original.shiPosition.displayName} / 应爻${result.original.yingPosition.displayName}）")
                    if (originalInterp != null) {
                        add("核心含义：${originalInterp.coreMeaning}")
                        add("关键词：${originalInterp.keywords.joinToString(" · ")}")
                        add("处事建议：${originalInterp.advice}")
                    }
                },
            )
            if (changed != null) {
                put(
                    "变卦",
                    buildList {
                        add("卦名：${changed.name}")
                        add("卦宫：${changed.palace.displayName} · ${changed.element.displayName}（世爻${changed.shiPosition.displayName} / 应爻${changed.yingPosition.displayName}）")
                        if (changedInterp != null) {
                            add("核心含义：${changedInterp.coreMeaning}")
                            add("关键词：${changedInterp.keywords.joinToString(" · ")}")
                            add("处事建议：${changedInterp.advice}")
                        }
                    },
                )
            }
            put(
                "动爻与旬空",
                listOf(
                    "动爻：${if (result.changingPositions.isEmpty()) "无动爻" else result.changingPositions.joinToString("、") { it.displayName }}",
                    "旬空：${result.voidBranches.joinToString("") { it.displayName }}",
                ),
            )
            put(
                "本卦装卦",
                result.yaoFromBottom.sortedByDescending { it.position.indexFromBottom }.map { yao ->
                    buildString {
                        append("${yao.position.displayName} · ${yao.yinYang.displayName}")
                        if (yao.moving) append(" (动)")
                        append(" · ${yao.heavenlyStem.displayName}${yao.earthlyBranch.displayName}")
                        append(" · ${yao.sixRelation.displayName}")
                        append(" · ${yao.sixSpirit.displayName}")
                        if (yao.isVoid) append(" [空亡]")
                        if (yao.isShi) append(" [世]") else if (yao.isYing) append(" [应]")
                        if (!yao.lineText.isNullOrBlank()) append(" · ${yao.lineText}")
                    }
                },
            )
            if (changed != null) {
                put(
                    "变卦装卦",
                    changed.yaoFromBottom.sortedByDescending { it.position.indexFromBottom }.map { yao ->
                        buildString {
                            append("${yao.position.displayName} · ${yao.yinYang.displayName}")
                            append(" · ${yao.heavenlyStem.displayName}${yao.earthlyBranch.displayName}")
                            append(" · ${yao.sixRelation.displayName}")
                            append(" · ${yao.sixSpirit.displayName}")
                            if (yao.isVoid) append(" [空亡]")
                            if (yao.isShi) append(" [世]") else if (yao.isYing) append(" [应]")
                        }
                    },
                )
            }
        }

        return gson.toJson(
            ArchiveSnapshot(
                version = 1,
                source = ArchiveSource.LIU_YAO,
                title = result.original.name + (changed?.let { " 之 ${it.name}" } ?: ""),
                sections = sections,
            ),
        )
    }

    fun encode(result: MeiHuaTimeReading): String =
        encode(result, null)

    fun encode(result: MeiHuaTimeReading, interpretations: HexagramInterpretationRepository?): String {
        val castTime = runCatching { timeFormatter.format(result.timeInfo.gregorianDateTime) }
            .getOrDefault(result.timeInfo.gregorianDateTime.toString())
        val origInterp = interpretations?.interpretationFor(result.original.codeFromBottom)
        val mutualInterp = interpretations?.interpretationFor(result.mutual.codeFromBottom)
        val changedInterp = interpretations?.interpretationFor(result.changed.codeFromBottom)

        val sections = buildMap {
            put(
                "时间与四柱",
                listOf(
                    "公历：$castTime",
                    "农历：${result.timeInfo.lunarDate}",
                    "四柱：${result.timeInfo.yearGanzhi.displayName} ${result.timeInfo.monthGanzhi.displayName} ${result.timeInfo.dayGanzhi.displayName} ${result.timeInfo.hourGanzhi.displayName}",
                ),
            )
            put(
                "体用与动爻",
                listOf(
                    "体卦：${result.bodyTrigram.displayName} · 用卦：${result.useTrigram.displayName}",
                    "动爻：${result.movingPosition.displayName}",
                    "起卦数字：年${result.numbers.yearBranch} 月${result.numbers.lunarMonth} 日${result.numbers.lunarDay} 时${result.numbers.hourBranch}",
                ),
            )
            put(
                "本卦",
                buildList {
                    add("卦名：${result.original.name}")
                    if (origInterp != null) {
                        add("核心含义：${origInterp.coreMeaning}")
                        add("关键词：${origInterp.keywords.joinToString(" · ")}")
                        add("处事建议：${origInterp.advice}")
                    }
                },
            )
            put(
                "互卦",
                buildList {
                    add("卦名：${result.mutual.name}")
                    if (mutualInterp != null) {
                        add("核心含义：${mutualInterp.coreMeaning}")
                        add("关键词：${mutualInterp.keywords.joinToString(" · ")}")
                    }
                },
            )
            put(
                "变卦",
                buildList {
                    add("卦名：${result.changed.name}")
                    if (changedInterp != null) {
                        add("核心含义：${changedInterp.coreMeaning}")
                        add("关键词：${changedInterp.keywords.joinToString(" · ")}")
                        add("处事建议：${changedInterp.advice}")
                    }
                },
            )
        }

        return gson.toJson(
            ArchiveSnapshot(
                version = 1,
                source = ArchiveSource.MEI_HUA,
                title = "${result.original.name} 之 ${result.changed.name}",
                sections = sections,
            ),
        )
    }

    fun encode(result: XiaoLiuRenReading): String {
        val castTime = runCatching { timeFormatter.format(result.timeInfo.gregorianDateTime) }
            .getOrDefault(result.timeInfo.gregorianDateTime.toString())

        val sections = mapOf(
            "时间与起课" to listOf(
                "公历：$castTime",
                "农历：${result.timeInfo.lunarDate}",
                "起课月份（${result.timeInfo.lunarMonth}月）：${result.monthPalace.displayName}",
                "起课日期（${result.timeInfo.lunarDay}日）：${result.dayPalace.displayName}",
                "起课时辰（${result.timeInfo.hourGanzhi.earthlyBranch.displayName}时 / ${result.timeInfo.hourGanzhi.earthlyBranch.index + 1}数）：${result.hourPalace.displayName}",
            ),
            "推演过程" to listOf(
                "六宫循环：${result.palaceCycle.joinToString(" → ") { it.displayName }}",
            ),
            "断课结论" to listOf(
                "最终宫位：${result.finalPalace.displayName}",
                "断辞释义：${result.finalPalace.meaning}",
            ),
        )

        return gson.toJson(
            ArchiveSnapshot(
                version = 1,
                source = ArchiveSource.XIAO_LIU_REN,
                title = "小六壬 · ${result.finalPalace.displayName}",
                sections = sections,
            ),
        )
    }

    fun encode(result: TarotReading): String = gson.toJson(
        ArchiveSnapshot(
            1,
            ArchiveSource.TAROT,
            "${result.spread.name} · ${result.drawnCards.joinToString(" ") { it.card.nameZh }}",
            buildMap {
                put(
                    "牌阵信息",
                    listOf(
                        "牌阵：${result.spread.name}",
                        "牌组：${result.deckType.displayName}",
                        "说明：${result.spread.description}",
                    ),
                )
                result.drawnCards.forEach { drawn ->
                    val card = drawn.card
                    val isReversed = drawn.orientation == TarotOrientation.REVERSED
                    val meanings = if (isReversed) card.reversedMeaningsZh else card.uprightMeaningsZh
                    val orientationName = if (isReversed) "逆位 (Reversed)" else "正位 (Upright)"
                    put(
                        "【${drawn.slot.name}】${card.nameZh} (${card.nameEn})",
                        listOf(
                            "朝向：$orientationName",
                            "牌位：${drawn.slot.description}",
                            "属性：${card.arcana.displayName} · ${card.element.displayName}",
                            "关键词：${card.keywordsZh.joinToString(" · ")}",
                            "核心牌义：${meanings.joinToString(" / ")}",
                            "占卜指引：${card.fortuneTellingZh.joinToString(" / ")}",
                        ),
                    )
                }
            },
        ),
    )

    fun encode(result: com.boompala.engine.pulse.PulseDiagnosisResult): String = gson.toJson(
        ArchiveSnapshot(
            version = 1,
            source = ArchiveSource.PULSE,
            title = "脉象推演 · 【${result.category.chineseName}】",
            sections = buildMap {
                put(
                    "脉象与特征",
                    listOf(
                        "脉象名称：${result.category.chineseName}",
                        "属性：${result.category.natureSummary}",
                        "特征描述：${result.profile.featureDescription}",
                        "四字脉诀：${result.category.classicPhrase}",
                    ),
                )
                put(
                    "宜忌指引",
                    listOf(
                        "【宜】：${result.profile.dosList.joinToString(" · ")}",
                        "【忌】：${result.profile.dontsList.joinToString(" · ")}",
                    ),
                )
                put(
                    "辨证调理",
                    buildList {
                        result.profile.syndromes.forEach { syn ->
                            add("【${syn.title}】")
                            add("表现：${syn.symptoms}")
                            add("食疗：${syn.dietaryRecommendations}")
                        }
                    },
                )
                put(
                    "调摄建议",
                    listOf(
                        "情绪调护：${result.profile.emotionalAdvice}",
                        "生活起居：${result.profile.lifestyleAdvice}",
                        "运动调养：${result.profile.exerciseAdvice}",
                    ),
                )
                put(
                    "时辰经络",
                    listOf(
                        "时辰：${result.meridianInfo.earthlyBranch} (${result.meridianInfo.timeRangeText})",
                        "当令：${result.meridianInfo.meridianName} (${result.meridianInfo.physiologicalRole})",
                        "指引：${result.meridianInfo.healthGuidance}",
                    ),
                )
                put(
                    "典籍渊源与医理",
                    listOf(
                        "典籍：${result.profile.classicLiterature}",
                        "医理：${result.profile.theoreticalReason}",
                    ),
                )
            },
        ),
    )

    fun decode(json: String): Result<ArchiveSnapshot> = runCatching {
        val raw = gson.fromJson(json, ArchiveSnapshot::class.java) ?: error("empty snapshot")
        if (raw.version != 1) error("unknown snapshot version ${raw.version}")
        sanitizeSnapshot(raw)
    }

    private fun sanitizeSnapshot(raw: ArchiveSnapshot): ArchiveSnapshot {
        val sanitizedSections = LinkedHashMap<String, List<String>>()
        raw.sections.forEach { (sectionTitle, lines) ->
            val cleanedLines = mutableListOf<String>()
            for (line in lines) {
                val cleaned = sanitizeLine(line)
                if (cleaned.isNotEmpty()) {
                    cleanedLines.addAll(cleaned)
                }
            }
            if (cleanedLines.isNotEmpty()) {
                sanitizedSections[sectionTitle] = cleanedLines
            }
        }
        return raw.copy(sections = sanitizedSections)
    }

    private fun sanitizeLine(line: String): List<String> {
        val trimmed = line.trim()
        if (trimmed.isEmpty()) return emptyList()

        // 1. Omit raw debug placeholders
        if (trimmed.contains("保存时离线解释已写入本快照") ||
            trimmed.contains("保存时的离线六爻解释快照") ||
            trimmed.contains("保存时的离线梅花易数解释快照")
        ) {
            return emptyList()
        }

        // 2. Omit raw 6-digit binary code lines like "111111" or "010101"
        if (trimmed.matches(Regex("^[01]{6}$"))) {
            return emptyList()
        }

        // 3. Parse Kotlin data class toString() representation of HexagramInterpretation
        if (trimmed.startsWith("HexagramInterpretation(") && trimmed.endsWith(")")) {
            return parseHexagramInterpretationString(trimmed)
        }

        // 4. Clean old Xiao Liu Ren format "时1：大安" -> "时辰（第 1 数）：大安"
        val hourMatch = Regex("^时(\\d+)：(.*)$").find(trimmed)
        if (hourMatch != null) {
            val (num, palace) = hourMatch.destructured
            return listOf("时辰（第 $num 数）：$palace")
        }

        return listOf(trimmed)
    }

    private fun parseHexagramInterpretationString(raw: String): List<String> {
        val content = raw.removeSurrounding("HexagramInterpretation(", ")")
        val lines = mutableListOf<String>()

        fun extractField(fieldName: String): String? {
            val pattern = Regex("""\b$fieldName=([^,()]+(?:(?:\([^)]*\))?[^,()]*)*)(?:,|$)""")
            return pattern.find(content)?.groupValues?.get(1)?.trim()
        }

        val coreMeaning = extractField("coreMeaning")
        if (!coreMeaning.isNullOrBlank()) {
            lines.add("核心含义：$coreMeaning")
        }

        val keywordsMatch = Regex("""keywords=\[([^\]]*)\]""").find(content)
        if (keywordsMatch != null) {
            val kw = keywordsMatch.groupValues[1].split(",")
                .map(String::trim)
                .filter(String::isNotBlank)
                .joinToString(" · ")
            if (kw.isNotBlank()) {
                lines.add("关键词：$kw")
            }
        }

        val generalTrend = extractField("generalTrend")
        if (!generalTrend.isNullOrBlank()) {
            lines.add("通用趋势：$generalTrend")
        }

        val advice = extractField("advice")
        if (!advice.isNullOrBlank()) {
            lines.add("处事建议：$advice")
        }

        val relationship = extractField("relationship")
        if (!relationship.isNullOrBlank()) {
            lines.add("感情说明：$relationship")
        }

        val career = extractField("career")
        if (!career.isNullOrBlank()) {
            lines.add("事业说明：$career")
        }

        val wealth = extractField("wealth")
        if (!wealth.isNullOrBlank()) {
            lines.add("财运说明：$wealth")
        }

        return if (lines.isEmpty()) listOf(raw) else lines
    }
}
