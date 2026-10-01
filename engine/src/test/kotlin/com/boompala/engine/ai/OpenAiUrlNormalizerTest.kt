package com.boompala.engine.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class OpenAiUrlNormalizerTest {

    @Test
    fun `normalizes urls without v1 and trailing slash`() {
        val result = OpenAiUrlNormalizer.normalize("https://api.deepseek.com")
        assertEquals("https://api.deepseek.com/v1/chat/completions", result)
    }

    @Test
    fun `normalizes urls with trailing slash`() {
        val result = OpenAiUrlNormalizer.normalize("https://api.deepseek.com/")
        assertEquals("https://api.deepseek.com/v1/chat/completions", result)
    }

    @Test
    fun `normalizes urls ending with v1`() {
        val result = OpenAiUrlNormalizer.normalize("https://api.deepseek.com/v1")
        assertEquals("https://api.deepseek.com/v1/chat/completions", result)
        assertFalse("Should never contain /v1/v1", result.contains("/v1/v1"))
    }

    @Test
    fun `normalizes urls ending with v1 and trailing slash`() {
        val result = OpenAiUrlNormalizer.normalize("https://api.deepseek.com/v1/")
        assertEquals("https://api.deepseek.com/v1/chat/completions", result)
        assertFalse("Should never contain /v1/v1", result.contains("/v1/v1"))
    }

    @Test
    fun `handles multiple trailing slashes and spaces`() {
        val result = OpenAiUrlNormalizer.normalize("  https://api.openai.com/v1///   ")
        assertEquals("https://api.openai.com/v1/chat/completions", result)
    }

    @Test
    fun `preserves existing full completions path`() {
        val result = OpenAiUrlNormalizer.normalize("https://api.custom.com/v1/chat/completions")
        assertEquals("https://api.custom.com/v1/chat/completions", result)
    }

    @Test
    fun `handles custom subpath ending with v1`() {
        val result = OpenAiUrlNormalizer.normalize("https://proxy.corp.internal/gateway/openai/v1")
        assertEquals("https://proxy.corp.internal/gateway/openai/v1/chat/completions", result)
        assertFalse("Should never contain /v1/v1", result.contains("/v1/v1"))
    }

    @Test
    fun `returns empty for blank input`() {
        assertEquals("", OpenAiUrlNormalizer.normalize(""))
        assertEquals("", OpenAiUrlNormalizer.normalize("   "))
    }
}
