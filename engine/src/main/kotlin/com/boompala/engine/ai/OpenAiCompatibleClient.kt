package com.boompala.engine.ai

import com.google.gson.Gson
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.net.ConnectException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException

/**
 * 抽象 HTTP 传输层接口，方便单元测试与解耦。
 */
interface AiHttpTransport {
    fun post(
        url: String,
        headers: Map<String, String>,
        jsonBody: String,
        connectTimeoutMs: Int = 15000,
        readTimeoutMs: Int = 30000,
    ): HttpResponse

    data class HttpResponse(
        val statusCode: Int,
        val statusMessage: String,
        val inputStream: InputStream,
        val errorStream: InputStream?,
    ) : AutoCloseable {
        override fun close() {
            runCatching { inputStream.close() }
            runCatching { errorStream?.close() }
        }
    }
}

/**
 * 原生 HttpURLConnection 实现，轻量、零外部网络库依赖、完美兼容 Wear OS。
 */
class DefaultHttpUrlConnectionTransport : AiHttpTransport {
    override fun post(
        url: String,
        headers: Map<String, String>,
        jsonBody: String,
        connectTimeoutMs: Int,
        readTimeoutMs: Int,
    ): AiHttpTransport.HttpResponse {
        val targetUrl = URL(url)
        val conn = targetUrl.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.connectTimeout = connectTimeoutMs
        conn.readTimeout = readTimeoutMs
        conn.doOutput = true
        conn.doInput = true
        conn.useCaches = false

        headers.forEach { (key, value) ->
            conn.setRequestProperty(key, value)
        }

        conn.outputStream.use { os ->
            os.write(jsonBody.toByteArray(Charsets.UTF_8))
            os.flush()
        }

        val code = conn.responseCode
        val message = conn.responseMessage ?: ""
        val isSuccess = code in 200..299
        val inStream = if (isSuccess) conn.inputStream else (conn.errorStream ?: ByteArrayInputStream(ByteArray(0)))
        val errStream = if (!isSuccess) inStream else null

        return AiHttpTransport.HttpResponse(code, message, inStream, errStream)
    }
}

/**
 * 独立的 OpenAI-compatible 协议客户端。
 *
 * 统一支持 DeepSeek, OpenAI, Moonshot(Kimi) 以及任何兼容 OpenAI 接口规范的第三方中转或自建反代。
 * 支持 Server-Sent Events (SSE) 流式传输，并将其转换为 Kotlin Flow。
 */
class OpenAiCompatibleClient(
    private val transport: AiHttpTransport = DefaultHttpUrlConnectionTransport(),
    private val gson: Gson = Gson(),
) {

    /**
     * 发起流式对话请求。
     *
     * @param baseUrl 用户配置的服务商 Base URL（自动规范化）
     * @param apiKey 服务商 API Key（安全传输，严禁在日志中泄露）
     * @param request 请求体模型
     * @return 包含文本增量、完成与错误事件的 Flow
     */
    fun streamChat(
        baseUrl: String,
        apiKey: String,
        request: AiChatRequest,
    ): Flow<AiStreamEvent> = flow {
        val normalizedUrl = OpenAiUrlNormalizer.normalize(baseUrl)
        if (normalizedUrl.isBlank()) {
            emit(AiStreamEvent.Error(AiError.BadRequest("Base URL 不能为空")))
            return@flow
        }

        val cleanKey = apiKey.trim()
        if (cleanKey.isBlank()) {
            emit(AiStreamEvent.Error(AiError.InvalidApiKey("API Key 不能为空")))
            return@flow
        }

        val headers = mapOf(
            "Content-Type" to "application/json; charset=utf-8",
            "Accept" to "text/event-stream",
            "Authorization" to "Bearer $cleanKey",
        )

        val jsonBody = gson.toJson(request)

        val response = try {
            transport.post(normalizedUrl, headers, jsonBody)
        } catch (e: SocketTimeoutException) {
            emit(AiStreamEvent.Error(AiError.NetworkTimeout("网络连接或读取超时，请稍后重试", e)))
            return@flow
        } catch (e: UnknownHostException) {
            emit(AiStreamEvent.Error(AiError.NetworkUnavailable("无法解析服务商域名，请检查手表网络", e)))
            return@flow
        } catch (e: ConnectException) {
            emit(AiStreamEvent.Error(AiError.NetworkUnavailable("无法连接至服务商服务器，请检查网络", e)))
            return@flow
        } catch (e: java.io.IOException) {
            emit(AiStreamEvent.Error(AiError.NetworkUnavailable("网络请求失败: ${e.message}", e)))
            return@flow
        } catch (e: Throwable) {
            emit(AiStreamEvent.Error(AiError.Unknown("网络未知异常: ${e.message}", e)))
            return@flow
        }

        response.use { resp ->
            when (resp.statusCode) {
                200, 201 -> {
                    parseSseStream(resp.inputStream) { event ->
                        emit(event)
                    }
                }
                401 -> {
                    emit(AiStreamEvent.Error(AiError.InvalidApiKey()))
                }
                429 -> {
                    val detail = extractErrorDetail(resp.errorStream)
                    emit(AiStreamEvent.Error(AiError.RateLimited(detail ?: "请求过于频繁或额度已耗尽 (HTTP 429)")))
                }
                in 400..499 -> {
                    val detail = extractErrorDetail(resp.errorStream)
                    emit(AiStreamEvent.Error(AiError.BadRequest(detail)))
                }
                in 500..599 -> {
                    val detail = extractErrorDetail(resp.errorStream)
                    emit(AiStreamEvent.Error(AiError.ServerError(resp.statusCode, detail)))
                }
                else -> {
                    val detail = extractErrorDetail(resp.errorStream)
                    emit(AiStreamEvent.Error(AiError.Unknown("HTTP ${resp.statusCode}: $detail")))
                }
            }
        }
    }.flowOn(Dispatchers.IO)

    /**
     * 解析 SSE 流式内容并逐项发射。
     */
    private suspend fun parseSseStream(
        inputStream: InputStream,
        emitEvent: suspend (AiStreamEvent) -> Unit,
    ) {
        val reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))
        val fullTextBuilder = StringBuilder()
        var hasEmittedCompleted = false

        try {
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val currentLine = line?.trim().orEmpty()
                if (currentLine.isBlank()) continue
                if (currentLine.startsWith(":")) continue // Ping / comment line

                if (currentLine.startsWith("data:")) {
                    val dataContent = currentLine.removePrefix("data:").trim()
                    if (dataContent == "[DONE]") {
                        if (!hasEmittedCompleted) {
                            emitEvent(AiStreamEvent.Completed(fullTextBuilder.toString()))
                            hasEmittedCompleted = true
                        }
                        break
                    }

                    val deltaText = extractDeltaContent(dataContent)
                    if (!deltaText.isNullOrEmpty()) {
                        fullTextBuilder.append(deltaText)
                        emitEvent(AiStreamEvent.TextDelta(deltaText))
                    }
                }
            }

            if (!hasEmittedCompleted) {
                emitEvent(AiStreamEvent.Completed(fullTextBuilder.toString()))
                hasEmittedCompleted = true
            }
        } catch (e: Throwable) {
            if (!hasEmittedCompleted) {
                emitEvent(AiStreamEvent.Error(AiError.InvalidResponse("解析流式响应中断", e)))
            }
        }
    }

    /**
     * 从单条 SSE data JSON 中提取 delta.content。
     */
    internal fun extractDeltaContent(jsonStr: String): String? {
        return runCatching {
            val jsonElement = JsonParser.parseString(jsonStr)
            if (!jsonElement.isJsonObject) return null
            val root = jsonElement.asJsonObject
            val choices = root.getAsJsonArray("choices") ?: return null
            if (choices.size() == 0) return null
            val firstChoice = choices[0].asJsonObject
            val delta = firstChoice.getAsJsonObject("delta") ?: return null
            if (delta.has("content") && !delta.get("content").isJsonNull) {
                delta.get("content").asString
            } else {
                null
            }
        }.getOrNull()
    }

    /**
     * 从错误流中安全提取错误说明。
     */
    private fun extractErrorDetail(errorStream: InputStream?): String? {
        if (errorStream == null) return null
        return runCatching {
            val text = errorStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            if (text.isBlank()) return null
            val jsonElement = JsonParser.parseString(text)
            if (jsonElement.isJsonObject) {
                val errorObj = jsonElement.asJsonObject.getAsJsonObject("error")
                if (errorObj != null && errorObj.has("message")) {
                    return errorObj.get("message").asString
                }
            }
            text.take(150)
        }.getOrNull()
    }
}
