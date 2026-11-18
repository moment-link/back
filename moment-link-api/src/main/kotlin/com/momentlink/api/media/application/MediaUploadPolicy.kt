package com.momentlink.api.media.application

import com.momentlink.api.auth.common.BusinessException
import com.momentlink.api.common.web.ErrorCode
import com.momentlink.api.media.dto.PrepareUploadReqDto
import com.momentlink.domain.media.MediaType
import org.springframework.stereotype.Component
import java.util.Base64

@Component
class MediaUploadPolicy(
    private val properties: MediaUploadProperties,
) {

    fun validateIdempotencyKey(key: String) {
        if (key.isBlank() || key.length > MAX_IDEMPOTENCY_KEY_LENGTH) {
            throw BusinessException(ErrorCode.INVALID_IDEMPOTENCY_KEY)
        }
    }

    fun validate(req: PrepareUploadReqDto) {
        val fileName = req.fileName.trim()
        if (fileName.isEmpty() || fileName.length > MAX_FILE_NAME_LENGTH || req.sizeBytes <= 0 || !isSha256Base64(req.checksum)) {
            throw BusinessException(ErrorCode.INVALID_MEDIA_METADATA)
        }

        val (allowedTypes, maxBytes) = when (req.mediaType) {
            MediaType.IMAGE -> properties.imageMimeTypes to properties.imageMaxBytes
            MediaType.VIDEO -> properties.videoMimeTypes to properties.videoMaxBytes
        }
        if (normalizeMimeType(req.mimeType) !in allowedTypes) throw BusinessException(ErrorCode.UNSUPPORTED_MEDIA_TYPE)
        if (req.sizeBytes > maxBytes) throw BusinessException(ErrorCode.MEDIA_SIZE_LIMIT_EXCEEDED)
    }

    // MediaRecorder는 "video/webm;codecs=vp9"처럼 코덱을 붙여 준다. 저장·서명에는 기본 형식만 쓴다.
    fun normalizeMimeType(mimeType: String): String =
        mimeType.substringBefore(';').trim().lowercase()

    // SHA-256 원본 32바이트의 표준 Base64
    private fun isSha256Base64(value: String): Boolean =
        runCatching { Base64.getDecoder().decode(value).size == SHA256_BYTES }.getOrDefault(false)

    companion object {
        private const val MAX_IDEMPOTENCY_KEY_LENGTH = 128
        private const val MAX_FILE_NAME_LENGTH = 255
        private const val SHA256_BYTES = 32
    }
}
