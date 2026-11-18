package com.momentlink.api.storage.infra.google

import com.fasterxml.jackson.annotation.JsonProperty
import com.momentlink.api.auth.application.dto.GoogleUserRes
import com.momentlink.api.auth.common.BusinessException
import com.momentlink.api.common.web.ErrorCode
import com.momentlink.api.storage.application.StorageProperties
import com.momentlink.domain.media.TransferFailureType
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientResponseException
import org.springframework.web.util.UriComponentsBuilder

data class DriveGrant(
    val refreshToken: String,
    val accountId: String,
    val email: String,
)

data class DriveTokenRes(
    @JsonProperty("access_token") val accessToken: String,
    @JsonProperty("refresh_token") val refreshToken: String? = null,
    val scope: String? = null,
)

@Component
class GoogleDriveOAuthClient(
    properties: StorageProperties,
    restClientBuilder: RestClient.Builder,
) {
    private val drive = properties.googleDrive
    private val restClient = restClientBuilder.build()

    // refresh_token을 받으려면 offline + consent가 필요하다.
    fun createAuthorizationUrl(state: String): String =
        UriComponentsBuilder.fromUriString(drive.authorizationUri)
            .queryParam("client_id", drive.clientId)
            .queryParam("redirect_uri", drive.redirectUri)
            .queryParam("response_type", "code")
            .queryParam("scope", "openid email $DRIVE_FILE_SCOPE")
            .queryParam("access_type", "offline")
            .queryParam("prompt", "consent")
            .queryParam("state", state)
            .build().encode().toUriString()

    fun exchangeCode(code: String): DriveGrant {
        val token = try {
            requestToken(form("authorization_code").apply {
                add("code", code)
                add("redirect_uri", drive.redirectUri)
            })
        } catch (ex: RestClientResponseException) {
            throw BusinessException(ErrorCode.STORAGE_CONNECT_FAILED, reason = "Google token ${ex.statusCode.value()}")
        }
        // 세분화된 동의 화면에서 Drive 권한만 빼고 승인할 수 있다.
        if (DRIVE_FILE_SCOPE !in token.scope.orEmpty().split(" ")) {
            throw BusinessException(ErrorCode.STORAGE_CONNECT_FAILED, reason = "drive.file 권한 미승인")
        }
        val refreshToken = token.refreshToken
            ?: throw BusinessException(ErrorCode.STORAGE_CONNECT_FAILED, reason = "refresh_token 없음")

        val user = try {
            restClient.get()
                .uri(drive.userInfoUri)
                .headers { it.setBearerAuth(token.accessToken) }
                .retrieve()
                .body(GoogleUserRes::class.java)
        } catch (ex: RestClientResponseException) {
            throw BusinessException(ErrorCode.STORAGE_CONNECT_FAILED, reason = "Google userinfo ${ex.statusCode.value()}")
        } ?: throw BusinessException(ErrorCode.STORAGE_CONNECT_FAILED, reason = "userinfo 응답 없음")
        return DriveGrant(refreshToken, user.sub, user.email)
    }

    fun refreshAccessToken(refreshToken: String): String =
        try {
            requestToken(form("refresh_token").apply { add("refresh_token", refreshToken) }).accessToken
        } catch (ex: RestClientResponseException) {
            // invalid_grant: 사용자가 권한을 회수했거나 토큰이 만료됨
            throw DriveTransferException(TransferFailureType.AUTHENTICATION, retryable = true, "저장소 토큰 갱신 실패 ${ex.statusCode.value()}")
        }

    private fun requestToken(form: MultiValueMap<String, String>): DriveTokenRes =
        restClient.post()
            .uri(drive.tokenUri)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(form)
            .retrieve()
            .body(DriveTokenRes::class.java)
            ?: throw BusinessException(ErrorCode.STORAGE_CONNECT_FAILED, reason = "token 응답 없음")

    private fun form(grantType: String): LinkedMultiValueMap<String, String> =
        LinkedMultiValueMap<String, String>().apply {
            add("grant_type", grantType)
            add("client_id", drive.clientId)
            add("client_secret", drive.clientSecret)
        }

    companion object {
        private const val DRIVE_FILE_SCOPE = "https://www.googleapis.com/auth/drive.file"
    }
}
