package com.momentlink.api.auth.application.provider

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "auth.oauth.google")
data class GoogleOAuthProperties(
    val clientId: String,
    val clientSecret: String,
    val authorizationUri: String,
    val tokenUri: String,
    val userInfoUri: String,
    val redirectUri: String,
)
