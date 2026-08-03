package com.boompala.engine.data

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

class HexagramReferenceTest {
    @Test fun usesAllSixtyFourHexagramsInKingWenOrder() {
        val references = hexagramReferences()
        assertEquals(64, references.size)
        assertEquals((1..64).toList(), references.map { it.order })
        assertEquals("乾为天", references.first().name)
        assertEquals("火水未济", references.last().name)
        assertEquals(64, references.map { it.codeFromBottom }.toSet().size)
    }

    @Test fun classicalTextsCoverAllHexagrams() {
        val asset = File(System.getProperty("yaoTextAssetPath")).reader()
        val classics = asset.use(JsonClassicalTextRepository::fromReader)
        val qian = requireNotNull(classics.textsFor("111111"))
        assertEquals("乾：元亨。利貞。", qian.guaText)
        assertEquals("天行健，君子以自強不息。", qian.imageText)
    }
}
