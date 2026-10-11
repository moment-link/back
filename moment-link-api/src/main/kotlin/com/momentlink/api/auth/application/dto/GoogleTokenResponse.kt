package com.momentlink.api.auth.application.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class GoogleTokenRes(
    @JsonProperty("access_token")
    val accessToken: String,
)

data class GoogleUserRes(
    val sub: String,
    val email: String,
    @JsonProperty("email_verified")
    val emailVerified: Boolean,
)