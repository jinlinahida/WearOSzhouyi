package com.boompala.engine.data

import com.boompala.engine.tarot.ArcanaType
import com.boompala.engine.tarot.DeckType
import com.boompala.engine.tarot.TarotElement
import com.boompala.engine.tarot.TarotSuit
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class JsonTarotCardRepositoryTest {

    @Test
    fun `offline asset covers all 78 tarot cards with full validation`() {
        val assetPath = System.getProperty("tarotCardAssetPath")
        assertNotNull("System property tarotCardAssetPath should be set", assetPath)
        val asset = File(requireNotNull(assetPath))
        assertTrue("Missing asset at ${asset.absolutePath}", asset.isFile)

        val repository = asset.bufferedReader().use(JsonTarotCardRepository::fromReader)

        val allCards = repository.allCards()
        assertEquals(78, allCards.size)
        assertEquals(78, allCards.map { it.id }.distinct().size)
        assertEquals(78, allCards.map { it.code }.distinct().size)

        val majorCards = repository.majorArcana()
        val minorCards = repository.minorArcana()
        assertEquals(22, majorCards.size)
        assertEquals(56, minorCards.size)

        assertEquals(22, repository.cardsForDeckType(DeckType.MAJOR_22).size)
        assertEquals(56, repository.cardsForDeckType(DeckType.MINOR_56).size)
        assertEquals(78, repository.cardsForDeckType(DeckType.FULL_78).size)

        // Check Major Arcana properties
        val fool = requireNotNull(repository.cardById(0))
        assertEquals("major_00", fool.code)
        assertEquals("愚者", fool.nameZh)
        assertEquals("The Fool", fool.nameEn)
        assertEquals(ArcanaType.MAJOR, fool.arcana)
        assertEquals(TarotSuit.MAJOR, fool.suit)
        assertEquals(0, fool.rank)
        assertTrue(fool.keywordsZh.isNotEmpty())
        assertTrue(fool.uprightMeanings.isNotEmpty())
        assertTrue(fool.reversedMeanings.isNotEmpty())

        val world = requireNotNull(repository.cardByCode("major_21"))
        assertEquals(21, world.id)
        assertEquals("世界", world.nameZh)
        assertEquals(ArcanaType.MAJOR, world.arcana)

        // Check Minor Arcana suits count
        TarotSuit.values().filter { it != TarotSuit.MAJOR }.forEach { suit ->
            val suitCards = minorCards.filter { it.suit == suit }
            assertEquals("Suit $suit should have 14 cards", 14, suitCards.size)
            val ranks = suitCards.map { it.rank }.sorted()
            assertEquals((1..14).toList(), ranks)
        }

        // Check elemental assignment
        assertEquals(TarotElement.FIRE, requireNotNull(repository.cardByCode("wands_01")).element)
        assertEquals(TarotElement.WATER, requireNotNull(repository.cardByCode("cups_01")).element)
        assertEquals(TarotElement.AIR, requireNotNull(repository.cardByCode("swords_01")).element)
        assertEquals(TarotElement.EARTH, requireNotNull(repository.cardByCode("pentacles_01")).element)

        // Every card must have non-blank content in Chinese and English
        allCards.forEach { card ->
            assertTrue("Card ${card.code} has blank Chinese name", card.nameZh.isNotBlank())
            assertTrue("Card ${card.code} has blank English name", card.nameEn.isNotBlank())
            assertTrue("Card ${card.code} has empty Chinese keywords", card.keywordsZh.isNotEmpty() && card.keywordsZh.all(String::isNotBlank))
            assertTrue("Card ${card.code} has empty Chinese upright meanings", card.uprightMeaningsZh.isNotEmpty() && card.uprightMeaningsZh.all(String::isNotBlank))
            assertTrue("Card ${card.code} has empty Chinese reversed meanings", card.reversedMeaningsZh.isNotEmpty() && card.reversedMeaningsZh.all(String::isNotBlank))
            assertTrue("Card ${card.code} has empty Chinese fortune telling", card.fortuneTellingZh.isNotEmpty() && card.fortuneTellingZh.all(String::isNotBlank))
            assertTrue("Card ${card.code} has empty English upright meanings", card.uprightMeaningsEn.isNotEmpty())
            assertTrue("Card ${card.code} has empty English reversed meanings", card.reversedMeaningsEn.isNotEmpty())
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `repository rejects data that does not have 78 cards`() {
        JsonTarotCardRepository.fromJson(
            """
            {
              "schemaVersion": 1,
              "source": {
                "name": "test",
                "license": "CC0",
                "licenseUrl": "https://example.com",
                "description": "test"
              },
              "cards": []
            }
            """.trimIndent(),
        )
    }
}
