package com.boompala.ai.sync

import com.boompala.settings.AiProvider

/**
 * 扫码配对接收到的 AI 配置载荷。
 */
data class AiConfigPayload(
    val providerId: String = "deepseek",
    val apiKey: String = "",
    val customBaseUrl: String = "",
    val customModel: String = "",
) {
    val provider: AiProvider
        get() = AiProvider.fromId(providerId)

    // 严防日志或调试打印泄露完整 API Key
    override fun toString(): String {
        val masked = if (apiKey.length <= 4) "••••" else "••••" + apiKey.takeLast(4)
        return "AiConfigPayload(providerId='$providerId', apiKey='$masked', customBaseUrl='$customBaseUrl', customModel='$customModel')"
    }
}
