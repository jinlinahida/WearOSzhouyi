package com.boompala.archive

import org.junit.Assert.*
import org.junit.Test

class ArchiveSnapshotCodecTest {
    @Test
    fun roundTripAndCorruptAreSafe() {
        val json = """{"version":1,"source":"XIAO_LIU_REN","title":"大安","sections":{"最终":["大安"]}}"""
        val snapshot = ArchiveSnapshotCodec.decode(json).getOrThrow()
        assertEquals(ArchiveSource.XIAO_LIU_REN, snapshot.source)
        assertEquals("大安", snapshot.sections["最终"]!!.single())
        assertTrue(ArchiveSnapshotCodec.decode("not-json").isFailure)
        assertTrue(ArchiveSnapshotCodec.decode("{\"version\":99}").isFailure)
    }

    @Test
    fun sameSnapshotCanBeUsedForIndependentRecords() {
        val json = """{"version":1,"source":"MEI_HUA","title":"火水未济","sections":{}}"""
        assertEquals(ArchiveSnapshotCodec.decode(json).getOrThrow(), ArchiveSnapshotCodec.decode(json).getOrThrow())
    }

    @Test
    fun legacyDataClassToStringSnapshotIsCleanlySanitized() {
        val legacyJson = """{
            "version": 1,
            "source": "LIU_YAO",
            "title": "乾为天",
            "sections": {
                "本卦": [
                    "乾为天",
                    "111111",
                    "HexagramInterpretation(codeFromBottom=111111, name=乾为天, coreMeaning=刚健中正，自强不息, upperTrigram=TrigramInterpretation(name=乾, image=天, meaning=健), lowerTrigram=TrigramInterpretation(name=乾, image=天, meaning=健), keywords=[刚健, 进取, 恒心], generalTrend=大吉大利, advice=自强不息，戒骄戒躁, relationship=顺遂, career=大有可为, wealth=财源广进)"
                ],
                "解释": ["保存时离线解释已写入本快照"]
            }
        }"""
        val snapshot = ArchiveSnapshotCodec.decode(legacyJson).getOrThrow()
        val originalSection = snapshot.sections["本卦"]
        assertNotNull(originalSection)

        // Ensure raw binary is stripped
        assertFalse(originalSection!!.contains("111111"))
        // Ensure no internal variable names remain
        assertFalse(originalSection.any { it.contains("codeFromBottom=") })
        assertFalse(originalSection.any { it.contains("upperTrigram=") })
        assertFalse(originalSection.any { it.contains("HexagramInterpretation(") })

        // Ensure clean extracted fields are present
        assertTrue(originalSection.any { it == "核心含义：刚健中正，自强不息" })
        assertTrue(originalSection.any { it == "关键词：刚健 · 进取 · 恒心" })
        assertTrue(originalSection.any { it == "处事建议：自强不息，戒骄戒躁" })
        assertTrue(originalSection.any { it == "通用趋势：大吉大利" })

        // Ensure placeholder section "解释" was removed completely
        assertNull(snapshot.sections["解释"])
    }

    @Test
    fun legacyXiaoLiuRenSnapshotIsSanitized() {
        val legacyJson = """{
            "version": 1,
            "source": "XIAO_LIU_REN",
            "title": "大安",
            "sections": {
                "起课": [
                    "月1：大安",
                    "日1：大安",
                    "时1：大安"
                ]
            }
        }"""
        val snapshot = ArchiveSnapshotCodec.decode(legacyJson).getOrThrow()
        val lines = snapshot.sections["起课"]
        assertNotNull(lines)
        assertTrue(lines!!.contains("时辰（第 1 数）：大安"))
    }

    @Test
    fun tarotReadingEncodesAndDecodesSuccessfully() {
        val card = com.boompala.engine.tarot.TarotCard(
            id = 0,
            code = "major_00",
            nameEn = "The Fool",
            nameZh = "愚者",
            arcana = com.boompala.engine.tarot.ArcanaType.MAJOR,
            suit = com.boompala.engine.tarot.TarotSuit.MAJOR,
            rank = 0,
            rankName = "0",
            element = com.boompala.engine.tarot.TarotElement.AIR,
            keywordsEn = listOf("new beginnings"),
            keywordsZh = listOf("新的开始", "纯真"),
            uprightMeanings = listOf("新的旅程开始"),
            reversedMeanings = listOf("盲目冒险"),
            fortuneTelling = listOf("勇敢迈出第一步"),
        )
        val reading = com.boompala.engine.tarot.TarotReading(
            spread = com.boompala.engine.tarot.TarotSpread.ONE_CARD,
            deckType = com.boompala.engine.tarot.DeckType.FULL_78,
            drawnCards = listOf(
                com.boompala.engine.tarot.DrawnTarotCard(
                    slot = com.boompala.engine.tarot.TarotSpread.ONE_CARD.slots.single(),
                    card = card,
                    orientation = com.boompala.engine.tarot.TarotOrientation.UPRIGHT,
                ),
            ),
            castAt = 1700000000000L,
        )
        val json = ArchiveSnapshotCodec.encode(reading)
        val snapshot = ArchiveSnapshotCodec.decode(json).getOrThrow()
        assertEquals(ArchiveSource.TAROT, snapshot.source)
        assertTrue(snapshot.title.contains("单张牌指引"))
        assertTrue(snapshot.title.contains("愚者"))
        assertTrue(snapshot.sections.containsKey("牌阵信息"))
        assertTrue(snapshot.sections.keys.any { it.contains("愚者") })
    }

    @Test
    fun celticCrossReadingEncodesAndDecodesSuccessfully() {
        val spread = com.boompala.engine.tarot.TarotSpread.CELTIC_CROSS
        val cards = (0 until 10).map { i ->
            com.boompala.engine.tarot.TarotCard(
                id = i,
                code = "major_${i.toString().padStart(2, '0')}",
                nameEn = "Card $i",
                nameZh = "牌$i",
                arcana = com.boompala.engine.tarot.ArcanaType.MAJOR,
                suit = com.boompala.engine.tarot.TarotSuit.MAJOR,
                rank = i,
                rankName = "$i",
                element = com.boompala.engine.tarot.TarotElement.AIR,
                keywordsEn = listOf("key$i"),
                keywordsZh = listOf("关键词$i"),
                uprightMeanings = listOf("正位牌义$i"),
                reversedMeanings = listOf("逆位牌义$i"),
                fortuneTelling = listOf("断语$i"),
            )
        }
        val drawn = spread.slots.mapIndexed { idx, slot ->
            com.boompala.engine.tarot.DrawnTarotCard(
                slot = slot,
                card = cards[idx],
                orientation = if (idx % 2 == 0) com.boompala.engine.tarot.TarotOrientation.UPRIGHT else com.boompala.engine.tarot.TarotOrientation.REVERSED,
            )
        }
        val reading = com.boompala.engine.tarot.TarotReading(
            spread = spread,
            deckType = com.boompala.engine.tarot.DeckType.FULL_78,
            drawnCards = drawn,
            castAt = 1700000000000L,
        )
        val json = ArchiveSnapshotCodec.encode(reading)
        val snapshot = ArchiveSnapshotCodec.decode(json).getOrThrow()
        assertEquals(ArchiveSource.TAROT, snapshot.source)
        assertTrue(snapshot.title.contains("凯尔特十字"))
        assertTrue(snapshot.sections.containsKey("牌阵信息"))
        assertEquals(11, snapshot.sections.size) // 1 info section + 10 card sections
    }

    @Test
    fun pulseReadingEncodesAndDecodesSuccessfully() {
        val metrics = com.boompala.engine.pulse.PulseFeatureMetrics(
            heartRateBpm = 75.0,
            regularityPercent = 95.0,
            h1 = 1.0,
            h2 = 0.38,
            h3 = 0.52,
            kValue = 0.35,
            h3Ratio = 0.52,
            h2Ratio = 0.38,
            isRawPpg = true,
        )
        val result = com.boompala.engine.pulse.TcmPulseClassifier.classify(metrics, hour24 = 10)
        val json = ArchiveSnapshotCodec.encode(result)
        val snapshot = ArchiveSnapshotCodec.decode(json).getOrThrow()

        assertEquals(ArchiveSource.PULSE, snapshot.source)
        assertTrue(snapshot.title.contains("脉象推演"))
        assertTrue(snapshot.title.contains(result.category.chineseName))
        assertTrue(snapshot.sections.containsKey("脉象与特征"))
        assertTrue(snapshot.sections.containsKey("宜忌指引"))
        assertTrue(snapshot.sections.containsKey("辨证调理"))
        assertTrue(snapshot.sections.containsKey("时辰经络"))
        assertTrue(snapshot.sections.containsKey("典籍渊源与医理"))
    }

    @Test
    fun liuYaoReadingEncodesAndDecodesWithAiDataOffline() {
        val castAt = java.time.Instant.parse("2026-06-21T00:30:00Z")
        val zoneId = java.time.ZoneId.of("Asia/Shanghai")
        val timeInfo = com.boompala.engine.model.DivinationTimeInfo(
            gregorianDateTime = java.time.ZonedDateTime.of(2026, 6, 21, 8, 30, 0, 0, zoneId),
            lunarDate = "二〇二六年五月初七",
            lunarYearGanzhi = com.boompala.engine.model.Ganzhi(com.boompala.engine.model.HeavenlyStem.BING, com.boompala.engine.model.EarthlyBranch.WU),
            lunarMonth = 5,
            lunarDay = 7,
            yearGanzhi = com.boompala.engine.model.Ganzhi(com.boompala.engine.model.HeavenlyStem.BING, com.boompala.engine.model.EarthlyBranch.WU),
            monthGanzhi = com.boompala.engine.model.Ganzhi(com.boompala.engine.model.HeavenlyStem.JIA, com.boompala.engine.model.EarthlyBranch.WU),
            dayGanzhi = com.boompala.engine.model.Ganzhi(com.boompala.engine.model.HeavenlyStem.BING, com.boompala.engine.model.EarthlyBranch.YIN),
            hourGanzhi = com.boompala.engine.model.Ganzhi(com.boompala.engine.model.HeavenlyStem.REN, com.boompala.engine.model.EarthlyBranch.CHEN),
        )
        val engine = com.boompala.engine.LiuYaoEngine(calendar = { _, _ -> timeInfo })
        val input = com.boompala.engine.model.HexagramInput(
            linesFromBottom = listOf(7, 7, 7, 7, 7, 7).mapIndexed { i, v ->
                com.boompala.engine.model.YaoLineInput(
                    com.boompala.engine.model.YaoPosition.entries[i],
                    com.boompala.engine.model.YaoState.fromNumericValue(v),
                )
            },
            castAt = castAt,
            zoneId = zoneId,
        )
        val result = engine.calculate(input)

        // 1. 无 AI 数据编码：保持向后兼容，不生成 AI 解卦区段
        val jsonWithoutAi = ArchiveSnapshotCodec.encode(result)
        val snapshotWithoutAi = ArchiveSnapshotCodec.decode(jsonWithoutAi).getOrThrow()
        assertEquals(ArchiveSource.LIU_YAO, snapshotWithoutAi.source)
        assertFalse(snapshotWithoutAi.sections.containsKey("灵犀 · AI 解卦"))

        // 2. 携带已完成的 AI 解读数据编码
        val aiData = AiArchiveData(
            topic = "事业官运",
            question = "求测升职前景",
            fullText = "【核心判断】\n官星得月令生旺，晋升有望。\n\n【卦象依据】\n世爻持官鬼，临日建拱扶。\n\n【趋势分析】\n秋冬之际转机显现。\n\n【建议】\n稳扎稳打，宜主动沟通。",
        )
        val jsonWithAi = ArchiveSnapshotCodec.encode(result, interpretations = null, aiData = aiData)
        val snapshotWithAi = ArchiveSnapshotCodec.decode(jsonWithAi).getOrThrow()
        assertEquals(ArchiveSource.LIU_YAO, snapshotWithAi.source)
        assertTrue(snapshotWithAi.sections.containsKey("灵犀 · AI 解卦"))

        val aiSection = snapshotWithAi.sections["灵犀 · AI 解卦"]!!
        assertTrue(aiSection.any { it.contains("占问主题：事业官运") })
        assertTrue(aiSection.any { it.contains("占问问题：求测升职前景") })
        assertTrue(aiSection.any { it.contains("【核心判断】") })
        assertTrue(aiSection.any { it.contains("【建议】") })

        // 3. 空白或失败的 AI 数据不会污染归档
        val blankAiData = AiArchiveData(topic = "综合", question = "", fullText = "   ")
        val jsonWithBlank = ArchiveSnapshotCodec.encode(result, interpretations = null, aiData = blankAiData)
        val snapshotWithBlank = ArchiveSnapshotCodec.decode(jsonWithBlank).getOrThrow()
        assertFalse(snapshotWithBlank.sections.containsKey("灵犀 · AI 解卦"))
    }
}
