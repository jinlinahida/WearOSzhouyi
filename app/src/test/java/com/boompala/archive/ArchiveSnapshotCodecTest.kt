package com.boompala.archive

import org.junit.Assert.*
import org.junit.Test

class ArchiveSnapshotCodecTest {
    @Test fun roundTripAndCorruptAreSafe() {
        val json = """{"version":1,"source":"XIAO_LIU_REN","title":"大安","sections":{"最终":["大安"]}}"""
        val snapshot = ArchiveSnapshotCodec.decode(json).getOrThrow()
        assertEquals(ArchiveSource.XIAO_LIU_REN, snapshot.source)
        assertEquals("大安", snapshot.sections["最终"]!!.single())
        assertTrue(ArchiveSnapshotCodec.decode("not-json").isFailure)
        assertTrue(ArchiveSnapshotCodec.decode("{\"version\":99}").isFailure)
    }
    @Test fun sameSnapshotCanBeUsedForIndependentRecords() {
        val json = """{"version":1,"source":"MEI_HUA","title":"火水未济","sections":{}}"""
        assertEquals(ArchiveSnapshotCodec.decode(json).getOrThrow(), ArchiveSnapshotCodec.decode(json).getOrThrow())
    }
}
