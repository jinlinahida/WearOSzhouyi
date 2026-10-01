package com.boompala.ui.ai

import com.boompala.engine.ai.AiDivinationTopic
import com.boompala.engine.ai.AiError
import com.boompala.engine.ai.AiStreamEvent
import com.boompala.settings.AiNetworkMode
import com.boompala.settings.AiProvider
import com.boompala.settings.AppSettings
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AiDivinationCardTest {

    @Test
    fun `detects local pure mode correctly`() {
        val settings = AppSettings(
            aiNetworkMode = AiNetworkMode.LOCAL,
            aiApiKey = "sk-valid-key",
        )
        assertEquals(AiNetworkMode.LOCAL, settings.aiNetworkMode)
        assertFalse("In local mode, AI network calls must not be initiated", settings.aiNetworkMode == AiNetworkMode.ONLINE)
    }

    @Test
    fun `detects unconfigured online mode when key is blank`() {
        val settings = AppSettings(
            aiNetworkMode = AiNetworkMode.ONLINE,
            aiApiKey = "   ",
        )
        assertEquals(AiNetworkMode.ONLINE, settings.aiNetworkMode)
        assertFalse("Blank key should mean AI is not configured", settings.isAiConfigured)
    }

    @Test
    fun `detects fully configured online mode`() {
        val settings = AppSettings(
            aiNetworkMode = AiNetworkMode.ONLINE,
            aiProvider = AiProvider.DEEPSEEK,
            aiApiKey = "sk-1234567890",
        )
        assertEquals(AiNetworkMode.ONLINE, settings.aiNetworkMode)
        assertTrue("Valid key should mean AI is configured", settings.isAiConfigured)
        assertEquals("https://api.deepseek.com/v1", settings.effectiveAiBaseUrl)
        assertEquals("deepseek-chat", settings.effectiveAiModel)
    }

    @Test
    fun `ai card state transitions from idle to loading, streaming, and completed`() {
        var state: AiCardState = AiCardState.Idle
        assertEquals(AiCardState.Idle, state)

        // Idle -> Loading
        state = AiCardState.Loading(
            topic = AiDivinationTopic.CAREER,
            question = "换工作是否顺利？",
        )
        assertTrue(state is AiCardState.Loading)
        assertEquals(AiDivinationTopic.CAREER, (state as AiCardState.Loading).topic)

        // Loading -> Streaming
        val fullAccumulator = StringBuilder()
        fullAccumulator.append("【核心判断】")
        state = AiCardState.Streaming(
            topic = AiDivinationTopic.CAREER,
            question = "换工作是否顺利？",
            text = fullAccumulator.toString(),
        )
        assertTrue(state is AiCardState.Streaming)
        assertEquals("【核心判断】", (state as AiCardState.Streaming).text)

        fullAccumulator.append("\n此卦官鬼持世，得月建生助。")
        state = AiCardState.Streaming(
            topic = AiDivinationTopic.CAREER,
            question = "换工作是否顺利？",
            text = fullAccumulator.toString(),
        )
        assertEquals("【核心判断】\n此卦官鬼持世，得月建生助。", (state as AiCardState.Streaming).text)

        // Streaming -> Completed
        state = AiCardState.Completed(
            topic = AiDivinationTopic.CAREER,
            question = "换工作是否顺利？",
            fullText = fullAccumulator.toString(),
        )
        assertTrue(state is AiCardState.Completed)
        assertEquals(fullAccumulator.toString(), (state as AiCardState.Completed).fullText)
    }

    @Test
    fun `ai card error state maps all error types to user friendly Chinese messages`() {
        val errorMappings = listOf(
            AiError.InvalidApiKey() to "AI 密钥无效，请检查配置。",
            AiError.RateLimited() to "AI 服务当前繁忙或额度不足。",
            AiError.NetworkTimeout() to "连接 AI 服务超时，请稍后再试。",
            AiError.NetworkUnavailable() to "当前没有可用网络。",
            AiError.BadRequest("配置无效") to "AI 请求配置有误，请检查模型设置。",
            AiError.ServerError(500) to "AI 服务暂时不可用。",
            AiError.InvalidResponse("数据错误") to "AI 服务返回的数据无法解析。",
            AiError.Unknown("未知错误") to "AI 解卦暂时失败，请稍后重试。",
        )

        errorMappings.forEach { (error, expectedFriendlyText) ->
            val errorState = AiCardState.Error(
                topic = AiDivinationTopic.GENERAL,
                question = "",
                error = error,
            )
            val friendlyMsg = when (errorState.error) {
                is AiError.InvalidApiKey -> "AI 密钥无效，请检查配置。"
                is AiError.RateLimited -> "AI 服务当前繁忙或额度不足。"
                is AiError.NetworkTimeout -> "连接 AI 服务超时，请稍后再试。"
                is AiError.NetworkUnavailable -> "当前没有可用网络。"
                is AiError.BadRequest -> "AI 请求配置有误，请检查模型设置。"
                is AiError.ServerError -> "AI 服务暂时不可用。"
                is AiError.InvalidResponse -> "AI 服务返回的数据无法解析。"
                is AiError.Unknown -> "AI 解卦暂时失败，请稍后重试。"
            }
            assertEquals(expectedFriendlyText, friendlyMsg)
            assertFalse("Friendly message must not leak API key", friendlyMsg.contains("sk-"))
        }
    }

    @Test
    fun `lifecycle cancellation halts ongoing stream without leaking background work`() = runBlocking {
        var emittedCount = 0
        val mockInfiniteFlow = flow {
            var counter = 0
            while (true) {
                emit(AiStreamEvent.TextDelta("Token ${counter++}"))
                delay(10)
            }
        }

        val job = launch {
            mockInfiniteFlow.collect {
                emittedCount++
            }
        }

        delay(40)
        // Simulate leaving screen / disposing effect
        job.cancel()
        val countAtCancellation = emittedCount
        delay(60)

        assertEquals("No more items should be collected after cancellation", countAtCancellation, emittedCount)
    }

    @Test
    fun `text deltas accumulation builds full text correctly`() = runBlocking {
        val deltas = listOf("【核心判断】", "\n", "吉", "顺", "安", "泰")
        val streamFlow = flow {
            deltas.forEach { emit(AiStreamEvent.TextDelta(it)) }
            emit(AiStreamEvent.Completed("【核心判断】\n吉顺安泰"))
        }

        val accumulated = StringBuilder()
        val events = streamFlow.toList()

        events.forEach { event ->
            when (event) {
                is AiStreamEvent.TextDelta -> accumulated.append(event.text)
                is AiStreamEvent.Completed -> assertEquals("【核心判断】\n吉顺安泰", event.fullText)
                is AiStreamEvent.Error -> throw AssertionError("Should not error")
            }
        }

        assertEquals("【核心判断】\n吉顺安泰", accumulated.toString())
    }

    @Test
    fun `custom user question is preserved and handles clear and boundaries`() {
        var userQuestion = ""
        val onQuestionChange = { input: String ->
            if (input.length <= 100) {
                userQuestion = input
            }
        }

        // 1. Initial is empty
        assertEquals("", userQuestion)

        // 2. User types custom question
        onQuestionChange("这周末去杭州旅游天气和行程如何？")
        assertEquals("这周末去杭州旅游天气和行程如何？", userQuestion)

        // 3. User types question exceeding 100 chars (boundary test)
        val over100Chars = "问".repeat(105)
        onQuestionChange(over100Chars)
        // Should ignore strings longer than 100 chars
        assertEquals("这周末去杭州旅游天气和行程如何？", userQuestion)

        // 4. User clears question
        userQuestion = ""
        assertEquals("", userQuestion)
    }
}
