package com.momentlink.api.common.web

import java.time.Duration
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TokenCookieProviderTest {
    private val provider = TokenCookieProvider()
    private val cookieTypes = listOf(
        Triple(TokenCookie.REFRESH_TOKEN, "refresh_token", "/api/v1/auth"),
        Triple(TokenCookie.PARTICIPANT_TOKEN, "participant_token", "/api/v1/participant/42"),
    )

    @Test
    fun `create scopes each token cookie to its endpoint with secure attributes`() {
        for ((type, expectedName, expectedPath) in cookieTypes) {
            val cookie = provider.create(type, "token-value", Instant.now().plusSeconds(3600), eventId = 42)

            assertEquals(expectedName, cookie.name)
            assertEquals(expectedPath, cookie.path)
            assertEquals("token-value", cookie.value)
            assertTrue(cookie.isHttpOnly)
            assertTrue(cookie.isSecure)
            assertEquals("Lax", cookie.sameSite)
        }
    }

    @Test
    fun `create keeps future tokens alive for their remaining lifetime`() {
        for ((type) in cookieTypes) {
            val before = Instant.now()
            val expiresAt = before.plusSeconds(3600)

            val cookie = provider.create(type, "token-value", expiresAt, eventId = 42)

            val after = Instant.now()
            val minimumSeconds = Duration.between(after, expiresAt).seconds
            val maximumSeconds = Duration.between(before, expiresAt).seconds
            assertTrue(cookie.maxAge.seconds > 0, "$type must not expire a future token")
            assertTrue(
                cookie.maxAge.seconds in minimumSeconds..maximumSeconds,
                "$type Max-Age must reflect the token's remaining lifetime",
            )
        }
    }

    @Test
    fun `create immediately expires tokens whose expiry is in the past`() {
        for ((type) in cookieTypes) {
            val cookie = provider.create(type, "token-value", Instant.now().minusSeconds(60), eventId = 42)

            assertEquals(Duration.ZERO, cookie.maxAge, "$type must not create a session cookie")
        }
    }

    @Test
    fun `expire clears the issued cookie at the same name domain and path`() {
        for ((type) in cookieTypes) {
            val issued = provider.create(type, "token-value", Instant.now().plusSeconds(3600), eventId = 42)

            val expired = provider.expire(type, eventId = 42)

            assertEquals(issued.name, expired.name)
            assertEquals(issued.domain, expired.domain)
            assertEquals(issued.path, expired.path)
            assertEquals("", expired.value)
            assertEquals(Duration.ZERO, expired.maxAge)
            assertTrue(expired.isHttpOnly)
            assertTrue(expired.isSecure)
            assertEquals("Lax", expired.sameSite)
        }
    }
}
