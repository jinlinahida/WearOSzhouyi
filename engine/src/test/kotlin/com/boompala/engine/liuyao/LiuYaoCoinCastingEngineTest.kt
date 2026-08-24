package com.boompala.engine.liuyao

import com.boompala.engine.BasicHexagramEngine
import com.boompala.engine.LiuYaoEngine
import com.boompala.engine.data.LineTextRepository
import com.boompala.engine.model.DivinationTimeInfo
import com.boompala.engine.model.EarthlyBranch
import com.boompala.engine.model.Ganzhi
import com.boompala.engine.model.HeavenlyStem
import com.boompala.engine.model.HexagramInput
import com.boompala.engine.model.YaoLineInput
import com.boompala.engine.model.YaoPolarity
import com.boompala.engine.model.YaoPosition
import com.boompala.engine.model.YaoState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.random.Random

class LiuYaoCoinCastingEngineTest {

    private val testInstant = Instant.parse("2026-08-16T12:00:00Z")
    private val testZone = ZoneId.of("Asia/Shanghai")

    @Test
    fun singleCoinSideValuesAreValid() {
        assertEquals(CoinSide.TAILS, CoinSide.fromValue(2))
        assertEquals(CoinSide.HEADS, CoinSide.fromValue(3))
        assertEquals(2, CoinSide.TAILS.value)
        assertEquals(3, CoinSide.HEADS.value)
    }

    @Test(expected = IllegalArgumentException::class)
    fun singleCoinSideThrowsOnInvalidValue() {
        CoinSide.fromValue(4)
    }

    @Test
    fun threeCoinsSumMappingsToYaoStates() {
        // 2 + 2 + 2 = 6 -> 老阴 (阴爻，动爻)
        val toss6 = LiuYaoCoinCastingEngine.singleLineFromValues(listOf(2, 2, 2))
        assertEquals(6, toss6.sum)
        assertEquals(YaoState.OLD_YIN, toss6.state)
        assertFalse(toss6.state.isYang)
        assertTrue(toss6.state.isChanging)

        // 2 + 2 + 3 = 7 -> 少阳 (阳爻，静爻)
        val toss7 = LiuYaoCoinCastingEngine.singleLineFromValues(listOf(2, 2, 3))
        assertEquals(7, toss7.sum)
        assertEquals(YaoState.YOUNG_YANG, toss7.state)
        assertTrue(toss7.state.isYang)
        assertFalse(toss7.state.isChanging)

        // 2 + 3 + 3 = 8 -> 少阴 (阴爻，静爻)
        val toss8 = LiuYaoCoinCastingEngine.singleLineFromValues(listOf(2, 3, 3))
        assertEquals(8, toss8.sum)
        assertEquals(YaoState.YOUNG_YIN, toss8.state)
        assertFalse(toss8.state.isYang)
        assertFalse(toss8.state.isChanging)

        // 3 + 3 + 3 = 9 -> 老阳 (阳爻，动爻)
        val toss9 = LiuYaoCoinCastingEngine.singleLineFromValues(listOf(3, 3, 3))
        assertEquals(9, toss9.sum)
        assertEquals(YaoState.OLD_YANG, toss9.state)
        assertTrue(toss9.state.isYang)
        assertTrue(toss9.state.isChanging)
    }

    @Test
    fun randomTossFollowsThreeCoinProbabilityDistribution() {
        val iterations = 8000
        val counts = mutableMapOf<Int, Int>()
        val rng = Random(42)

        repeat(iterations) {
            val result = LiuYaoCoinCastingEngine.castSingleLine(rng)
            assertTrue(result.sum in 6..9)
            counts[result.sum] = (counts[result.sum] ?: 0) + 1
        }

        val count6 = counts[6] ?: 0 // expected ~ 1000 (1/8)
        val count7 = counts[7] ?: 0 // expected ~ 3000 (3/8)
        val count8 = counts[8] ?: 0 // expected ~ 3000 (3/8)
        val count9 = counts[9] ?: 0 // expected ~ 1000 (1/8)

        // Verify that 7 and 8 are roughly 3x more frequent than 6 and 9 (not 1/4 uniform)
        assertTrue("Count of 7 ($count7) should be significantly greater than count of 6 ($count6)", count7 > count6 * 2)
        assertTrue("Count of 8 ($count8) should be significantly greater than count of 9 ($count9)", count8 > count9 * 2)
        assertTrue("Count of 6 ($count6) should be around 1000", count6 in 800..1200)
        assertTrue("Count of 7 ($count7) should be around 3000", count7 in 2700..3300)
        assertTrue("Count of 8 ($count8) should be around 3000", count8 in 2700..3300)
        assertTrue("Count of 9 ($count9) should be around 1000", count9 in 800..1200)
    }

    @Test
    fun firstTossStrictlyMapsToFirstYaoAndSixthTossStrictlyMapsToTopYao() {
        // Step 1: 1st toss (初爻) -> 9 (老阳)
        // Step 2: 2nd toss (二爻) -> 7 (少阳)
        // Step 3: 3rd toss (三爻) -> 8 (少阴)
        // Step 4: 4th toss (四爻) -> 7 (少阳)
        // Step 5: 5th toss (五爻) -> 8 (少阴)
        // Step 6: 6th toss (上爻) -> 6 (老阴)
        val records = listOf(
            CoinCastingRecord(YaoPosition.FIRST, LiuYaoCoinCastingEngine.singleLineFromValues(listOf(3, 3, 3))),
            CoinCastingRecord(YaoPosition.SECOND, LiuYaoCoinCastingEngine.singleLineFromValues(listOf(2, 2, 3))),
            CoinCastingRecord(YaoPosition.THIRD, LiuYaoCoinCastingEngine.singleLineFromValues(listOf(2, 3, 3))),
            CoinCastingRecord(YaoPosition.FOURTH, LiuYaoCoinCastingEngine.singleLineFromValues(listOf(2, 2, 3))),
            CoinCastingRecord(YaoPosition.FIFTH, LiuYaoCoinCastingEngine.singleLineFromValues(listOf(2, 3, 3))),
            CoinCastingRecord(YaoPosition.TOP, LiuYaoCoinCastingEngine.singleLineFromValues(listOf(2, 2, 2))),
        )

        val input = LiuYaoCoinCastingEngine.toHexagramInput(records, testInstant, testZone)

        // Verify contract: index 0 must strictly be 初爻 (FIRST), index 5 must strictly be 上爻 (TOP)
        assertEquals(6, input.linesFromBottom.size)
        assertEquals(YaoPosition.FIRST, input.linesFromBottom[0].position)
        assertEquals(YaoState.OLD_YANG, input.linesFromBottom[0].state)

        assertEquals(YaoPosition.SECOND, input.linesFromBottom[1].position)
        assertEquals(YaoState.YOUNG_YANG, input.linesFromBottom[1].state)

        assertEquals(YaoPosition.THIRD, input.linesFromBottom[2].position)
        assertEquals(YaoState.YOUNG_YIN, input.linesFromBottom[2].state)

        assertEquals(YaoPosition.FOURTH, input.linesFromBottom[3].position)
        assertEquals(YaoState.YOUNG_YANG, input.linesFromBottom[3].state)

        assertEquals(YaoPosition.FIFTH, input.linesFromBottom[4].position)
        assertEquals(YaoState.YOUNG_YIN, input.linesFromBottom[4].state)

        assertEquals(YaoPosition.TOP, input.linesFromBottom[5].position)
        assertEquals(YaoState.OLD_YIN, input.linesFromBottom[5].state)

        // Verify that 初爻 is moving yang, 上爻 is moving yin
        val result = BasicHexagramEngine.derive(input)
        assertEquals(listOf(YaoPosition.FIRST, YaoPosition.TOP), result.changingPositions)
        assertEquals(YaoPolarity.YANG, result.original.linesFromBottom[0]) // 初爻: 阳
        assertEquals(YaoPolarity.YIN, result.original.linesFromBottom[5])  // 上爻: 阴

        assertEquals(YaoPolarity.YIN, result.changed!!.linesFromBottom[0])  // 变卦初爻: 阴 (阳变阴)
        assertEquals(YaoPolarity.YANG, result.changed!!.linesFromBottom[5]) // 变卦上爻: 阳 (阴变阳)
    }

    @Test
    fun visualReversalOrdersTopLineFirstAndBottomLineLast() {
        val orderedPositions = YaoPosition.entries // [FIRST, SECOND, THIRD, FOURTH, FIFTH, TOP]
        val visualOrder = orderedPositions.reversed() // [TOP, FIFTH, FOURTH, THIRD, SECOND, FIRST]

        assertEquals(YaoPosition.TOP, visualOrder.first())
        assertEquals(YaoPosition.FIRST, visualOrder.last())

        // Index in data from bottom corresponds to 5 for TOP and 0 for FIRST
        assertEquals(5, visualOrder[0].indexFromBottom)
        assertEquals(4, visualOrder[1].indexFromBottom)
        assertEquals(3, visualOrder[2].indexFromBottom)
        assertEquals(2, visualOrder[3].indexFromBottom)
        assertEquals(1, visualOrder[4].indexFromBottom)
        assertEquals(0, visualOrder[5].indexFromBottom)
    }

    @Test(expected = IllegalArgumentException::class)
    fun toHexagramInputRejectsReversedPositionOrder() {
        // Passing records in top-to-bottom order to toHexagramInput must throw
        val reversedRecords = listOf(
            CoinCastingRecord(YaoPosition.TOP, LiuYaoCoinCastingEngine.singleLineFromValues(listOf(3, 3, 3))),
            CoinCastingRecord(YaoPosition.FIFTH, LiuYaoCoinCastingEngine.singleLineFromValues(listOf(2, 2, 3))),
            CoinCastingRecord(YaoPosition.FOURTH, LiuYaoCoinCastingEngine.singleLineFromValues(listOf(2, 3, 3))),
            CoinCastingRecord(YaoPosition.THIRD, LiuYaoCoinCastingEngine.singleLineFromValues(listOf(2, 2, 3))),
            CoinCastingRecord(YaoPosition.SECOND, LiuYaoCoinCastingEngine.singleLineFromValues(listOf(2, 3, 3))),
            CoinCastingRecord(YaoPosition.FIRST, LiuYaoCoinCastingEngine.singleLineFromValues(listOf(2, 2, 2))),
        )
        LiuYaoCoinCastingEngine.toHexagramInput(reversedRecords, testInstant, testZone)
    }

    @Test
    fun deterministicSixLinesProducesExactHexagramAndChangingLines() {
        // Initial hexagram:
        // 初爻: 7 (少阳 -> 阳静)
        // 二爻: 6 (老阴 -> 阴动 -> 变阳)
        // 三爻: 8 (少阴 -> 阴静)
        // 四爻: 9 (老阳 -> 阳动 -> 变阴)
        // 五爻: 7 (少阳 -> 阳静)
        // 上爻: 8 (少阴 -> 阴静)
        val coinValues = listOf(
            listOf(2, 2, 3), // 7: 初爻
            listOf(2, 2, 2), // 6: 二爻 (动)
            listOf(2, 3, 3), // 8: 三爻
            listOf(3, 3, 3), // 9: 四爻 (动)
            listOf(3, 2, 2), // 7: 五爻
            listOf(3, 3, 2), // 8: 上爻
        )

        val input = LiuYaoCoinCastingEngine.castDeterministic(
            sixLineCoinValues = coinValues,
            castAt = testInstant,
            zoneId = testZone,
        )

        assertEquals(6, input.linesFromBottom.size)
        assertEquals(YaoPosition.FIRST, input.linesFromBottom[0].position)
        assertEquals(YaoState.YOUNG_YANG, input.linesFromBottom[0].state)

        assertEquals(YaoPosition.SECOND, input.linesFromBottom[1].position)
        assertEquals(YaoState.OLD_YIN, input.linesFromBottom[1].state)

        assertEquals(YaoPosition.THIRD, input.linesFromBottom[2].position)
        assertEquals(YaoState.YOUNG_YIN, input.linesFromBottom[2].state)

        assertEquals(YaoPosition.FOURTH, input.linesFromBottom[3].position)
        assertEquals(YaoState.OLD_YANG, input.linesFromBottom[3].state)

        assertEquals(YaoPosition.FIFTH, input.linesFromBottom[4].position)
        assertEquals(YaoState.YOUNG_YANG, input.linesFromBottom[4].state)

        assertEquals(YaoPosition.TOP, input.linesFromBottom[5].position)
        assertEquals(YaoState.YOUNG_YIN, input.linesFromBottom[5].state)

        // Pass to BasicHexagramEngine to verify original and changed hexagrams
        val result = BasicHexagramEngine.derive(input)
        assertTrue(result.hasChangingLines)
        assertEquals(listOf(YaoPosition.SECOND, YaoPosition.FOURTH), result.changingPositions)

        // Original lines (初 to 上): 阳, 阴, 阴, 阳, 阳, 阴
        assertEquals(
            listOf(YaoPolarity.YANG, YaoPolarity.YIN, YaoPolarity.YIN, YaoPolarity.YANG, YaoPolarity.YANG, YaoPolarity.YIN),
            result.original.linesFromBottom,
        )

        // Changed lines (初 to 上): 阳, [阳], 阴, [阴], 阳, 阴
        assertNotNull(result.changed)
        assertEquals(
            listOf(YaoPolarity.YANG, YaoPolarity.YANG, YaoPolarity.YIN, YaoPolarity.YIN, YaoPolarity.YANG, YaoPolarity.YIN),
            result.changed!!.linesFromBottom,
        )
    }

    @Test
    fun staticHexagramWithoutChangingLinesHasNullChanged() {
        // All static lines: 7 and 8
        val coinValues = listOf(
            listOf(2, 2, 3), // 7: 初爻
            listOf(2, 3, 3), // 8: 二爻
            listOf(2, 3, 3), // 8: 三爻
            listOf(3, 2, 2), // 7: 四爻
            listOf(3, 2, 2), // 7: 五爻
            listOf(3, 3, 2), // 8: 上爻
        )

        val input = LiuYaoCoinCastingEngine.castDeterministic(
            sixLineCoinValues = coinValues,
            castAt = testInstant,
            zoneId = testZone,
        )

        val result = BasicHexagramEngine.derive(input)
        assertFalse(result.hasChangingLines)
        assertTrue(result.changingPositions.isEmpty())
        assertNull(result.changed)
    }

    @Test
    fun deterministicCoinsAndManualInputProduceIdenticalDivinationResult() {
        // Specified sequence: [6, 7, 8, 9, 7, 8]
        // 初爻: 6 (老阴) -> [2, 2, 2]
        // 二爻: 7 (少阳) -> [2, 2, 3]
        // 三爻: 8 (少阴) -> [2, 3, 3]
        // 四爻: 9 (老阳) -> [3, 3, 3]
        // 五爻: 7 (少阳) -> [2, 2, 3]
        // 上爻: 8 (少阴) -> [2, 3, 3]
        val coinValues = listOf(
            listOf(2, 2, 2), // 6: 初爻
            listOf(2, 2, 3), // 7: 二爻
            listOf(2, 3, 3), // 8: 三爻
            listOf(3, 3, 3), // 9: 四爻
            listOf(2, 2, 3), // 7: 五爻
            listOf(2, 3, 3), // 8: 上爻
        )

        val coinInput = LiuYaoCoinCastingEngine.castDeterministic(
            sixLineCoinValues = coinValues,
            castAt = testInstant,
            zoneId = testZone,
        )

        val manualInput = HexagramInput(
            linesFromBottom = listOf(
                YaoLineInput(YaoPosition.FIRST, YaoState.OLD_YIN),
                YaoLineInput(YaoPosition.SECOND, YaoState.YOUNG_YANG),
                YaoLineInput(YaoPosition.THIRD, YaoState.YOUNG_YIN),
                YaoLineInput(YaoPosition.FOURTH, YaoState.OLD_YANG),
                YaoLineInput(YaoPosition.FIFTH, YaoState.YOUNG_YANG),
                YaoLineInput(YaoPosition.TOP, YaoState.YOUNG_YIN),
            ),
            castAt = testInstant,
            zoneId = testZone,
        )

        // 1. Verify inputs are structurally identical
        assertEquals(manualInput.linesFromBottom, coinInput.linesFromBottom)
        assertEquals(manualInput.castAt, coinInput.castAt)
        assertEquals(manualInput.zoneId, coinInput.zoneId)

        // 2. Feed both into LiuYaoEngine
        val mockTimeInfo = DivinationTimeInfo(
            gregorianDateTime = ZonedDateTime.ofInstant(testInstant, testZone),
            lunarDate = "丙午年 六月十七 申时",
            lunarYearGanzhi = Ganzhi(HeavenlyStem.BING, EarthlyBranch.WU),
            lunarMonth = 6,
            lunarDay = 17,
            yearGanzhi = Ganzhi(HeavenlyStem.BING, EarthlyBranch.WU),
            monthGanzhi = Ganzhi(HeavenlyStem.YI, EarthlyBranch.WEI),
            dayGanzhi = Ganzhi(HeavenlyStem.JIA, EarthlyBranch.ZI),
            hourGanzhi = Ganzhi(HeavenlyStem.REN, EarthlyBranch.SHEN),
        )

        val engine = LiuYaoEngine(
            calendar = { _, _ -> mockTimeInfo },
            lineTexts = LineTextRepository { code, pos -> "$code:${pos.displayName}" },
        )

        val coinResult = engine.calculate(coinInput)
        val manualResult = engine.calculate(manualInput)

        // 3. Verify absolute equivalence across every field
        assertEquals(manualResult.original.name, coinResult.original.name)
        assertEquals(manualResult.original.palace, coinResult.original.palace)
        assertEquals(manualResult.original.palaceStage, coinResult.original.palaceStage)
        assertEquals(manualResult.original.shiPosition, coinResult.original.shiPosition)
        assertEquals(manualResult.original.yingPosition, coinResult.original.yingPosition)
        assertEquals(manualResult.changed?.name, coinResult.changed?.name)
        assertEquals(manualResult.changingPositions, coinResult.changingPositions)

        // Yao lines matching
        assertEquals(manualResult.yaoFromBottom.size, coinResult.yaoFromBottom.size)
        manualResult.yaoFromBottom.zip(coinResult.yaoFromBottom).forEach { (mYao, cYao) ->
            assertEquals(mYao.position, cYao.position)
            assertEquals(mYao.yinYang, cYao.yinYang)
            assertEquals(mYao.moving, cYao.moving)
            assertEquals(mYao.heavenlyStem, cYao.heavenlyStem)
            assertEquals(mYao.earthlyBranch, cYao.earthlyBranch)
            assertEquals(mYao.element, cYao.element)
            assertEquals(mYao.sixRelation, cYao.sixRelation)
            assertEquals(mYao.sixSpirit, cYao.sixSpirit)
            assertEquals(mYao.isShi, cYao.isShi)
            assertEquals(mYao.isYing, cYao.isYing)
            assertEquals(mYao.isVoid, cYao.isVoid)
            assertEquals(mYao.lineText, cYao.lineText)
        }
    }
}
