package com.boompala.engine.data

import com.boompala.engine.model.YaoPosition
import com.google.gson.Gson
import java.io.Reader

/**
 * Keeps the 384 line texts outside calculation and UI code.
 *
 * Implementations keep textual data outside calculation and UI code.
 */
fun interface LineTextRepository {
    fun lineText(
        hexagramCodeFromBottom: String,
        position: YaoPosition,
    ): String?
}

object EmptyLineTextRepository : LineTextRepository {
    override fun lineText(
        hexagramCodeFromBottom: String,
        position: YaoPosition,
    ): String? = null
}

/**
 * Immutable, validated reader for `yao_text.json`.
 *
 * The repository is deliberately constructed from a [Reader] rather than an
 * Android [android.content.res.AssetManager]. This keeps parsing testable in a
 * plain JVM and lets the application decide how an offline asset is supplied.
 */
class JsonLineTextRepository private constructor(
    private val texts: Map<LineTextKey, String>,
) : LineTextRepository {

    override fun lineText(
        hexagramCodeFromBottom: String,
        position: YaoPosition,
    ): String? = texts[LineTextKey(hexagramCodeFromBottom, position.indexFromBottom)]

    companion object {
        const val SCHEMA_VERSION = 1
        const val HEXAGRAM_COUNT = 64
        const val LINE_COUNT = 384

        fun fromReader(reader: Reader): JsonLineTextRepository =
            fromJson(reader.readText())

        fun fromJson(json: String): JsonLineTextRepository {
            val dataset = Gson().fromJson(json, Dataset::class.java)
                ?: error("yao_text.json is empty or invalid JSON.")
            require(dataset.schemaVersion == SCHEMA_VERSION) {
                "Unsupported yao_text.json schema: ${dataset.schemaVersion}."
            }
            require(dataset.hexagrams.size == HEXAGRAM_COUNT) {
                "Expected $HEXAGRAM_COUNT hexagrams, found ${dataset.hexagrams.size}."
            }

            val textByKey = buildMap {
                dataset.hexagrams.forEach { hexagram ->
                    require(hexagram.code.matches(Regex("[01]{6}"))) {
                        "Invalid hexagram code: ${hexagram.code}."
                    }
                    require(hexagram.lines.size == YaoPosition.entries.size) {
                        "Hexagram ${hexagram.code} must have six line texts."
                    }
                    hexagram.lines.forEach { line ->
                        require(line.position in YaoPosition.entries.indices) {
                            "Invalid line position ${line.position} in ${hexagram.code}."
                        }
                        require(line.text.isNotBlank()) {
                            "Blank line text in ${hexagram.code} at ${line.position}."
                        }
                        val key = LineTextKey(hexagram.code, line.position)
                        require(put(key, line.text) == null) {
                            "Duplicate line text for ${hexagram.code} at ${line.position}."
                        }
                    }
                }
            }

            require(textByKey.size == LINE_COUNT) {
                "Expected $LINE_COUNT unique line texts, found ${textByKey.size}."
            }
            return JsonLineTextRepository(textByKey)
        }
    }

    private data class LineTextKey(
        val codeFromBottom: String,
        val positionFromBottom: Int,
    )

    private data class Dataset(
        val schemaVersion: Int,
        val hexagrams: List<HexagramEntry>,
    )

    private data class HexagramEntry(
        val code: String,
        val lines: List<LineEntry>,
    )

    private data class LineEntry(
        val position: Int,
        val text: String,
    )
}
