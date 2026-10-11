package com.momentlink.api.auth.application.dto

import com.momentlink.domain.user.LoginProvider

data class OAuthUserProfile(
    val provider: LoginProvider,
    val providerUserId: String,
    val email: String,
)