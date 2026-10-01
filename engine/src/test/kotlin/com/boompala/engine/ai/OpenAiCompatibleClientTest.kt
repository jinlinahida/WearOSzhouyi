package com.boompala.engine.ai

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class OpenAiCompatibleClientTest {

    private class FakeAiHttpTransport(
        private val responseCode: Int = 200,
        private val responseBody: String = "",
        private val errorBody: String? = null,
        private val throwException: Throwable? = null,
    ) : AiHttpTransport {
        var recordedUrl: String? = null
        var recordedHeaders: Map<String, String>? = null
        var recordedBody: String? = null

        override fun post(
            url: String,
            headers: Map<String, String>,
            jsonBody: String,
            connectTimeoutMs: Int,
            readTimeoutMs: Int,
        ): AiHttpTransport.HttpResponse {
            recordedUrl = url
            recordedHeaders = headers
            recordedBody = jsonBody

            if (throwException != null) {
                throw throwException
            }

            val inStream = ByteArrayInputStream(responseBody.toByteArray(Charsets.UTF_8))
            val errStream = errorBody?.let { ByteArrayInputStream(it.toByteArray(Charsets.UTF_8)) }
            return AiHttpTransport.HttpResponse(
                statusCode = responseCode,
                statusMessage = if (responseCode == 200) "OK" else "Error",
                inputStream = inStream,
                errorStream = errStream,
            )
        }
    }

    private val sampleRequest = AiChatRequest(
        model = "deepseek-chat",
        messages = listOf(
            AiChatMessage(role = "user", content = "求测"),
        ),
    )

    @Test
    fun `streamChat parses valid sse chunks and emits text deltas and completed`() = runBlocking {
        val sseData = """
            : keep-alive
            data: {"choices":[{"delta":{"content":"此卦"}}]}

            data: {"choices":[{"delta":{"content":"为吉"}}]}
            data: [DONE]
        """.trimIndent()

        val transport = FakeAiHttpTransport(responseCode = 200, responseBody = sseData)
        val client = OpenAiCompatibleClient(transport = transport)

        val events = client.streamChat(
            baseUrl = "https://api.deepseek.com",
            apiKey = "sk-secret123456",
            request = sampleRequest,
        ).toList()

        assertEquals("https://api.deepseek.com/v1/chat/completions", transport.recordedUrl)
        assertEquals("Bearer sk-secret123456", transport.recordedHeaders?.get("Authorization"))
        assertEquals("text/event-stream", transport.recordedHeaders?.get("Accept"))

        assertEquals(3, events.size)
        assertEquals(AiStreamEvent.TextDelta("此卦"), events[0])
        assertEquals(AiStreamEvent.TextDelta("为吉"), events[1])
        assertTrue(events[2] is AiStreamEvent.Completed)
        assertEquals("此卦为吉", (events[2] as AiStreamEvent.Completed).fullText)
    }

    @Test
    fun `streamChat handles empty lines and malformed json gracefully`() = runBlocking {
        val sseData = """
            data: {"invalid_json": true
            data: {"choices":[{"delta":{"content":"正常"}}]}
            data: {}
            data: {"choices":[]}
            data: {"choices":[{"delta":{"content":"内容"}}]}
            data: [DONE]
        """.trimIndent()

        val transport = FakeAiHttpTransport(responseCode = 200, responseBody = sseData)
        val client = OpenAiCompatibleClient(transport = transport)

        val events = client.streamChat(
            baseUrl = "https://api.openai.com/v1",
            apiKey = "sk-test",
            request = sampleRequest,
        ).toList()

        val deltas = events.filterIsInstance<AiStreamEvent.TextDelta>()
        assertEquals(2, deltas.size)
        assertEquals("正常", deltas[0].text)
        assertEquals("内容", deltas[1].text)
        assertTrue(events.last() is AiStreamEvent.Completed)
        assertEquals("正常内容", (events.last() as AiStreamEvent.Completed).fullText)
    }

    @Test
    fun `streamChat maps 401 to InvalidApiKey without leaking key`() = runBlocking {
        val transport = FakeAiHttpTransport(
            responseCode = 401,
            errorBody = """{"error":{"message":"Incorrect API key provided"}}""",
        )
        val client = OpenAiCompatibleClient(transport = transport)

        val events = client.streamChat(
            baseUrl = "https://api.openai.com/v1",
            apiKey = "sk-secret123456",
            request = sampleRequest,
        ).toList()

        assertEquals(1, events.size)
        val err = events[0] as AiStreamEvent.Error
        assertTrue(err.error is AiError.InvalidApiKey)
        assertFalse("Error message must not contain secret key", err.error.message.contains("sk-secret123456"))
    }

    @Test
    fun `streamChat maps 429 to RateLimited`() = runBlocking {
        val transport = FakeAiHttpTransport(
            responseCode = 429,
            errorBody = """{"error":{"message":"Rate limit reached"}}""",
        )
        val client = OpenAiCompatibleClient(transport = transport)

        val events = client.streamChat(
            baseUrl = "https://api.openai.com/v1",
            apiKey = "sk-test",
            request = sampleRequest,
        ).toList()

        assertEquals(1, events.size)
        val err = events[0] as AiStreamEvent.Error
        assertTrue(err.error is AiError.RateLimited)
        assertTrue(err.error.message.contains("Rate limit reached"))
    }

    @Test
    fun `streamChat maps 500 to ServerError`() = runBlocking {
        val transport = FakeAiHttpTransport(
            responseCode = 500,
            errorBody = """{"error":{"message":"Internal server error"}}""",
        )
        val client = OpenAiCompatibleClient(transport = transport)

        val events = client.streamChat(
            baseUrl = "https://api.moonshot.cn/v1",
            apiKey = "sk-test",
            request = sampleRequest,
        ).toList()

        assertEquals(1, events.size)
        val err = events[0] as AiStreamEvent.Error
        assertTrue(err.error is AiError.ServerError)
        assertEquals(500, (err.error as AiError.ServerError).statusCode)
    }

    @Test
    fun `streamChat maps timeout exception to NetworkTimeout`() = runBlocking {
        val transport = FakeAiHttpTransport(
            throwException = SocketTimeoutException("Read timed out"),
        )
        val client = OpenAiCompatibleClient(transport = transport)

        val events = client.streamChat(
            baseUrl = "https://api.deepseek.com",
            apiKey = "sk-test",
            request = sampleRequest,
        ).toList()

        assertEquals(1, events.size)
        val err = events[0] as AiStreamEvent.Error
        assertTrue(err.error is AiError.NetworkTimeout)
    }

    @Test
    fun `streamChat maps network failures to NetworkUnavailable`() = runBlocking {
        val transport = FakeAiHttpTransport(
            throwException = UnknownHostException("api.deepseek.com"),
        )
        val client = OpenAiCompatibleClient(transport = transport)

        val events = client.streamChat(
            baseUrl = "https://api.deepseek.com",
            apiKey = "sk-test",
            request = sampleRequest,
        ).toList()

        assertEquals(1, events.size)
        val err = events[0] as AiStreamEvent.Error
        assertTrue(err.error is AiError.NetworkUnavailable)
    }

    @Test
    fun `streamChat rejects blank baseUrl or apiKey upfront`() = runBlocking {
        val client = OpenAiCompatibleClient(transport = FakeAiHttpTransport())

        val events1 = client.streamChat(
            baseUrl = "",
            apiKey = "sk-test",
            request = sampleRequest,
        ).toList()
        assertTrue((events1[0] as AiStreamEvent.Error).error is AiError.BadRequest)

        val events2 = client.streamChat(
            baseUrl = "https://api.deepseek.com",
            apiKey = "   ",
            request = sampleRequest,
        ).toList()
        assertTrue((events2[0] as AiStreamEvent.Error).error is AiError.InvalidApiKey)
    }

    @Test
    fun `streamChat normalizes urls correctly without v1 duplicate`() = runBlocking {
        val transport = FakeAiHttpTransport(responseCode = 200, responseBody = "data: [DONE]\n\n")
        val client = OpenAiCompatibleClient(transport = transport)

        // Case 1: base with /v1
        client.streamChat("https://api.openai.com/v1", "sk-test", sampleRequest).toList()
        assertEquals("https://api.openai.com/v1/chat/completions", transport.recordedUrl)

        // Case 2: base with /v1/ trailing slash
        client.streamChat("https://api.openai.com/v1/", "sk-test", sampleRequest).toList()
        assertEquals("https://api.openai.com/v1/chat/completions", transport.recordedUrl)

        // Case 3: base without /v1
        client.streamChat("https://api.deepseek.com", "sk-test", sampleRequest).toList()
        assertEquals("https://api.deepseek.com/v1/chat/completions", transport.recordedUrl)

        // Case 4: base with explicit full endpoint
        client.streamChat("https://custom.ai.com/v1/chat/completions", "sk-test", sampleRequest).toList()
        assertEquals("https://custom.ai.com/v1/chat/completions", transport.recordedUrl)
    }

    @Test
    fun `streamChat maps 400 to BadRequest`() = runBlocking {
        val transport = FakeAiHttpTransport(
            responseCode = 400,
            errorBody = """{"error":{"message":"Invalid model parameter"}}""",
        )
        val client = OpenAiCompatibleClient(transport = transport)

        val events = client.streamChat(
            baseUrl = "https://api.openai.com/v1",
            apiKey = "sk-test",
            request = sampleRequest,
        ).toList()

        assertEquals(1, events.size)
        val err = events[0] as AiStreamEvent.Error
        assertTrue(err.error is AiError.BadRequest)
        assertTrue(err.error.message.contains("Invalid model parameter"))
    }
}
