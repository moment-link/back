package com.momentlink.api.media.application

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration
import java.time.Instant

@ConfigurationProperties(prefix = "media.upload")
data class MediaUploadProperties(
    val urlValidity: Duration,
    val viewUrlValidity: Duration,
    val imageMaxBytes: Long,
    val videoMaxBytes: Long,
    val imageMimeTypes: Set<String>,
    val videoMimeTypes: Set<String>,
    val cleanupDelay: Duration,
) {
    // 업로드 URL이 살아 있는 동안 도착할 수 있는 파일까지 지우도록 만료 시각 뒤로 미룬다.
    fun purgeAt(uploadExpiresAt: Instant?, now: Instant): Instant =
        maxOf(now, uploadExpiresAt ?: now).plus(cleanupDelay)
}
