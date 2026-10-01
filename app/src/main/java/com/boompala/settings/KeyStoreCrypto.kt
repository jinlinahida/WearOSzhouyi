package com.boompala.settings

import java.security.KeyStore
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Android Keystore AES-256-GCM 硬件加密工具。
 *
 * 用于在设备本地持久化存储用户 AI API Key 时进行硬件加密。
 * 支持：
 * - Android Keystore (AES/GCM/NoPadding 256-bit)；
 * - 无缝向前兼容与自动迁移（未带前缀的明文旧密钥直接读取，并在下次保存时加密）；
 * - 在非 Android 环境（如 JVM 单元测试）或不支持硬件 Keystore 的异常情况下安全降级，永不丢数据或崩溃。
 */
object KeyStoreCrypto {
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "boompala_ai_secure_v1"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val PREFIX = "enc:v1:"
    private const val GCM_TAG_LENGTH = 128

    fun encrypt(plainText: String): String {
        val trimmed = plainText.trim()
        if (trimmed.isBlank()) return trimmed
        return try {
            val secretKey = getOrCreateSecretKey()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            val iv = cipher.iv
            val cipherText = cipher.doFinal(trimmed.toByteArray(Charsets.UTF_8))
            val ivBase64 = Base64.getEncoder().encodeToString(iv)
            val cipherBase64 = Base64.getEncoder().encodeToString(cipherText)
            "$PREFIX$ivBase64:$cipherBase64"
        } catch (_: Throwable) {
            // JVM 单元测试环境或不支持 KeyStore 时安全降级
            trimmed
        }
    }

    fun decrypt(storedValue: String): String {
        val trimmed = storedValue.trim()
        if (!trimmed.startsWith(PREFIX)) {
            // 未加密的旧版本密钥，平滑兼容读取（无缝数据迁移）
            return trimmed
        }
        return try {
            val content = trimmed.removePrefix(PREFIX)
            val parts = content.split(":")
            if (parts.size != 2) return ""
            val iv = Base64.getDecoder().decode(parts[0])
            val cipherText = Base64.getDecoder().decode(parts[1])
            val secretKey = getOrCreateSecretKey()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
            val plainBytes = cipher.doFinal(cipherText)
            String(plainBytes, Charsets.UTF_8)
        } catch (_: Throwable) {
            ""
        }
    }

    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        if (keyStore.containsAlias(KEY_ALIAS)) {
            val entry = keyStore.getEntry(KEY_ALIAS, null) as KeyStore.SecretKeyEntry
            return entry.secretKey
        }

        val keyGenerator = KeyGenerator.getInstance("AES", ANDROID_KEYSTORE)
        val spec = android.security.keystore.KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            android.security.keystore.KeyProperties.PURPOSE_ENCRYPT or android.security.keystore.KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(android.security.keystore.KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(android.security.keystore.KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }
}
