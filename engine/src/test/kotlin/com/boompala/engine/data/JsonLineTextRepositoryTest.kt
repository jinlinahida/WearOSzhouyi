package com.boompala.engine.data

import com.boompala.engine.model.YaoPosition
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class JsonLineTextRepositoryTest {

    @Test
    fun `offline asset contains all 384 positional line texts`() {
        val asset = File(requireNotNull(System.getProperty("yaoTextAssetPath")))
        assertTrue("Missing asset at ${asset.absolutePath}", asset.isFile)

        val repository = asset.bufferedReader().use(JsonLineTextRepository::fromReader)

        assertEquals("潛龍勿用。", repository.lineText("111111", YaoPosition.FIRST))
        assertEquals("濡其尾，吝。", repository.lineText("010101", YaoPosition.FIRST))
        assertEquals(
            "貞吉，悔亡，震用伐鬼方，三年有賞于大國。",
            repository.lineText("010101", YaoPosition.FOURTH),
        )
        assertEquals("龍戰于野，其血玄黃。", repository.lineText("000000", YaoPosition.TOP))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `repository rejects an incomplete dataset`() {
        JsonLineTextRepository.fromJson(
            """
            {
              "schemaVersion": 1,
              "hexagrams": []
            }
            """.trimIndent(),
        )
    }
}
