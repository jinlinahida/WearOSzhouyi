package com.boompala.engine.tarot

import com.boompala.engine.data.JsonTarotCardRepository
import java.io.File
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TarotEngineTest {

    private lateinit var repository: JsonTarotCardRepository
    private lateinit var engine: TarotEngine

    @Before
    fun setUp() {
        val assetPath = System.getProperty("tarotCardAssetPath")
        assertNotNull("System property tarotCardAssetPath should be set", assetPath)
        val asset = File(requireNotNull(assetPath))
        repository = asset.bufferedReader().use(JsonTarotCardRepository::fromReader)
        engine = TarotEngine(repository)
    }

    @Test
    fun `cast all standard spreads successfully without card duplication`() {
        val random = Random(42)

        TarotSpread.ALL_SPREADS.forEach { spread ->
            val reading = engine.cast(
                spread = spread,
                deckType = DeckType.FULL_78,
                random = random,
                allowReversed = true,
                castAt = 1000L,
            )

            assertEquals(spread, reading.spread)
            assertEquals(spread.cardCount, reading.drawnCards.size)
            assertEquals(1000L, reading.castAt)

            // Ensure no duplicate cards in the spread
            val cardIds = reading.drawnCards.map { it.card.id }
            assertEquals(spread.cardCount, cardIds.distinct().size)

            // Ensure slot mapping aligns with spread definition
            reading.drawnCards.forEachIndexed { index, drawnCard ->
                assertEquals(spread.slots[index], drawnCard.slot)
                assertTrue(drawnCard.card.nameZh.isNotBlank())
            }
        }
    }

    @Test
    fun `cast with major arcana only restricts cards to major arcana`() {
        val reading = engine.cast(
            spread = TarotSpread.TIME_FLOW,
            deckType = DeckType.MAJOR_22,
            random = Random(123),
        )

        assertEquals(3, reading.drawnCards.size)
        reading.drawnCards.forEach { drawn ->
            assertEquals(ArcanaType.MAJOR, drawn.card.arcana)
            assertTrue(drawn.card.id in 0..21)
        }
    }

    @Test
    fun `cast with allowReversed false yields only upright cards`() {
        val reading = engine.cast(
            spread = TarotSpread.CELTIC_CROSS,
            deckType = DeckType.FULL_78,
            random = Random(999),
            allowReversed = false,
        )

        assertEquals(10, reading.drawnCards.size)
        reading.drawnCards.forEach { drawn ->
            assertEquals(TarotOrientation.UPRIGHT, drawn.orientation)
        }
    }

    @Test
    fun `deterministic cast correctly maps cards and orientations`() {
        val cardIds = listOf(0, 1, 2)
        val orientations = listOf(
            TarotOrientation.UPRIGHT,
            TarotOrientation.REVERSED,
            TarotOrientation.UPRIGHT,
        )

        val reading = engine.castDeterministic(
            spread = TarotSpread.HOLY_TRIANGLE,
            cardIds = cardIds,
            orientations = orientations,
            castAt = 2000L,
        )

        assertEquals(3, reading.drawnCards.size)
        assertEquals("愚者", reading.drawnCards[0].card.nameZh)
        assertEquals(TarotOrientation.UPRIGHT, reading.drawnCards[0].orientation)
        assertEquals("魔术师", reading.drawnCards[1].card.nameZh)
        assertEquals(TarotOrientation.REVERSED, reading.drawnCards[1].orientation)
        assertEquals("女祭司", reading.drawnCards[2].card.nameZh)
        assertEquals(TarotOrientation.UPRIGHT, reading.drawnCards[2].orientation)
        assertEquals(2000L, reading.castAt)
    }

    @Test
    fun `cast with minor arcana only restricts cards to minor arcana`() {
        val reading = engine.cast(
            spread = TarotSpread.FOUR_ELEMENTS,
            deckType = DeckType.MINOR_56,
            random = Random(456),
        )

        assertEquals(4, reading.drawnCards.size)
        reading.drawnCards.forEach { drawn ->
            assertEquals(ArcanaType.MINOR, drawn.card.arcana)
            assertTrue(drawn.card.id in 22..77)
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `cast throws when spread requires more cards than deck has`() {
        // Celtic cross requires 10 cards, let's test a case where spread requires more cards than deck
        val hugeSpread = TarotSpread(
            id = "huge_spread",
            name = "超大牌阵",
            description = "测试牌阵",
            cardCount = 25,
            slots = (0 until 25).map { TarotSlot(it, "Slot $it", "Desc $it") },
        )
        // MAJOR_22 only has 22 cards
        engine.cast(
            spread = hugeSpread,
            deckType = DeckType.MAJOR_22,
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `castDeterministic rejects duplicate card ids`() {
        engine.castDeterministic(
            spread = TarotSpread.HOLY_TRIANGLE,
            cardIds = listOf(0, 0, 1),
            orientations = listOf(TarotOrientation.UPRIGHT, TarotOrientation.UPRIGHT, TarotOrientation.UPRIGHT),
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `castDeterministic throws on non existent card id`() {
        engine.castDeterministic(
            spread = TarotSpread.ONE_CARD,
            cardIds = listOf(999),
            orientations = listOf(TarotOrientation.UPRIGHT),
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `castDeterministic throws on cardIds size mismatch`() {
        engine.castDeterministic(
            spread = TarotSpread.TIME_FLOW,
            cardIds = listOf(0, 1),
            orientations = listOf(TarotOrientation.UPRIGHT, TarotOrientation.UPRIGHT),
        )
    }

    @Test
    fun `time flow spread correctly produces three distinct cards for past present future`() {
        val reading = engine.cast(
            spread = TarotSpread.TIME_FLOW,
            deckType = DeckType.FULL_78,
            random = Random(789),
            allowReversed = true,
            castAt = 3000L,
        )

        assertEquals(3, reading.drawnCards.size)
        assertEquals("过去", reading.drawnCards[0].slot.name)
        assertEquals("现在", reading.drawnCards[1].slot.name)
        assertEquals("未来", reading.drawnCards[2].slot.name)

        val ids = reading.drawnCards.map { it.card.id }
        assertEquals(3, ids.distinct().size)

        // Test deterministic cast for TIME_FLOW
        val deterministic = engine.castDeterministic(
            spread = TarotSpread.TIME_FLOW,
            cardIds = listOf(10, 11, 12),
            orientations = listOf(TarotOrientation.UPRIGHT, TarotOrientation.REVERSED, TarotOrientation.UPRIGHT),
            castAt = 3000L,
        )
        assertEquals(3, deterministic.drawnCards.size)
        assertEquals(10, deterministic.drawnCards[0].card.id)
        assertEquals("过去", deterministic.drawnCards[0].slot.name)
        assertEquals(TarotOrientation.UPRIGHT, deterministic.drawnCards[0].orientation)
        assertEquals(11, deterministic.drawnCards[1].card.id)
        assertEquals("现在", deterministic.drawnCards[1].slot.name)
        assertEquals(TarotOrientation.REVERSED, deterministic.drawnCards[1].orientation)
        assertEquals(12, deterministic.drawnCards[2].card.id)
        assertEquals("未来", deterministic.drawnCards[2].slot.name)
        assertEquals(TarotOrientation.UPRIGHT, deterministic.drawnCards[2].orientation)
    }

    @Test
    fun `holy triangle spread correctly produces three distinct cards for situation obstacle solution`() {
        val reading = engine.cast(
            spread = TarotSpread.HOLY_TRIANGLE,
            deckType = DeckType.FULL_78,
            random = Random(321),
            allowReversed = true,
            castAt = 4000L,
        )

        assertEquals(3, reading.drawnCards.size)
        assertEquals("现状", reading.drawnCards[0].slot.name)
        assertEquals("阻碍与根源", reading.drawnCards[1].slot.name)
        assertEquals("对策与建议", reading.drawnCards[2].slot.name)

        val ids = reading.drawnCards.map { it.card.id }
        assertEquals(3, ids.distinct().size)

        // Test with MAJOR_22 only
        val majorOnlyReading = engine.cast(
            spread = TarotSpread.HOLY_TRIANGLE,
            deckType = DeckType.MAJOR_22,
            random = Random(654),
            allowReversed = false,
        )
        assertEquals(3, majorOnlyReading.drawnCards.size)
        majorOnlyReading.drawnCards.forEach { drawn ->
            assertEquals(ArcanaType.MAJOR, drawn.card.arcana)
            assertEquals(TarotOrientation.UPRIGHT, drawn.orientation)
        }

        // Test deterministic cast for HOLY_TRIANGLE
        val deterministic = engine.castDeterministic(
            spread = TarotSpread.HOLY_TRIANGLE,
            cardIds = listOf(0, 1, 2),
            orientations = listOf(TarotOrientation.UPRIGHT, TarotOrientation.REVERSED, TarotOrientation.UPRIGHT),
            castAt = 4000L,
        )
        assertEquals(3, deterministic.drawnCards.size)
        assertEquals(0, deterministic.drawnCards[0].card.id)
        assertEquals("现状", deterministic.drawnCards[0].slot.name)
        assertEquals("愚者", deterministic.drawnCards[0].card.nameZh)
        assertEquals(TarotOrientation.UPRIGHT, deterministic.drawnCards[0].orientation)

        assertEquals(1, deterministic.drawnCards[1].card.id)
        assertEquals("阻碍与根源", deterministic.drawnCards[1].slot.name)
        assertEquals("魔术师", deterministic.drawnCards[1].card.nameZh)
        assertEquals(TarotOrientation.REVERSED, deterministic.drawnCards[1].orientation)

        assertEquals(2, deterministic.drawnCards[2].card.id)
        assertEquals("对策与建议", deterministic.drawnCards[2].slot.name)
        assertEquals("女祭司", deterministic.drawnCards[2].card.nameZh)
        assertEquals(TarotOrientation.UPRIGHT, deterministic.drawnCards[2].orientation)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `castDeterministic throws on orientations size mismatch`() {
        engine.castDeterministic(
            spread = TarotSpread.TIME_FLOW,
            cardIds = listOf(0, 1, 2),
            orientations = listOf(TarotOrientation.UPRIGHT, TarotOrientation.UPRIGHT),
        )
    }
}
