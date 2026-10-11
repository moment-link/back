package com.momentlink.api.common.web

import org.springframework.http.ResponseCookie
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.Instant

@Component
class TokenCookieProvider {

    fun create(type: TokenCookie, token: String, expiresAt: Instant, eventId: Long? = null): ResponseCookie {
        val maxAge = Duration.between(Instant.now(), expiresAt).coerceAtLeast(Duration.ZERO)
        return build(type, token, maxAge, eventId)
    }

    fun expire(type: TokenCookie, eventId: Long? = null): ResponseCookie =
        build(type, "", Duration.ZERO, eventId)

    private fun build(type: TokenCookie, token: String, maxAge: Duration, eventId: Long?): ResponseCookie =
        ResponseCookie.from(type.cookieName, token)
            .httpOnly(true)
            .secure(true)
            .sameSite("Lax")
            .path(type.path(eventId))
            .maxAge(maxAge)
            .build()
}
