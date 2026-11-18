package com.momentlink.api.media.infra.s3

import com.momentlink.api.auth.common.BusinessException
import com.momentlink.api.common.web.ErrorCode
import com.momentlink.api.media.port.out.DownloadTarget
import com.momentlink.api.media.port.out.MediaObjectStorage
import com.momentlink.api.media.port.out.StoredObjectInfo
import com.momentlink.api.media.port.out.UploadTarget
import org.springframework.stereotype.Component
import software.amazon.awssdk.core.exception.SdkClientException
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.ChecksumMode
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.model.S3Exception
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import java.io.InputStream
import java.time.Duration

@Component
class S3MediaObjectStorage(
    private val s3Client: S3Client,
    private val presigner: S3Presigner,
    private val properties: S3Properties,
) : MediaObjectStorage {

    // 크기도 서명에 묶어 준비 때 알린 크기와 다른 파일은 S3가 받지 않게 한다.
    override fun presignUpload(objectKey: String, mimeType: String, sizeBytes: Long, checksum: String, validity: Duration): UploadTarget {
        val put = PutObjectRequest.builder()
            .bucket(properties.bucket)
            .key(objectKey)
            .contentType(mimeType)
            .contentLength(sizeBytes)
            .checksumSHA256(checksum)
            .build()
        val presigned = presigner.presignPutObject { it.signatureDuration(validity).putObjectRequest(put) }

        // 브라우저가 그대로 보내야 하는 서명 헤더. Host·Content-Length는 브라우저가 직접 붙인다.
        val headers = presigned.signedHeaders()
            .filterKeys { it.lowercase() !in BROWSER_MANAGED_HEADERS }
            .mapValues { (_, values) -> values.joinToString(",") }
        return UploadTarget(presigned.url().toString(), headers)
    }

    override fun presignDownload(objectKey: String, validity: Duration): DownloadTarget {
        val get = GetObjectRequest.builder()
            .bucket(properties.bucket)
            .key(objectKey)
            .build()
        val presigned = presigner.presignGetObject { it.signatureDuration(validity).getObjectRequest(get) }
        return DownloadTarget(presigned.url().toString(), presigned.expiration())
    }

    override fun head(objectKey: String): StoredObjectInfo =
        storageCall(objectKey) {
            val res = s3Client.headObject {
                it.bucket(properties.bucket).key(objectKey).checksumMode(ChecksumMode.ENABLED)
            }
            StoredObjectInfo(res.contentLength(), res.checksumSHA256(), res.eTag())
        }

    // 대용량 전송용 스트림. 기본 API 타임아웃(10초) 대신 긴 타임아웃을 쓴다.
    override fun open(objectKey: String): InputStream =
        storageCall(objectKey) {
            s3Client.getObject {
                it.bucket(properties.bucket)
                    .key(objectKey)
                    .overrideConfiguration { config -> config.apiCallTimeout(STREAM_TIMEOUT) }
            }
        }

    // 없는 키 삭제도 S3는 성공으로 응답한다.
    override fun delete(objectKey: String) {
        storageCall(objectKey) {
            s3Client.deleteObject { it.bucket(properties.bucket).key(objectKey) }
        }
    }

    // HEAD 404는 ListBucket 권한이 있어야 403이 아니라 404로 온다.
    private fun <T> storageCall(objectKey: String, call: () -> T): T =
        try {
            call()
        } catch (ex: S3Exception) {
            if (ex.statusCode() == NOT_FOUND) {
                throw BusinessException(ErrorCode.UPLOAD_OBJECT_NOT_FOUND, reason = "key=$objectKey")
            }
            throw BusinessException(ErrorCode.MEDIA_STORAGE_UNAVAILABLE, reason = "S3 ${ex.statusCode()} key=$objectKey")
        } catch (ex: SdkClientException) {
            throw BusinessException(ErrorCode.MEDIA_STORAGE_UNAVAILABLE, reason = "S3 client error key=$objectKey: ${ex.message}")
        }

    companion object {
        private const val NOT_FOUND = 404
        private val BROWSER_MANAGED_HEADERS = setOf("host", "content-length")
        private val STREAM_TIMEOUT = Duration.ofMinutes(10)
    }
}
