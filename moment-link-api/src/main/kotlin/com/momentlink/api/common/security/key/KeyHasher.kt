package com.momentlink.api.common.security.key

import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Base64

@Component
class KeyHasher {

    private val encoder = Base64.getUrlEncoder().withoutPadding()

    fun hash(rawKey: String): String {
        val digest = MessageDigest
            .getInstance("SHA-256")
            .digest(rawKey.toByteArray(StandardCharsets.UTF_8))

        return encoder.encodeToString(digest)
    }
}