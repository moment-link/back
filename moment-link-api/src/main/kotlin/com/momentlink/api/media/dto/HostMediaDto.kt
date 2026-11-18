package com.momentlink.api.media.dto

import com.momentlink.api.media.port.out.DownloadTarget
import com.momentlink.domain.media.MediaAsset
import com.momentlink.domain.media.MediaType
import com.momentlink.domain.media.PublicationStatus
import com.momentlink.domain.participant.UploadStatus
import java.time.Instant

data class HostMediaItemResDto(
    val mediaId: Long,
    val fileName: String,
    val mediaType: MediaType,
    val thumbnailUrl: String?,
    val uploadStatus: UploadStatus,
    val publicationStatus: PublicationStatus,
    val uploaderDisplayName: String,
    val capturedAt: Instant?,
    val uploadedAt: Instant?,
) {
    companion object {
        fun from(media: MediaAsset, uploaderDisplayName: String, thumbnailUrl: String?): HostMediaItemResDto =
            HostMediaItemResDto(
                mediaId = media.requireId(),
                fileName = media.fileName,
                mediaType = media.mediaType,
                thumbnailUrl = thumbnailUrl,
                uploadStatus = media.uploadStatus,
                publicationStatus = media.publicationStatus,
                uploaderDisplayName = uploaderDisplayName,
                capturedAt = media.capturedAt,
                uploadedAt = media.uploadedAt,
            )
    }
}

data class HostMediaDetailResDto(
    val mediaId: Long,
    val fileName: String,
    val mediaType: MediaType,
    val mimeType: String,
    val sizeBytes: Long,
    val mediaUrl: String?,
    val thumbnailUrl: String?,
    val urlExpiresAt: Instant?,
    val capturedAt: Instant?,
    val uploadedAt: Instant?,
    val uploaderDisplayName: String,
    val uploadStatus: UploadStatus,
    val publicationStatus: PublicationStatus,
) {
    companion object {
        fun from(media: MediaAsset, uploaderDisplayName: String, view: DownloadTarget?, thumbnailUrl: String?): HostMediaDetailResDto =
            HostMediaDetailResDto(
                mediaId = media.requireId(),
                fileName = media.fileName,
                mediaType = media.mediaType,
                mimeType = media.mimeType,
                sizeBytes = media.sizeBytes,
                mediaUrl = view?.url,
                thumbnailUrl = thumbnailUrl,
                urlExpiresAt = view?.expiresAt,
                capturedAt = media.capturedAt,
                uploadedAt = media.uploadedAt,
                uploaderDisplayName = uploaderDisplayName,
                uploadStatus = media.uploadStatus,
                publicationStatus = media.publicationStatus,
            )
    }
}

data class PublicationChangeReqDto(
    val publicationStatus: PublicationStatus,
)

data class PublicationChangeResDto(
    val mediaId: Long,
    val publicationStatus: PublicationStatus,
)
