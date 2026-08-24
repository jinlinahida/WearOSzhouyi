package com.boompala.engine.dailyfortune

import com.boompala.engine.model.FiveElement

/**
 * Five-element generation/overcoming tables and the derived lucky lookups.
 *
 * The tables are intentionally local to this feature so the Liu Yao Najia
 * rules remain untouched. Generation cycle: 木 -> 火 -> 土 -> 金 -> 水 -> 木.
 * Overcoming cycle: 金克木, 木克土, 土克水, 水克火, 火克金.
 */
internal object DailyFortuneRules {

    private val generatedBy = mapOf(
        FiveElement.WOOD to FiveElement.WATER,
        FiveElement.FIRE to FiveElement.WOOD,
        FiveElement.EARTH to FiveElement.FIRE,
        FiveElement.METAL to FiveElement.EARTH,
        FiveElement.WATER to FiveElement.METAL,
    )

    private val overcomeBy = mapOf(
        FiveElement.WOOD to FiveElement.METAL,
        FiveElement.FIRE to FiveElement.WATER,
        FiveElement.EARTH to FiveElement.WOOD,
        FiveElement.METAL to FiveElement.FIRE,
        FiveElement.WATER to FiveElement.EARTH,
    )

    private val colorByElement = FortuneColor.entries.associateBy { it.element }

    /** Hetu generative numbers per element: 水1/6、火2/7、木3/8、金4/9、土5/10. */
    private val hetuNumbersByElement = mapOf(
        FiveElement.WATER to listOf(1, 6),
        FiveElement.FIRE to listOf(2, 7),
        FiveElement.WOOD to listOf(3, 8),
        FiveElement.METAL to listOf(4, 9),
        FiveElement.EARTH to listOf(5, 10),
    )

    /** The element that generates [element] (生我者). */
    fun elementGenerating(element: FiveElement): FiveElement = generatedBy.getValue(element)

    /** The element that overcomes [element] (克我者). */
    fun elementOvercoming(element: FiveElement): FiveElement = overcomeBy.getValue(element)

    fun colorFor(element: FiveElement): FortuneColor = colorByElement.getValue(element)

    /** Lucky color: the color of the element that generates the day stem element. */
    fun luckyColor(dayStemElement: FiveElement): FortuneColor =
        colorFor(elementGenerating(dayStemElement))

    /** Support color: the color of the day stem element itself. */
    fun supportColor(dayStemElement: FiveElement): FortuneColor = colorFor(dayStemElement)

    /** Avoid color: the color of the element that overcomes the day stem element. */
    fun avoidColor(dayStemElement: FiveElement): FortuneColor =
        colorFor(elementOvercoming(dayStemElement))

    /** Lucky numbers: Hetu generative numbers of the element that generates the day stem element. */
    fun luckyNumbers(dayStemElement: FiveElement): List<Int> =
        hetuNumbersByElement.getValue(elementGenerating(dayStemElement))
}
