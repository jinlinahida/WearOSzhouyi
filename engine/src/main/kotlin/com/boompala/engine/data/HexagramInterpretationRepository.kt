package com.boompala.engine.data

import com.google.gson.Gson
import java.io.Reader

/**
 * Offline, general-purpose reference material for the sixty-four hexagrams.
 *
 * This is deliberately separate from both the calculation rules and line-text
 * source. It is a concise project-authored reference, not an answer to a
 * specific divination question.
 */
data class HexagramInterpretation(
    val codeFromBottom: String,
    val name: String,
    val coreMeaning: String,
    val upperTrigram: TrigramInterpretation,
    val lowerTrigram: TrigramInterpretation,
    val keywords: List<String>,
    val generalTrend: String,
    val advice: String,
    val relationship: String,
    val career: String,
    val wealth: String,
)

data class TrigramInterpretation(
    val name: String,
    val image: String,
    val meaning: String,
)

fun interface HexagramInterpretationRepository {
    fun interpretationFor(codeFromBottom: String): HexagramInterpretation?
}

object EmptyHexagramInterpretationRepository : HexagramInterpretationRepository {
    override fun interpretationFor(codeFromBottom: String): HexagramInterpretation? = null
}

/**
 * Validated reader for the project-authored `hexagram_interpretations.json`.
 *
 * The validation intentionally requires the complete 2^6 set of line codes,
 * rather than merely accepting 64 arbitrary records. This catches duplicated
 * records that would otherwise hide a missing hexagram.
 */
class JsonHexagramInterpretationRepository private constructor(
    private val interpretations: Map<String, HexagramInterpretation>,
) : HexagramInterpretationRepository {

    override fun interpretationFor(codeFromBottom: String): HexagramInterpretation? =
        interpretations[codeFromBottom]

    companion object {
        const val SCHEMA_VERSION = 1
        const val HEXAGRAM_COUNT = 64
        private val expectedCodes = (0 until HEXAGRAM_COUNT).map { value ->
            (5 downTo 0).joinToString(separator = "") { bit ->
                if (value and (1 shl bit) == 0) "0" else "1"
            }
        }.toSet()

        fun fromReader(reader: Reader): JsonHexagramInterpretationRepository =
            fromJson(reader.readText())

        fun fromJson(json: String): JsonHexagramInterpretationRepository {
            val dataset = Gson().fromJson(json, Dataset::class.java)
                ?: error("hexagram_interpretations.json is empty or invalid JSON.")
            require(dataset.schemaVersion == SCHEMA_VERSION) {
                "Unsupported interpretation schema: ${dataset.schemaVersion}."
            }
            require(
                dataset.source.name.isNotBlank() &&
                    dataset.source.license.isNotBlank() &&
                    dataset.source.licenseUrl.isNotBlank(),
            ) {
                "Interpretation data must declare its source and license."
            }
            require(dataset.source.description.isNotBlank()) {
                "Interpretation data must declare how its text was prepared."
            }
            require(dataset.hexagrams.size == HEXAGRAM_COUNT) {
                "Expected $HEXAGRAM_COUNT hexagrams, found ${dataset.hexagrams.size}."
            }
            require(dataset.trigrams.keys == TRIGRAM_IDS) {
                "Interpretation data must define the eight trigrams exactly once."
            }
            dataset.trigrams.forEach { (id, trigram) ->
                require(trigram.name.isNotBlank() && trigram.image.isNotBlank() && trigram.meaning.isNotBlank()) {
                    "Trigram $id is incomplete."
                }
            }

            val byCode = buildMap {
                dataset.hexagrams.forEach { entry ->
                    require(entry.code.matches(Regex("[01]{6}"))) {
                        "Invalid hexagram code: ${entry.code}."
                    }
                    require(entry.name.isNotBlank() && entry.coreMeaning.isNotBlank()) {
                        "Hexagram ${entry.code} is missing its name or core meaning."
                    }
                    require(entry.upperTrigram in TRIGRAM_IDS && entry.lowerTrigram in TRIGRAM_IDS) {
                        "Hexagram ${entry.code} refers to an unknown trigram."
                    }
                    require(entry.keywords.size in 2..5 && entry.keywords.all(String::isNotBlank)) {
                        "Hexagram ${entry.code} must have two to five nonblank keywords."
                    }
                    require(entry.keywords.distinct().size == entry.keywords.size) {
                        "Hexagram ${entry.code} has duplicate keywords."
                    }
                    require(
                        listOf(
                            entry.generalTrend,
                            entry.advice,
                            entry.relationship,
                            entry.career,
                            entry.wealth,
                        ).all(String::isNotBlank),
                    ) {
                        "Hexagram ${entry.code} has incomplete reference text."
                    }

                    val interpretation = HexagramInterpretation(
                        codeFromBottom = entry.code,
                        name = entry.name,
                        coreMeaning = entry.coreMeaning,
                        upperTrigram = dataset.trigrams.getValue(entry.upperTrigram).toPublicModel(),
                        lowerTrigram = dataset.trigrams.getValue(entry.lowerTrigram).toPublicModel(),
                        keywords = entry.keywords,
                        generalTrend = entry.generalTrend,
                        advice = entry.advice,
                        relationship = entry.relationship,
                        career = entry.career,
                        wealth = entry.wealth,
                    )
                    require(put(entry.code, interpretation) == null) {
                        "Duplicate interpretation for ${entry.code}."
                    }
                }
            }
            require(byCode.keys == expectedCodes) {
                "Interpretation data must cover every one of the 64 binary hexagram codes exactly once."
            }
            return JsonHexagramInterpretationRepository(byCode)
        }

        private val TRIGRAM_IDS = setOf("qian", "dui", "li", "zhen", "xun", "kan", "gen", "kun")
    }

    private data class Dataset(
        val schemaVersion: Int,
        val source: Source,
        val trigrams: Map<String, TrigramEntry>,
        val hexagrams: List<HexagramEntry>,
    )

    private data class Source(
        val name: String,
        val license: String,
        val licenseUrl: String,
        val description: String,
    )

    private data class TrigramEntry(
        val name: String,
        val image: String,
        val meaning: String,
    ) {
        fun toPublicModel() = TrigramInterpretation(name, image, meaning)
    }

    private data class HexagramEntry(
        val code: String,
        val name: String,
        val coreMeaning: String,
        val upperTrigram: String,
        val lowerTrigram: String,
        val keywords: List<String>,
        val generalTrend: String,
        val advice: String,
        val relationship: String,
        val career: String,
        val wealth: String,
    )
}
