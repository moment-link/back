package com.momentlink.api.media.dto

import com.momentlink.api.media.port.out.UploadTarget
import com.momentlink.domain.media.MediaAsset
import com.momentlink.domain.media.MediaType
import com.momentlink.domain.media.PublicationStatus
import com.momentlink.domain.participant.UploadStatus
import java.time.Instant

data class PrepareUploadReqDto(
    val fileName: String,
    val mediaType: MediaType,
    val mimeType: String,
    val sizeBytes: Long,
    val checksum: String,
    val capturedAt: Instant? = null,
)

data class PrepareUploadResDto(
    val mediaId: Long,
    val uploadStatus: UploadStatus,
    val upload: UploadTargetResDto,
) {
    companion object {
        fun from(media: MediaAsset, target: UploadTarget): PrepareUploadResDto =
            PrepareUploadResDto(
                mediaId = media.requireId(),
                uploadStatus = media.uploadStatus,
                upload = UploadTargetResDto(
                    method = "PUT",
                    url = target.url,
                    expiresAt = checkNotNull(media.uploadExpiresAt),
                    headers = target.headers,
                ),
            )
    }
}

data class UploadTargetResDto(
    val method: String,
    val url: String,
    val expiresAt: Instant,
    val headers: Map<String, String>,
)

data class CompleteUploadResDto(
    val mediaId: Long,
    val uploadStatus: UploadStatus,
    val publicationStatus: PublicationStatus,
    val uploadedAt: Instant?,
) {
    companion object {
        fun from(media: MediaAsset): CompleteUploadResDto =
            CompleteUploadResDto(media.requireId(), media.uploadStatus, media.publicationStatus, media.uploadedAt)
    }
}

data class CancelUploadResDto(
    val mediaId: Long,
    val uploadStatus: UploadStatus,
)

data class MediaDeleteResDto(
    val mediaId: Long,
    val deleted: Boolean,
)

data class MyMediaItemResDto(
    val mediaId: Long,
    val fileName: String,
    val mediaType: MediaType,
    val uploadStatus: UploadStatus,
    val publicationStatus: PublicationStatus,
    val thumbnailUrl: String?,
    val capturedAt: Instant?,
    val uploadedAt: Instant?,
) {
    companion object {
        fun from(media: MediaAsset, thumbnailUrl: String?): MyMediaItemResDto =
            MyMediaItemResDto(
                mediaId = media.requireId(),
                fileName = media.fileName,
                mediaType = media.mediaType,
                uploadStatus = media.uploadStatus,
                publicationStatus = media.publicationStatus,
                thumbnailUrl = thumbnailUrl,
                capturedAt = media.capturedAt,
                uploadedAt = media.uploadedAt,
            )
    }
}
