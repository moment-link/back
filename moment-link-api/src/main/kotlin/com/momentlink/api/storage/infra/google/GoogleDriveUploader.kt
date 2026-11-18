package com.momentlink.api.storage.infra.google

import com.momentlink.domain.media.TransferFailureType
import org.springframework.http.MediaType
import org.springframework.http.StreamingHttpOutputMessage
import org.springframework.http.client.JdkClientHttpRequestFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientResponseException
import java.io.InputStream
import java.net.http.HttpTimeoutException
import java.time.Duration

data class DriveFileRes(
    val id: String,
)

// resumable 업로드로 S3 스트림을 메모리에 올리지 않고 흘려보낸다. (영상 최대 200 MiB)
@Component
class GoogleDriveUploader(
    restClientBuilder: RestClient.Builder,
) {
    private val restClient = restClientBuilder
        .requestFactory(JdkClientHttpRequestFactory().apply { setReadTimeout(UPLOAD_TIMEOUT) })
        .build()

    fun createFolder(accessToken: String, name: String): String =
        driveCall {
            restClient.post()
                .uri(FILES_URI)
                .headers { it.setBearerAuth(accessToken) }
                .contentType(MediaType.APPLICATION_JSON)
                .body(mapOf("name" to name, "mimeType" to FOLDER_MIME_TYPE))
                .retrieve()
                .body(DriveFileRes::class.java)
        }.id

    fun upload(
        accessToken: String,
        folderId: String,
        fileName: String,
        mimeType: String,
        sizeBytes: Long,
        content: InputStream,
    ): String {
        val sessionUri = driveCall {
            restClient.post()
                .uri("$UPLOAD_URI?uploadType=resumable")
                .headers {
                    it.setBearerAuth(accessToken)
                    it.set("X-Upload-Content-Type", mimeType)
                    it.set("X-Upload-Content-Length", sizeBytes.toString())
                }
                .contentType(MediaType.APPLICATION_JSON)
                .body(mapOf("name" to fileName, "parents" to listOf(folderId)))
                .retrieve()
                .toBodilessEntity()
                .headers.location
        }

        return driveCall {
            restClient.put()
                .uri(sessionUri)
                .contentType(MediaType.parseMediaType(mimeType))
                .contentLength(sizeBytes)
                .body(StreamingHttpOutputMessage.Body { output -> content.transferTo(output) })
                .retrieve()
                .body(DriveFileRes::class.java)
        }.id
    }

    private fun <T : Any> driveCall(call: () -> T?): T =
        try {
            call() ?: throw DriveTransferException(TransferFailureType.UNKNOWN, retryable = true, "Drive 응답 없음")
        } catch (ex: RestClientResponseException) {
            throw classify(ex)
        } catch (ex: ResourceAccessException) {
            val type = if (ex.cause is HttpTimeoutException) TransferFailureType.TIMEOUT else TransferFailureType.NETWORK
            throw DriveTransferException(type, retryable = true, "Drive 통신 실패")
        }

    private fun classify(ex: RestClientResponseException): DriveTransferException {
        val status = ex.statusCode.value()
        val isRateLimited = status == TOO_MANY_REQUESTS ||
            (status == FORBIDDEN && RATE_LIMIT_REASONS.any { ex.responseBodyAsString.contains(it) })
        val (type, retryable) = when {
            status == UNAUTHORIZED -> TransferFailureType.AUTHENTICATION to true
            isRateLimited -> TransferFailureType.RATE_LIMITED to true
            status == FORBIDDEN -> TransferFailureType.PERMISSION to false
            status == NOT_FOUND -> TransferFailureType.INVALID_DESTINATION to true
            status >= SERVER_ERROR -> TransferFailureType.PROVIDER_SERVER_ERROR to true
            else -> TransferFailureType.UNKNOWN to false
        }
        return DriveTransferException(type, retryable, "Drive API $status")
    }

    companion object {
        private const val FILES_URI = "https://www.googleapis.com/drive/v3/files"
        private const val UPLOAD_URI = "https://www.googleapis.com/upload/drive/v3/files"
        private const val FOLDER_MIME_TYPE = "application/vnd.google-apps.folder"
        private const val UNAUTHORIZED = 401
        private const val FORBIDDEN = 403
        private const val NOT_FOUND = 404
        private const val TOO_MANY_REQUESTS = 429
        private const val SERVER_ERROR = 500
        private val RATE_LIMIT_REASONS = listOf("rateLimitExceeded", "userRateLimitExceeded")
        private val UPLOAD_TIMEOUT = Duration.ofMinutes(10)
    }
}
