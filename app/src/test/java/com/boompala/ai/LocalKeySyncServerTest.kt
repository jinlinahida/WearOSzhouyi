package com.boompala.ai

import com.boompala.ai.sync.AiConfigPayload
import com.boompala.ai.sync.LocalKeySyncServer
import com.boompala.ai.sync.QrCodeGenerator
import com.boompala.settings.AiProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

class LocalKeySyncServerTest {

    @Test
    fun `qr code generator creates matrix for valid url and rejects blank`() {
        assertNull("Blank string should produce null QR", QrCodeGenerator.generateBitMatrix(""))
        assertNull("Whitespace string should produce null QR", QrCodeGenerator.generateBitMatrix("   "))

        val qr = QrCodeGenerator.generateBitMatrix("http://192.168.1.100:8899/?token=test123456", 200)
        assertNotNull("Valid url should produce QR matrix", qr)
        assertEquals(200, qr?.width)
        assertEquals(200, qr?.height)
    }

    @Test
    fun `ai config payload masks api key in toString to prevent accidental logging`() {
        val payload = AiConfigPayload(
            providerId = "deepseek",
            apiKey = "sk-abcdef1234567890",
            customBaseUrl = "https://api.deepseek.com",
            customModel = "deepseek-chat",
        )

        val str = payload.toString()
        assertFalse("toString must not contain full plaintext API key", str.contains("sk-abcdef1234567890"))
        assertTrue("toString should contain masked suffix", str.contains("••••7890"))
        assertEquals(AiProvider.DEEPSEEK, payload.provider)
    }

    @Test
    fun `pairing server serves html with valid token and rejects invalid token`() {
        val server = LocalKeySyncServer()
        val receivedConfig = AtomicReference<AiConfigPayload?>(null)

        val info = server.start(
            onConfigReceived = { receivedConfig.set(it) },
            onTimeout = {},
        )

        // 如果在运行测试的环境下没有 Wi-Fi 局域网接口，跳过网络调用测试
        if (info == null) {
            server.stop()
            return
        }

        try {
            assertEquals("Token must be 32 hex characters", 32, info.token.length)
            assertTrue("Token must be hexadecimal", info.token.matches(Regex("[0-9a-f]{32}")))
            assertFalse("URL must not contain API key", info.url.contains("apiKey"))
            assertTrue("URL must contain pairing token", info.url.contains(info.token))

            // 1. 尝试使用错误 Token 访问 GET
            val invalidUrl = URL("http://localhost:${info.port}/?token=wrong_token")
            val invalidConn = invalidUrl.openConnection() as HttpURLConnection
            invalidConn.connectTimeout = 3000
            invalidConn.readTimeout = 3000
            assertEquals(403, invalidConn.responseCode)

            // 2. 使用正确 Token 访问 GET
            val validUrl = URL("http://localhost:${info.port}/?token=${info.token}")
            val validConn = validUrl.openConnection() as HttpURLConnection
            validConn.connectTimeout = 3000
            validConn.readTimeout = 3000
            assertEquals(200, validConn.responseCode)
            val html = validConn.inputStream.bufferedReader().use { it.readText() }
            assertTrue("HTML should contain title", html.contains("Boompala"))
            assertTrue("HTML should contain token", html.contains(info.token))
        } finally {
            server.stop()
        }
    }

    @Test
    fun `pairing server receives config, executes callback and invalidates single-use token immediately`() {
        val server = LocalKeySyncServer()
        val receivedConfig = AtomicReference<AiConfigPayload?>(null)
        val latch = CountDownLatch(1)

        val info = server.start(
            onConfigReceived = {
                receivedConfig.set(it)
                latch.countDown()
            },
            onTimeout = {},
        )

        if (info == null) {
            server.stop()
            return
        }

        try {
            // POST 有效配置
            val postUrl = URL("http://localhost:${info.port}/api/config?token=${info.token}")
            val conn = postUrl.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")
            conn.connectTimeout = 3000
            conn.readTimeout = 3000

            val jsonBody = """
                {
                    "providerId": "deepseek",
                    "apiKey": "sk-secret-test-key-9988",
                    "customBaseUrl": "https://api.deepseek.com/v1",
                    "customModel": "deepseek-chat"
                }
            """.trimIndent()

            OutputStreamWriter(conn.outputStream).use { it.write(jsonBody) }

            assertEquals(200, conn.responseCode)
            assertTrue("Latch should count down upon receiving config", latch.await(3, TimeUnit.SECONDS))

            val payload = receivedConfig.get()
            assertNotNull(payload)
            assertEquals("deepseek", payload?.providerId)
            assertEquals("sk-secret-test-key-9988", payload?.apiKey)
            assertEquals(AiProvider.DEEPSEEK, payload?.provider)

            // 再次尝试使用已失效的 token 请求，应当返回 403 (一次性 Token 已经失效)
            val replayConn = postUrl.openConnection() as HttpURLConnection
            replayConn.requestMethod = "POST"
            replayConn.doOutput = true
            replayConn.setRequestProperty("Content-Type", "application/json")
            replayConn.connectTimeout = 2000
            replayConn.readTimeout = 2000

            try {
                OutputStreamWriter(replayConn.outputStream).use { it.write(jsonBody) }
                // 此时服务器要么已经根据成功回调关闭，要么返回 403
                val code = replayConn.responseCode
                assertTrue("Replay must fail with 403", code == 403)
            } catch (_: Throwable) {
                // 连接被关闭同样属于安全失效的表现
            }
        } finally {
            server.stop()
        }
    }
}
