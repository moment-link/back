package com.momentlink.api.auth.dto

import com.momentlink.domain.user.LoginProvider

data class OAuthCallbackReqDto (
    val provider: LoginProvider,
    val code: String,
)

data class RefreshReqDto (
    val refreshToken: String,
)