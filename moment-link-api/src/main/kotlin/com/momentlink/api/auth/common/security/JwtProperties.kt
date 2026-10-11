package com.momentlink.api.auth.common.security

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "auth.jwt")
data class JwtProperties(
    val issuer: String,
    val secret: String,
    val accessTokenExpiration: Duration,
    val refreshTokenExpiration: Duration,
)