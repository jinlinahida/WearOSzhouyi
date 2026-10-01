package com.boompala.engine.ai

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AiStreamBufferTest {

    @Test
    fun `bufferTextDeltas aggregates rapid tokens into combined deltas`() = runBlocking {
        val rawFlow = flow {
            emit(AiStreamEvent.TextDelta("天"))
            delay(10)
            emit(AiStreamEvent.TextDelta("风"))
            delay(10)
            emit(AiStreamEvent.TextDelta("姤"))
            delay(10)
            emit(AiStreamEvent.TextDelta("卦"))
            delay(10)
            emit(AiStreamEvent.Completed("天风姤卦"))
        }

        val bufferedEvents = rawFlow.bufferTextDeltas(windowMs = 80L).toList()

        // Rather than 4 distinct 1-character events + Completed, the rapid tokens should be combined
        val textEvents = bufferedEvents.filterIsInstance<AiStreamEvent.TextDelta>()
        assertTrue("Tokens should be aggregated into fewer chunks (got ${textEvents.size})", textEvents.size <= 2)
        val combinedText = textEvents.joinToString("") { it.text }
        assertEquals("天风姤卦", combinedText)

        val completed = bufferedEvents.filterIsInstance<AiStreamEvent.Completed>()
        assertEquals(1, completed.size)
        assertEquals("天风姤卦", completed[0].fullText)
    }

    @Test
    fun `bufferTextDeltas flushes remaining buffer before emitting Error`() = runBlocking {
        val rawFlow = flow {
            emit(AiStreamEvent.TextDelta("推断"))
            delay(10)
            emit(AiStreamEvent.Error(AiError.NetworkTimeout("超时")))
        }

        val bufferedEvents = rawFlow.bufferTextDeltas(windowMs = 80L).toList()

        assertEquals(2, bufferedEvents.size)
        assertEquals(AiStreamEvent.TextDelta("推断"), bufferedEvents[0])
        assertTrue(bufferedEvents[1] is AiStreamEvent.Error)
        assertEquals("超时", (bufferedEvents[1] as AiStreamEvent.Error).error.message)
    }
}
