package com.momentlink.api.storage.infra.google

import com.fasterxml.jackson.annotation.JsonProperty
import com.momentlink.api.auth.application.dto.GoogleUserRes
import com.momentlink.api.auth.common.BusinessException
import com.momentlink.api.common.web.ErrorCode
import com.momentlink.api.storage.application.StorageProperties
import com.momentlink.domain.media.TransferFailureType
import org.springframework.http.MediaType
import org.springframework.http.client.JdkClientHttpRequestFactory
import org.springframework.stereotype.Component
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientResponseException
import org.springframework.web.util.UriComponentsBuilder
import java.net.http.HttpClient
import java.time.Duration

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

    // 토큰 서버가 응답하지 않아도 워커 스레드가 멈추지 않게 한다.
    private val restClient = restClientBuilder
        .requestFactory(
            JdkClientHttpRequestFactory(HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build())
                .apply { setReadTimeout(READ_TIMEOUT) }
        )
        .build()

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
        } catch (ex: ResourceAccessException) {
            throw BusinessException(ErrorCode.STORAGE_CONNECT_FAILED, reason = "Google token 통신 실패: ${ex.message}")
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
        } catch (ex: ResourceAccessException) {
            throw BusinessException(ErrorCode.STORAGE_CONNECT_FAILED, reason = "Google userinfo 통신 실패: ${ex.message}")
        } ?: throw BusinessException(ErrorCode.STORAGE_CONNECT_FAILED, reason = "userinfo 응답 없음")
        return DriveGrant(refreshToken, user.sub, user.email)
    }

    fun refreshAccessToken(refreshToken: String): String =
        try {
            requestToken(form("refresh_token").apply { add("refresh_token", refreshToken) }).accessToken
        } catch (ex: RestClientResponseException) {
            throw classifyRefreshFailure(ex)
        } catch (ex: ResourceAccessException) {
            throw DriveTransferException(TransferFailureType.NETWORK, retryable = true, "저장소 토큰 갱신 통신 실패")
        }

    // 토큰이 이미 무효(400 invalid_token)면 폐기된 것으로 본다.
    fun revoke(refreshToken: String) {
        try {
            restClient.post()
                .uri(drive.revokeUri)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(LinkedMultiValueMap<String, String>().apply { add("token", refreshToken) })
                .retrieve()
                .toBodilessEntity()
        } catch (ex: RestClientResponseException) {
            if (ex.statusCode.value() != BAD_REQUEST) throw ex
        }
    }

    // 재인증이 필요한 건 invalid_grant(권한 회수·토큰 만료)뿐이다. 일시 오류로 저장소를 재인증 상태로 바꾸지 않는다.
    private fun classifyRefreshFailure(ex: RestClientResponseException): DriveTransferException {
        val status = ex.statusCode.value()
        val (type, retryable) = when {
            status == BAD_REQUEST && ex.responseBodyAsString.contains(INVALID_GRANT) -> TransferFailureType.AUTHENTICATION to true
            status == TOO_MANY_REQUESTS -> TransferFailureType.RATE_LIMITED to true
            status >= SERVER_ERROR -> TransferFailureType.PROVIDER_SERVER_ERROR to true
            else -> TransferFailureType.UNKNOWN to false
        }
        return DriveTransferException(type, retryable, "저장소 토큰 갱신 실패 $status")
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
        private const val INVALID_GRANT = "invalid_grant"
        private const val BAD_REQUEST = 400
        private const val TOO_MANY_REQUESTS = 429
        private const val SERVER_ERROR = 500
        private val CONNECT_TIMEOUT = Duration.ofSeconds(5)
        private val READ_TIMEOUT = Duration.ofSeconds(15)
    }
}
