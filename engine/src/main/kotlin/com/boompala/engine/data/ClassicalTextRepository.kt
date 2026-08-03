package com.boompala.engine.data

import com.google.gson.Gson
import java.io.Reader

data class HexagramClassics(val guaText: String, val tuanText: String, val imageText: String)
fun interface ClassicalTextRepository { fun textsFor(codeFromBottom: String): HexagramClassics? }
object EmptyClassicalTextRepository : ClassicalTextRepository { override fun textsFor(codeFromBottom: String) = null }

class JsonClassicalTextRepository private constructor(private val texts: Map<String, HexagramClassics>) : ClassicalTextRepository {
    override fun textsFor(codeFromBottom: String): HexagramClassics? = texts[codeFromBottom]
    companion object {
        fun fromReader(reader: Reader): JsonClassicalTextRepository {
            val dataset = Gson().fromJson(reader.readText(), Dataset::class.java)
            require(dataset.hexagrams.size == 64)
            return JsonClassicalTextRepository(dataset.hexagrams.associate { entry ->
                require(entry.code.matches(Regex("[01]{6}")) && entry.guaText.isNotBlank() && entry.tuanText.isNotBlank() && entry.imageText.isNotBlank())
                entry.code to HexagramClassics(entry.guaText, entry.tuanText, entry.imageText)
            })
        }
        private data class Dataset(val hexagrams: List<Entry>)
        private data class Entry(val code: String, val guaText: String, val tuanText: String, val imageText: String)
    }
}
