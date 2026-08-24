package com.boompala.engine.data

import com.boompala.engine.tarot.ArcanaType
import com.boompala.engine.tarot.DeckType
import com.boompala.engine.tarot.TarotCard
import com.boompala.engine.tarot.TarotElement
import com.boompala.engine.tarot.TarotSuit
import com.google.gson.Gson
import java.io.Reader

/**
 * Offline repository for standard 78 Tarot cards.
 */
interface TarotCardRepository {
    fun allCards(): List<TarotCard>
    fun cardById(id: Int): TarotCard?
    fun cardByCode(code: String): TarotCard?
    fun majorArcana(): List<TarotCard>
    fun minorArcana(): List<TarotCard>
    fun cardsForDeckType(deckType: DeckType): List<TarotCard>
}

object EmptyTarotCardRepository : TarotCardRepository {
    override fun allCards(): List<TarotCard> = emptyList()
    override fun cardById(id: Int): TarotCard? = null
    override fun cardByCode(code: String): TarotCard? = null
    override fun majorArcana(): List<TarotCard> = emptyList()
    override fun minorArcana(): List<TarotCard> = emptyList()
    override fun cardsForDeckType(deckType: DeckType): List<TarotCard> = emptyList()
}

/**
 * Validated JSON reader for `tarot_cards.json`.
 */
class JsonTarotCardRepository private constructor(
    private val cardsList: List<TarotCard>,
    private val byId: Map<Int, TarotCard>,
    private val byCode: Map<String, TarotCard>,
) : TarotCardRepository {

    override fun allCards(): List<TarotCard> = cardsList

    override fun cardById(id: Int): TarotCard? = byId[id]

    override fun cardByCode(code: String): TarotCard? = byCode[code]

    override fun majorArcana(): List<TarotCard> = cardsList.filter { it.arcana == ArcanaType.MAJOR }

    override fun minorArcana(): List<TarotCard> = cardsList.filter { it.arcana == ArcanaType.MINOR }

    override fun cardsForDeckType(deckType: DeckType): List<TarotCard> = when (deckType) {
        DeckType.FULL_78 -> cardsList
        DeckType.MAJOR_22 -> majorArcana()
        DeckType.MINOR_56 -> minorArcana()
    }

    companion object {
        const val SCHEMA_VERSION = 1
        const val CARD_COUNT = 78
        const val MAJOR_COUNT = 22
        const val MINOR_COUNT = 56

        fun fromReader(reader: Reader): JsonTarotCardRepository =
            fromJson(reader.readText())

        fun fromJson(json: String): JsonTarotCardRepository {
            val dataset = Gson().fromJson(json, Dataset::class.java)
                ?: error("tarot_cards.json is empty or invalid JSON.")

            require(dataset.schemaVersion == SCHEMA_VERSION) {
                "Unsupported tarot schema version: ${dataset.schemaVersion}."
            }
            require(
                dataset.source.name.isNotBlank() &&
                    dataset.source.license.isNotBlank() &&
                    dataset.source.licenseUrl.isNotBlank() &&
                    dataset.source.description.isNotBlank(),
            ) {
                "Tarot dataset must declare its source, license, and description."
            }
            require(dataset.cards.size == CARD_COUNT) {
                "Expected $CARD_COUNT tarot cards, found ${dataset.cards.size}."
            }

            val parsedCards = dataset.cards.map { entry ->
                require(entry.id in 0 until CARD_COUNT) {
                    "Invalid card id: ${entry.id}."
                }
                require(entry.code.isNotBlank()) {
                    "Card id ${entry.id} is missing code."
                }
                require(entry.nameEn.isNotBlank() && entry.nameZh.isNotBlank()) {
                    "Card ${entry.code} is missing name."
                }
                val arcana = try {
                    ArcanaType.valueOf(entry.arcana)
                } catch (e: Exception) {
                    error("Invalid arcana ${entry.arcana} on card ${entry.code}")
                }
                val suit = try {
                    TarotSuit.valueOf(entry.suit)
                } catch (e: Exception) {
                    error("Invalid suit ${entry.suit} on card ${entry.code}")
                }
                val element = try {
                    TarotElement.valueOf(entry.element)
                } catch (e: Exception) {
                    error("Invalid element ${entry.element} on card ${entry.code}")
                }
                val uprightZh = entry.uprightMeaningsZh?.takeIf { it.isNotEmpty() } ?: entry.uprightMeanings
                val reversedZh = entry.reversedMeaningsZh?.takeIf { it.isNotEmpty() } ?: entry.reversedMeanings
                val fortuneZh = entry.fortuneTellingZh?.takeIf { it.isNotEmpty() } ?: entry.fortuneTelling
                val uprightEn = entry.uprightMeaningsEn ?: emptyList()
                val reversedEn = entry.reversedMeaningsEn ?: emptyList()
                val fortuneEn = entry.fortuneTellingEn ?: emptyList()

                require(entry.keywordsZh.isNotEmpty()) {
                    "Card ${entry.code} has empty Chinese keywords."
                }
                require(uprightZh.isNotEmpty()) {
                    "Card ${entry.code} has empty upright meanings."
                }
                require(reversedZh.isNotEmpty()) {
                    "Card ${entry.code} has empty reversed meanings."
                }

                TarotCard(
                    id = entry.id,
                    code = entry.code,
                    nameEn = entry.nameEn,
                    nameZh = entry.nameZh,
                    arcana = arcana,
                    suit = suit,
                    rank = entry.rank,
                    rankName = entry.rankName,
                    element = element,
                    keywordsEn = entry.keywordsEn,
                    keywordsZh = entry.keywordsZh,
                    uprightMeanings = uprightZh,
                    reversedMeanings = reversedZh,
                    fortuneTelling = fortuneZh,
                    uprightMeaningsZh = uprightZh,
                    reversedMeaningsZh = reversedZh,
                    fortuneTellingZh = fortuneZh,
                    uprightMeaningsEn = uprightEn,
                    reversedMeaningsEn = reversedEn,
                    fortuneTellingEn = fortuneEn,
                )
            }

            val byIdMap = buildMap {
                parsedCards.forEach { card ->
                    require(put(card.id, card) == null) {
                        "Duplicate card id: ${card.id}."
                    }
                }
            }
            require(byIdMap.size == CARD_COUNT) {
                "Tarot dataset must have exactly $CARD_COUNT unique card IDs."
            }

            val byCodeMap = buildMap {
                parsedCards.forEach { card ->
                    require(put(card.code, card) == null) {
                        "Duplicate card code: ${card.code}."
                    }
                }
            }
            require(byCodeMap.size == CARD_COUNT) {
                "Tarot dataset must have exactly $CARD_COUNT unique card codes."
            }

            val majorCount = parsedCards.count { it.arcana == ArcanaType.MAJOR }
            val minorCount = parsedCards.count { it.arcana == ArcanaType.MINOR }
            require(majorCount == MAJOR_COUNT) {
                "Expected $MAJOR_COUNT major arcana cards, found $majorCount."
            }
            require(minorCount == MINOR_COUNT) {
                "Expected $MINOR_COUNT minor arcana cards, found $minorCount."
            }

            return JsonTarotCardRepository(
                cardsList = parsedCards,
                byId = byIdMap,
                byCode = byCodeMap,
            )
        }
    }

    private data class Dataset(
        val schemaVersion: Int,
        val source: Source,
        val cards: List<CardEntry>,
    )

    private data class Source(
        val name: String,
        val license: String,
        val licenseUrl: String,
        val description: String,
    )

    private data class CardEntry(
        val id: Int,
        val code: String,
        val nameEn: String,
        val nameZh: String,
        val arcana: String,
        val suit: String,
        val rank: Int,
        val rankName: String,
        val element: String,
        val keywordsEn: List<String>,
        val keywordsZh: List<String>,
        val uprightMeanings: List<String>,
        val reversedMeanings: List<String>,
        val fortuneTelling: List<String>,
        val uprightMeaningsZh: List<String>? = null,
        val reversedMeaningsZh: List<String>? = null,
        val fortuneTellingZh: List<String>? = null,
        val uprightMeaningsEn: List<String>? = null,
        val reversedMeaningsEn: List<String>? = null,
        val fortuneTellingEn: List<String>? = null,
    )
}
