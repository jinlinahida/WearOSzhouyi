package com.boompala.engine.data

import com.google.gson.Gson
import java.io.Reader

data class KnowledgeArticle(val id: String, val category: String, val title: String, val summary: String, val body: String)

fun interface KnowledgeRepository { fun articles(): List<KnowledgeArticle> }

class JsonKnowledgeRepository private constructor(private val values: List<KnowledgeArticle>) : KnowledgeRepository {
    override fun articles(): List<KnowledgeArticle> = values
    companion object {
        fun fromReader(reader: Reader): JsonKnowledgeRepository {
            val dataset = Gson().fromJson(reader.readText(), Dataset::class.java)
            require(dataset.schemaVersion == 1)
            require(dataset.source.name.isNotBlank() && dataset.source.license.isNotBlank())
            require(dataset.articles.isNotEmpty() && dataset.articles.all { it.id.isNotBlank() && it.title.isNotBlank() && it.summary.isNotBlank() && it.body.isNotBlank() })
            return JsonKnowledgeRepository(dataset.articles)
        }
        private data class Dataset(val schemaVersion: Int, val source: Source, val articles: List<KnowledgeArticle>)
        private data class Source(val name: String, val license: String)
    }
}
