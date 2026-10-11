package com.momentlink.api.common.security.key

import java.time.Instant

data class IssuedKey(
    val rawKey: String,
    val hashedKey: String,
) {
    companion object {
        fun from(rawKey: String, hashedKey: String): IssuedKey {
            return IssuedKey(rawKey, hashedKey)
        }
    }
}