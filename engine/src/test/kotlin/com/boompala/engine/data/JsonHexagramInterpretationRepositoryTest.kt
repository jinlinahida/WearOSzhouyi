package com.boompala.engine.data

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class JsonHexagramInterpretationRepositoryTest {

    @Test
    fun `offline asset covers every hexagram exactly once and exposes fixed case references`() {
        val asset = File(requireNotNull(System.getProperty("hexagramInterpretationAssetPath")))
        assertTrue("Missing asset at ${asset.absolutePath}", asset.isFile)

        val repository = asset.bufferedReader().use(JsonHexagramInterpretationRepository::fromReader)
        val original = requireNotNull(repository.interpretationFor("010101"))
        val changed = requireNotNull(repository.interpretationFor("110001"))

        assertEquals("火水未济", original.name)
        assertEquals("离", original.upperTrigram.name)
        assertEquals("坎", original.lowerTrigram.name)
        assertEquals("山泽损", changed.name)
        assertEquals("艮", changed.upperTrigram.name)
        assertEquals("兑", changed.lowerTrigram.name)
        assertNotNull(original.relationship)
        assertNotNull(changed.career)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `repository rejects data that does not cover all sixty four codes`() {
        JsonHexagramInterpretationRepository.fromJson(
            """
            {
              "schemaVersion": 1,
              "source": {
                "name": "test",
                "license": "test",
                "licenseUrl": "test",
                "description": "test"
              },
              "trigrams": {},
              "hexagrams": []
            }
            """.trimIndent(),
        )
    }
}
