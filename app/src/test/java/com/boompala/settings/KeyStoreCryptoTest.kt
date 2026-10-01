package com.boompala.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KeyStoreCryptoTest {

    @Test
    fun `legacy unencrypted api key is returned as-is for seamless migration`() {
        val legacyKey = "sk-deepseek-1234567890abcdef"
        val decrypted = KeyStoreCrypto.decrypt(legacyKey)
        assertEquals("Legacy plaintext key must be returned without alteration", legacyKey, decrypted)
    }

    @Test
    fun `blank or whitespace keys are handled gracefully`() {
        assertEquals("", KeyStoreCrypto.encrypt(""))
        assertEquals("", KeyStoreCrypto.encrypt("   "))
        assertEquals("", KeyStoreCrypto.decrypt(""))
        assertEquals("", KeyStoreCrypto.decrypt("   "))
    }

    @Test
    fun `malformed encrypted prefix fails safely without crashing`() {
        val corruptEncrypted1 = "enc:v1:corrupt_base64"
        assertEquals("", KeyStoreCrypto.decrypt(corruptEncrypted1))

        val corruptEncrypted2 = "enc:v1:invalid_iv:invalid_cipher"
        assertEquals("", KeyStoreCrypto.decrypt(corruptEncrypted2))
    }

    @Test
    fun `fallback behavior in jvm tests preserves key safely`() {
        val testKey = "sk-test-key-for-jvm-fallback"
        // In JVM unit tests where AndroidKeyStore is not mocked/available,
        // encrypt safely returns the trimmed plain key or encrypted string without throwing
        val encryptedOrFallback = KeyStoreCrypto.encrypt(testKey)
        assertFalse(encryptedOrFallback.isBlank())

        // Decrypting must restore the key
        val decrypted = KeyStoreCrypto.decrypt(encryptedOrFallback)
        assertEquals(testKey, decrypted)
    }
}
