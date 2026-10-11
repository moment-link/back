package com.momentlink.api.auth.application.dto

import java.time.Instant

data class LoginResResult(
    val refreshToken: String,
    val refreshTokenExpiresIn: Instant,
) {
    companion object {
        fun from(refreshToken: String, refreshTokenExpiresIn: Instant): LoginResResult {
            return LoginResResult(refreshToken, refreshTokenExpiresIn)
        }
    }
}
