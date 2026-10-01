package com.boompala.ai.sync

import com.google.gson.Gson
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.URLDecoder
import java.util.Collections
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 局域网临时扫码配对服务器。
 * 仅在用户主动进入扫码界面时启动，配对成功、取消或超时（5分钟）后彻底关闭。
 * 不常驻后台，不向公网暴露，使用单次有效 Token。
 */
class LocalKeySyncServer {

    data class PairingInfo(
        val ip: String,
        val port: Int,
        val token: String,
        val url: String,
    )

    private val gson = Gson()
    private val isRunning = AtomicBoolean(false)
    private val isTokenUsed = AtomicBoolean(false)
    private var serverSocket: ServerSocket? = null
    private var pairingToken: String? = null
    private var workerThread: Thread? = null
    private var timeoutScheduler: ScheduledExecutorService? = null

    /**
     * 获取手表局域网 IPv4 地址。
     */
    fun getLocalWifiIpAddress(): String? {
        return try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            // 优先查找 Wi-Fi 接口 (wlan)
            val sorted = interfaces.sortedByDescending { it.name.startsWith("wlan") || it.name.startsWith("eth") }
            for (intf in sorted) {
                if (intf.isLoopback || !intf.isUp) continue
                val addresses = Collections.list(intf.inetAddresses)
                for (addr in addresses) {
                    if (!addr.isLoopbackAddress && addr is Inet4Address && addr.isSiteLocalAddress) {
                        val host = addr.hostAddress
                        if (!host.isNullOrBlank()) {
                            return host
                        }
                    }
                }
            }
            null
        } catch (_: Throwable) {
            null
        }
    }

    /**
     * 启动临时配对服务器。
     * @param onConfigReceived 接收到有效配置时的回调
     * @param onTimeout 5分钟超时未完成时的回调
     */
    @Synchronized
    fun start(
        onConfigReceived: (AiConfigPayload) -> Unit,
        onTimeout: () -> Unit = {},
    ): PairingInfo? {
        if (isRunning.get()) {
            stop()
        }

        val ip = getLocalWifiIpAddress() ?: return null
        val token = UUID.randomUUID().toString().replace("-", "")
        pairingToken = token
        isTokenUsed.set(false)

        val ss = try {
            ServerSocket(8899).apply { soTimeout = 1000 }
        } catch (_: Throwable) {
            try {
                ServerSocket(0).apply { soTimeout = 1000 }
            } catch (_: Throwable) {
                return null
            }
        }
        serverSocket = ss
        isRunning.set(true)

        val port = ss.localPort
        val pairingUrl = "http://$ip:$port/?token=$token"

        // 5 分钟安全超时
        val scheduler = Executors.newSingleThreadScheduledExecutor()
        timeoutScheduler = scheduler
        scheduler.schedule({
            if (isRunning.get()) {
                stop()
                onTimeout()
            }
        }, 5, TimeUnit.MINUTES)

        workerThread = Thread({
            while (isRunning.get()) {
                val client: Socket = try {
                    ss.accept()
                } catch (_: SocketTimeoutException) {
                    continue
                } catch (_: SocketException) {
                    break
                } catch (_: Throwable) {
                    break
                }

                // 处理单次请求
                handleConnection(client, onConfigReceived)
            }
        }, "BoompalaKeySyncThread").apply {
            isDaemon = true
            start()
        }

        return PairingInfo(
            ip = ip,
            port = port,
            token = token,
            url = pairingUrl,
        )
    }

    @Synchronized
    fun stop() {
        isRunning.set(false)
        pairingToken = null
        try {
            serverSocket?.close()
        } catch (_: Throwable) {
        }
        serverSocket = null

        try {
            timeoutScheduler?.shutdownNow()
        } catch (_: Throwable) {
        }
        timeoutScheduler = null

        workerThread?.interrupt()
        workerThread = null
    }

    private fun handleConnection(client: Socket, onConfigReceived: (AiConfigPayload) -> Unit) {
        Thread({
            try {
                client.soTimeout = 8000
                val reader = BufferedReader(InputStreamReader(client.getInputStream(), Charsets.UTF_8))
                val output = client.getOutputStream()

                val requestLine = reader.readLine() ?: return@Thread
                val parts = requestLine.split(" ")
                if (parts.size < 2) return@Thread

                val method = parts[0].uppercase()
                val fullUri = parts[1]
                val path = fullUri.substringBefore("?")
                val queryString = fullUri.substringAfter("?", "")

                val params = parseQuery(queryString)
                val tokenFromRequest = params["token"]

                // 读取 Header
                var contentLength = 0
                while (true) {
                    val headerLine = reader.readLine() ?: break
                    if (headerLine.isEmpty()) break
                    if (headerLine.startsWith("Content-Length:", ignoreCase = true)) {
                        contentLength = headerLine.substringAfter(":").trim().toIntOrNull() ?: 0
                    }
                }

                when (method) {
                    "OPTIONS" -> {
                        sendResponse(
                            output,
                            statusCode = 204,
                            statusText = "No Content",
                            contentType = "text/plain",
                            body = "",
                        )
                    }

                    "GET" -> {
                        if (path == "/" || path == "/index.html") {
                            val currentToken = pairingToken
                            if (currentToken != null && tokenFromRequest == currentToken && !isTokenUsed.get()) {
                                val html = generateHtmlPage(tokenFromRequest)
                                sendResponse(
                                    output,
                                    statusCode = 200,
                                    statusText = "OK",
                                    contentType = "text/html; charset=utf-8",
                                    body = html,
                                )
                            } else {
                                val errorHtml = generateErrorHtml("配对凭证无效或已失效，请在手表上重新打开扫码页面。")
                                sendResponse(
                                    output,
                                    statusCode = 403,
                                    statusText = "Forbidden",
                                    contentType = "text/html; charset=utf-8",
                                    body = errorHtml,
                                )
                            }
                        } else {
                            sendResponse(
                                output,
                                statusCode = 404,
                                statusText = "Not Found",
                                contentType = "text/plain",
                                body = "Not Found",
                            )
                        }
                    }

                    "POST" -> {
                        if (path == "/api/config") {
                            val currentToken = pairingToken
                            if (currentToken != null && tokenFromRequest == currentToken && !isTokenUsed.get()) {
                                // 一次性 Token 校验成功，立即失效防止重放
                                isTokenUsed.set(true)
                                pairingToken = null

                                // 读取 Body
                                val body = if (contentLength > 0) {
                                    val chars = CharArray(contentLength)
                                    var read = 0
                                    while (read < contentLength) {
                                        val count = reader.read(chars, read, contentLength - read)
                                        if (count <= 0) break
                                        read += count
                                    }
                                    String(chars, 0, read)
                                } else ""

                                val payload = try {
                                    gson.fromJson(body, AiConfigPayload::class.java)
                                } catch (_: Throwable) {
                                    null
                                }

                                if (payload != null && payload.apiKey.isNotBlank()) {
                                    sendResponse(
                                        output,
                                        statusCode = 200,
                                        statusText = "OK",
                                        contentType = "application/json; charset=utf-8",
                                        body = """{"status":"success","message":"配置已同步至手表"}""",
                                    )
                                    // 派发配置
                                    onConfigReceived(payload)
                                    // 配置完成，延迟极短时间确保响应发送后自动销毁服务
                                    Thread {
                                        try {
                                            Thread.sleep(800)
                                        } catch (_: Throwable) {
                                        }
                                        stop()
                                    }.start()
                                } else {
                                    sendResponse(
                                        output,
                                        statusCode = 400,
                                        statusText = "Bad Request",
                                        contentType = "application/json; charset=utf-8",
                                        body = """{"status":"error","message":"API Key 不能为空"}""",
                                    )
                                }
                            } else {
                                sendResponse(
                                    output,
                                    statusCode = 403,
                                    statusText = "Forbidden",
                                    contentType = "application/json; charset=utf-8",
                                    body = """{"status":"error","message":"配对凭证无效或已失效"}""",
                                )
                            }
                        } else {
                            sendResponse(
                                output,
                                statusCode = 404,
                                statusText = "Not Found",
                                contentType = "text/plain",
                                body = "Not Found",
                            )
                        }
                    }

                    else -> {
                        sendResponse(
                            output,
                            statusCode = 405,
                            statusText = "Method Not Allowed",
                            contentType = "text/plain",
                            body = "Method Not Allowed",
                        )
                    }
                }
            } catch (_: Throwable) {
            } finally {
                try {
                    client.close()
                } catch (_: Throwable) {
                }
            }
        }, "BoompalaClientHandler").apply {
            isDaemon = true
            start()
        }
    }

    private fun parseQuery(query: String): Map<String, String> {
        if (query.isBlank()) return emptyMap()
        val result = mutableMapOf<String, String>()
        for (pair in query.split("&")) {
            val parts = pair.split("=", limit = 2)
            if (parts.isNotEmpty()) {
                val key = URLDecoder.decode(parts[0], "UTF-8")
                val value = if (parts.size > 1) URLDecoder.decode(parts[1], "UTF-8") else ""
                result[key] = value
            }
        }
        return result
    }

    private fun sendResponse(
        output: OutputStream,
        statusCode: Int,
        statusText: String,
        contentType: String,
        body: String,
    ) {
        val bodyBytes = body.toByteArray(Charsets.UTF_8)
        val header = "HTTP/1.1 $statusCode $statusText\r\n" +
            "Content-Type: $contentType\r\n" +
            "Content-Length: ${bodyBytes.size}\r\n" +
            "Access-Control-Allow-Origin: *\r\n" +
            "Access-Control-Allow-Methods: GET, POST, OPTIONS\r\n" +
            "Access-Control-Allow-Headers: Content-Type\r\n" +
            "Connection: close\r\n\r\n"
        output.write(header.toByteArray(Charsets.UTF_8))
        if (bodyBytes.isNotEmpty()) {
            output.write(bodyBytes)
        }
        output.flush()
    }

    private fun generateHtmlPage(token: String): String {
        return """
<!DOCTYPE html>
<html lang="zh-CN">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
  <title>Boompala · 手表 SI 配置同步</title>
  <style>
    * { box-sizing: border-box; margin: 0; padding: 0; }
    body {
      background-color: #121212;
      color: #ECEFF1;
      font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "PingFang SC", sans-serif;
      padding: 20px 16px;
      display: flex;
      flex-direction: column;
      align-items: center;
      min-height: 100vh;
    }
    .container {
      width: 100%;
      max-width: 420px;
      background: #1E1E1E;
      border: 1px solid #333333;
      border-radius: 16px;
      padding: 24px 20px;
      box-shadow: 0 8px 24px rgba(0,0,0,0.5);
    }
    .header { text-align: center; margin-bottom: 24px; }
    .title { font-size: 20px; font-weight: 700; color: #4FC3F7; }
    .subtitle { font-size: 13px; color: #90A4AE; margin-top: 6px; }
    .form-group { margin-bottom: 18px; }
    label { display: block; font-size: 13px; font-weight: 600; color: #CFD8DC; margin-bottom: 6px; }
    select, input {
      width: 100%;
      background: #2A2A2A;
      border: 1px solid #444444;
      border-radius: 10px;
      color: #FFFFFF;
      padding: 12px 14px;
      font-size: 14px;
      outline: none;
      transition: border-color 0.2s;
    }
    select:focus, input:focus { border-color: #4FC3F7; }
    .hint { font-size: 11px; color: #78909C; margin-top: 4px; }
    .btn-submit {
      width: 100%;
      background: linear-gradient(135deg, #0288D1, #00B0FF);
      color: #FFFFFF;
      border: none;
      border-radius: 10px;
      padding: 14px;
      font-size: 15px;
      font-weight: 600;
      cursor: pointer;
      margin-top: 10px;
      transition: opacity 0.2s;
    }
    .btn-submit:hover { opacity: 0.9; }
    .btn-submit:disabled { background: #555555; cursor: not-allowed; }
    .result-banner {
      display: none;
      margin-top: 18px;
      padding: 14px;
      border-radius: 10px;
      font-size: 14px;
      text-align: center;
      line-height: 1.5;
    }
    .success { background: #1B5E20; color: #C8E6C9; border: 1px solid #2E7D32; }
    .error { background: #B71C1C; color: #FFCDD2; border: 1px solid #C62828; }
  </style>
</head>
<body>
  <div class="container">
    <div class="header">
      <div class="title">🔮 Boompala 易盘</div>
      <div class="subtitle">局域网安全直连 · 快捷配置 API Key</div>
    </div>

    <form id="configForm">
      <div class="form-group">
        <label for="provider">SI 服务商</label>
        <select id="provider">
          <option value="deepseek" selected>DeepSeek (深度求索 - 推荐)</option>
          <option value="openai">OpenAI</option>
          <option value="moonshot">Moonshot (Kimi)</option>
          <option value="custom">自定义 (Custom)</option>
        </select>
      </div>

      <div class="form-group">
        <label for="apiKey">API Key (秘钥)</label>
        <input type="password" id="apiKey" placeholder="请输入或直接粘贴 sk-..." required autocomplete="off" />
        <div class="hint">秘钥经局域网直接写入手表，不经过第三方中转</div>
      </div>

      <div class="form-group">
        <label for="baseUrl">接口地址 (Base URL)</label>
        <input type="text" id="baseUrl" value="https://api.deepseek.com/v1" />
      </div>

      <div class="form-group">
        <label for="model">模型名称 (Model)</label>
        <input type="text" id="model" value="deepseek-chat" />
      </div>

      <button type="submit" id="submitBtn" class="btn-submit">✨ 同步配置至手表</button>
    </form>

    <div id="resultBanner" class="result-banner"></div>
  </div>

  <script>
    const providerDefaults = {
      deepseek: { baseUrl: 'https://api.deepseek.com/v1', model: 'deepseek-chat' },
      openai: { baseUrl: 'https://api.openai.com/v1', model: 'gpt-4o-mini' },
      moonshot: { baseUrl: 'https://api.moonshot.cn/v1', model: 'moonshot-v1-8k' },
      custom: { baseUrl: '', model: '' }
    };

    const providerSelect = document.getElementById('provider');
    const baseUrlInput = document.getElementById('baseUrl');
    const modelInput = document.getElementById('model');
    const apiKeyInput = document.getElementById('apiKey');
    const form = document.getElementById('configForm');
    const submitBtn = document.getElementById('submitBtn');
    const resultBanner = document.getElementById('resultBanner');

    providerSelect.addEventListener('change', (e) => {
      const val = e.target.value;
      const def = providerDefaults[val];
      if (def) {
        baseUrlInput.value = def.baseUrl;
        modelInput.value = def.model;
      }
    });

    form.addEventListener('submit', async (e) => {
      e.preventDefault();
      const apiKey = apiKeyInput.value.trim();
      if (!apiKey) {
        showError('请填写 API Key');
        return;
      }

      submitBtn.disabled = true;
      submitBtn.innerText = '正在同步至手表...';
      resultBanner.style.display = 'none';

      const payload = {
        providerId: providerSelect.value,
        apiKey: apiKey,
        customBaseUrl: baseUrlInput.value.trim(),
        customModel: modelInput.value.trim()
      };

      try {
        const resp = await fetch('/api/config?token=' + encodeURIComponent('$token'), {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(payload)
        });
        const data = await resp.json();
        if (resp.ok && data.status === 'success') {
          showSuccess('🎉 配置已成功同步到手表！<br>手表已振动确认，您现在可以关闭此网页。');
          form.style.display = 'none';
        } else {
          showError(data.message || '同步失败，请检查手表状态');
          submitBtn.disabled = false;
          submitBtn.innerText = '✨ 重新同步';
        }
      } catch (err) {
        showError('连接失败：请确认手机与手表处于同一 Wi-Fi');
        submitBtn.disabled = false;
        submitBtn.innerText = '✨ 重新同步';
      }
    });

    function showSuccess(msg) {
      resultBanner.className = 'result-banner success';
      resultBanner.innerHTML = msg;
      resultBanner.style.display = 'block';
    }

    function showError(msg) {
      resultBanner.className = 'result-banner error';
      resultBanner.innerHTML = msg;
      resultBanner.style.display = 'block';
    }
  </script>
</body>
</html>
        """.trimIndent()
    }

    private fun generateErrorHtml(message: String): String {
        return """
<!DOCTYPE html>
<html lang="zh-CN">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Boompala · 凭证失效</title>
  <style>
    body { background: #121212; color: #ECEFF1; font-family: sans-serif; display: flex; align-items: center; justify-content: center; min-height: 100vh; padding: 20px; }
    .card { background: #1E1E1E; border: 1px solid #444; border-radius: 14px; padding: 24px; text-align: center; max-width: 380px; }
    h2 { color: #FF5252; margin-bottom: 12px; }
    p { color: #B0BEC5; font-size: 14px; line-height: 1.6; }
  </style>
</head>
<body>
  <div class="card">
    <h2>⚠️ 配对失效</h2>
    <p>$message</p>
  </div>
</body>
</html>
        """.trimIndent()
    }
}
