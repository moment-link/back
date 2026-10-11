package com.momentlink.api.auth.application.dto

import java.time.Instant

data class RefreshResResult(
    val accessToken: String,
    val accessTokenExpiresIn: Instant,
    val refreshToken: String,
    val refreshTokenExpiresIn: Instant,
) {
    companion object {
        fun from(accessToken: String, accessTokenExpiresIn: Instant, refreshToken: String, refreshTokenExpiresIn: Instant): RefreshResResult {
            return RefreshResResult(accessToken, accessTokenExpiresIn, refreshToken, refreshTokenExpiresIn)
        }
    }
}