package com.boompala.engine.tarot

import com.boompala.engine.data.TarotCardRepository
import kotlin.random.Random

/**
 * Pure domain engine for Tarot shuffling, card drawing, orientation, and spread layout.
 */
class TarotEngine(
    private val repository: TarotCardRepository,
) {
    /**
     * Casts a Tarot spread using the specified deck type and randomness source.
     */
    fun cast(
        spread: TarotSpread,
        deckType: DeckType = DeckType.FULL_78,
        random: Random = Random.Default,
        allowReversed: Boolean = true,
        castAt: Long = System.currentTimeMillis(),
    ): TarotReading {
        val availableCards = repository.cardsForDeckType(deckType)
        require(availableCards.size >= spread.cardCount) {
            "Deck ${deckType.displayName} has only ${availableCards.size} cards, but spread ${spread.name} requires ${spread.cardCount} cards."
        }

        val shuffledCards = availableCards.shuffled(random)
        val drawnList = spread.slots.mapIndexed { index, slot ->
            val card = shuffledCards[index]
            val orientation = if (allowReversed && random.nextBoolean()) {
                TarotOrientation.REVERSED
            } else {
                TarotOrientation.UPRIGHT
            }
            DrawnTarotCard(
                slot = slot,
                card = card,
                orientation = orientation,
            )
        }

        return TarotReading(
            spread = spread,
            deckType = deckType,
            drawnCards = drawnList,
            castAt = castAt,
        )
    }

    /**
     * Constructs a deterministic reading from explicit card IDs and orientations.
     * Useful for testing, archive replay, and fixed readings.
     */
    fun castDeterministic(
        spread: TarotSpread,
        cardIds: List<Int>,
        orientations: List<TarotOrientation>,
        deckType: DeckType = DeckType.FULL_78,
        castAt: Long = System.currentTimeMillis(),
    ): TarotReading {
        require(cardIds.size == spread.cardCount) {
            "Expected ${spread.cardCount} card IDs for spread ${spread.name}, but got ${cardIds.size}."
        }
        require(orientations.size == spread.cardCount) {
            "Expected ${spread.cardCount} orientations for spread ${spread.name}, but got ${orientations.size}."
        }
        require(cardIds.distinct().size == cardIds.size) {
            "Duplicate card IDs provided in draw: $cardIds."
        }

        val drawnList = spread.slots.mapIndexed { index, slot ->
            val id = cardIds[index]
            val card = requireNotNull(repository.cardById(id)) {
                "Card ID $id not found in repository."
            }
            DrawnTarotCard(
                slot = slot,
                card = card,
                orientation = orientations[index],
            )
        }

        return TarotReading(
            spread = spread,
            deckType = deckType,
            drawnCards = drawnList,
            castAt = castAt,
        )
    }
}
