package com.boompala.engine.ai

/**
 * 规范化用户配置的 OpenAI-compatible Base URL。
 *
 * 保证兼容各种输入习惯：
 * - "https://api.deepseek.com" -> "https://api.deepseek.com/v1/chat/completions"
 * - "https://api.deepseek.com/" -> "https://api.deepseek.com/v1/chat/completions"
 * - "https://api.deepseek.com/v1" -> "https://api.deepseek.com/v1/chat/completions"
 * - "https://api.deepseek.com/v1/" -> "https://api.deepseek.com/v1/chat/completions"
 * - "https://example.com/openai/v1" -> "https://example.com/openai/v1/chat/completions"
 * - "https://example.com/v1/chat/completions" -> "https://example.com/v1/chat/completions"
 *
 * 绝对不会产生 "/v1/v1/chat/completions"。
 */
object OpenAiUrlNormalizer {

    fun normalize(rawBaseUrl: String): String {
        var trimmed = rawBaseUrl.trim()
        if (trimmed.isBlank()) {
            return ""
        }

        while (trimmed.endsWith("/")) {
            trimmed = trimmed.dropLast(1).trim()
        }

        if (trimmed.endsWith("/chat/completions")) {
            return trimmed
        }

        if (trimmed.endsWith("/v1")) {
            return "$trimmed/chat/completions"
        }

        return "$trimmed/v1/chat/completions"
    }
}
