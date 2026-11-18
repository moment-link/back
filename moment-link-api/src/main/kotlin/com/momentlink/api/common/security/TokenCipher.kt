package com.momentlink.api.common.security

import com.momentlink.api.storage.application.StorageProperties
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

// 외부 저장소 refresh token을 AES-256-GCM으로 암호화한다. 결과는 Base64(iv || ciphertext).
@Component
class TokenCipher(
    properties: StorageProperties,
) {
    private val key = SecretKeySpec(decodeKey(properties.tokenEncryptionKey), "AES")
    private val random = SecureRandom()

    fun encrypt(plainText: String): String {
        val iv = ByteArray(IV_BYTES).also(random::nextBytes)
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(TAG_BITS, iv))
        }
        return Base64.getEncoder().encodeToString(iv + cipher.doFinal(plainText.toByteArray(StandardCharsets.UTF_8)))
    }

    fun decrypt(encrypted: String): String {
        val bytes = Base64.getDecoder().decode(encrypted)
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_BITS, bytes, 0, IV_BYTES))
        }
        return String(cipher.doFinal(bytes, IV_BYTES, bytes.size - IV_BYTES), StandardCharsets.UTF_8)
    }

    companion object {
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val IV_BYTES = 12
        private const val TAG_BITS = 128
        private const val KEY_BYTES = 32

        private fun decodeKey(base64Key: String): ByteArray =
            Base64.getDecoder().decode(base64Key).also {
                require(it.size == KEY_BYTES) { "STORAGE_TOKEN_ENCRYPTION_KEY는 32바이트를 Base64로 인코딩한 값이어야 합니다." }
            }
    }
}
