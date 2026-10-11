package com.momentlink.api.auth.application

import com.momentlink.api.auth.common.security.InvalidJwtTokenException
import com.momentlink.api.auth.common.security.JwtProperties
import com.momentlink.api.auth.common.security.JwtTokenType
import io.jsonwebtoken.Claims
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.Instant
import java.util.Date
import java.util.UUID
import javax.crypto.SecretKey

@Component
class JwtProvider(
    private val properties: JwtProperties,
) {
    private val signingKey: SecretKey =
        Keys.hmacShaKeyFor(Decoders.BASE64.decode(properties.secret))

    private val jwtParser =
        Jwts.parser()
            .verifyWith(signingKey)
            .requireIssuer(properties.issuer)
            .build()

    fun createAccessToken(userId: Long): TokenDto =
        createToken(
            userId = userId,
            tokenType = JwtTokenType.ACCESS,
            expiration = properties.accessTokenExpiration,
        )

    fun createRefreshToken(userId: Long): TokenDto =
        createToken(
            userId = userId,
            tokenType = JwtTokenType.REFRESH,
            expiration = properties.refreshTokenExpiration,
        )

    fun extractUserId(token: String, expectedType: JwtTokenType): Long {
        val claims = parseClaims(token, expectedType)

        return claims.subject?.toLongOrNull()
            ?: throw InvalidJwtTokenException("JWT subject에 올바른 userId가 없습니다.")
    }

    fun validateAccessToken(token: String): Boolean =
        validateToken(token, JwtTokenType.ACCESS)

    fun validateRefreshToken(token: String): Boolean =
        validateToken(token, JwtTokenType.REFRESH)

    fun validateToken(token: String, expectedType: JwtTokenType): Boolean =
        try {
            extractUserId(token, expectedType)
            true
        } catch (_: InvalidJwtTokenException) {
            false
        }

    private fun createToken(userId: Long, tokenType: JwtTokenType, expiration: Duration): TokenDto {
        require(userId > 0) { "userId는 0보다 커야 합니다." }

        val issuedAt = Instant.now()
        val expiresAt = issuedAt.plus(expiration)

        val token = Jwts.builder()
            .issuer(properties.issuer)
            .subject(userId.toString())
            .issuedAt(Date.from(issuedAt))
            .expiration(Date.from(expiresAt))
            .id(UUID.randomUUID().toString())
            .claim("token_type", tokenType.name)
            .signWith(signingKey, Jwts.SIG.HS256)
            .compact()

        return TokenDto.create(token, expiresAt)
    }

    private fun parseClaims(token: String, expectedType: JwtTokenType): Claims {
        if (token.isBlank()) {
            throw InvalidJwtTokenException("JWT가 비어 있습니다.")
        }

        val signedClaims = try {
            jwtParser.parseSignedClaims(token)
        } catch (exception: JwtException) {
            throw InvalidJwtTokenException(
                message = "유효하지 않은 JWT입니다.",
                cause = exception,
            )
        } catch (exception: IllegalArgumentException) {
            throw InvalidJwtTokenException(
                message = "JWT 형식이 올바르지 않습니다.",
                cause = exception,
            )
        }

        if (signedClaims.header.algorithm != Jwts.SIG.HS256.id) {
            throw InvalidJwtTokenException("허용되지 않은 JWT 서명 알고리즘입니다.",)
        }

        val claims = signedClaims.payload
        val actualType =
            claims.get("token_type", String::class.java)

        if (actualType != expectedType.name) {
            throw InvalidJwtTokenException("올바르지 않은 JWT 종류입니다.",)
        }

        return claims
    }

}

data class TokenDto(
    val token: String,
    val expiresAt: Instant,
) {
    companion object {
        fun create(token:String, expiresAt: Instant): TokenDto
            = TokenDto(token, expiresAt)
    }
}

