package com.momentlink.api.common.security.key

import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

@Component
class KeyGenerator(
    private val keyHasher: KeyHasher,
) {

    private val secureRandom = SecureRandom()
    private val encoder = Base64.getUrlEncoder().withoutPadding()

    fun generate(): IssuedKey {
        val randomBytes = ByteArray(32)
        secureRandom.nextBytes(randomBytes)

        val rawKey = encoder.encodeToString(randomBytes)
        val hashedKey = keyHasher.hash(rawKey)

        return IssuedKey(
            rawKey = rawKey,
            hashedKey = hashedKey,
        )
    }

    fun generateState(): String {
        val randomBytes = ByteArray(32)
        secureRandom.nextBytes(randomBytes)

        return encoder.encodeToString(randomBytes)
    }
}