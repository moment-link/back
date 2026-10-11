package com.momentlink.api.auth.application.login

import com.momentlink.api.auth.application.dto.OAuthUserProfile
import com.momentlink.domain.user.LoginProvider

interface OAuthLoginClient {

    val provider: LoginProvider

    fun createAuthorizationUrl(state: String): String

    fun getUserProfile(code:String): OAuthUserProfile
}