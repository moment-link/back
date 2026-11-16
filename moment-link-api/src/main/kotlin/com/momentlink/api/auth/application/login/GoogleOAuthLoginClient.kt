package com.momentlink.api.auth.application.login

import com.momentlink.api.auth.application.dto.GoogleTokenRes
import com.momentlink.api.auth.application.dto.GoogleUserRes
import com.momentlink.api.auth.application.dto.OAuthUserProfile
import com.momentlink.api.auth.application.provider.GoogleOAuthProperties
import com.momentlink.api.auth.common.BusinessException
import com.momentlink.api.common.web.ErrorCode
import com.momentlink.domain.user.LoginProvider
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestClient
import org.springframework.web.util.UriComponentsBuilder

@Component
class GoogleOAuthLoginClient(
    private val properties: GoogleOAuthProperties,
    restClientBuilder: RestClient.Builder
): OAuthLoginClient {

    private val restClient = restClientBuilder.build()

    override val provider: LoginProvider
        = LoginProvider.GOOGLE

    override fun createAuthorizationUrl(state: String): String =
        UriComponentsBuilder
            .fromUriString(properties.authorizationUri)
            .queryParam("client_id", properties.clientId)
            .queryParam("redirect_uri", properties.redirectUri)
            .queryParam("response_type", "code")
            .queryParam("scope", "openid email profile")
            .queryParam("state", state)
            .build().encode().toUriString()

    override fun getUserProfile(code: String): OAuthUserProfile {
        val accessToken = exchangeCode(code)

        val googleUser = rejectedByGoogle("userinfo") {
            restClient.get()
                .uri(properties.userInfoUri)
                .headers { it.setBearerAuth(accessToken) }
                .retrieve()
                .body(GoogleUserRes::class.java)
        } ?: throw BusinessException(ErrorCode.GOOGLE_USER_INFO_RESPONSE_INVALID)

        if (!googleUser.emailVerified) {
            throw BusinessException(ErrorCode.OAUTH_LOGIN_FAILED, reason = "인증되지 않은 이메일")
        }
        return OAuthUserProfile(
            provider = provider,
            providerUserId = googleUser.sub,
            email = googleUser.email,
        )
    }

    private fun exchangeCode(code: String):String {
        val form = LinkedMultiValueMap<String, String>().apply {
            add("grant_type", "authorization_code")
            add("client_id", properties.clientId)
            add("client_secret", properties.clientSecret)
            add("redirect_uri", properties.redirectUri)
            add("code", code)
        }

        return rejectedByGoogle("token") {
            restClient.post()
                .uri(properties.tokenUri)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(GoogleTokenRes::class.java)
        }?.accessToken
            ?: throw BusinessException(ErrorCode.GOOGLE_TOKEN_RESPONSE_INVALID)
    }

    // 만료·재사용된 code처럼 Google이 4xx로 거절한 요청은 서버 오류가 아니라 로그인 실패로 본다.
    private fun <T> rejectedByGoogle(endpoint: String, call: () -> T): T =
        try {
            call()
        } catch (ex: HttpClientErrorException) {
            throw BusinessException(
                ErrorCode.OAUTH_LOGIN_FAILED,
                reason = "Google $endpoint ${ex.statusCode.value()}",
            )
        }
}