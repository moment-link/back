package com.momentlink.api.auth.dto

import com.momentlink.api.auth.application.dto.RefreshResResult
import java.time.Instant


data class OAuthLoginResDto (
    val authorizationUrl: String,
) {
    companion object {
        fun from(authorizationUrl: String) =
            OAuthLoginResDto(authorizationUrl)
    }
}

data class RefreshResDto (
    val accessToken: String,
    val accessTokenExpiresIn: Instant,
) {
    companion object {
        fun from(result: RefreshResResult): RefreshResDto
        = RefreshResDto(result.accessToken, result.accessTokenExpiresIn)
    }
}