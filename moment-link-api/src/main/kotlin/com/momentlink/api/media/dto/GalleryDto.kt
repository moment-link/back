package com.momentlink.api.media.dto

import com.fasterxml.jackson.annotation.JsonProperty
import com.momentlink.api.media.port.out.DownloadTarget
import com.momentlink.domain.media.MediaAsset
import com.momentlink.domain.media.MediaType
import java.time.Instant

// Kotlin의 is 접두사 프로퍼티가 JSON에서 "mine"으로 바뀌지 않게 이름을 고정한다.
data class GalleryItemResDto(
    val mediaId: Long,
    val mediaType: MediaType,
    val thumbnailUrl: String?,
    val capturedAt: Instant?,
    val uploadedAt: Instant?,
    val uploaderDisplayName: String,
    val likeCount: Int,
    val likedByMe: Boolean,
    @get:JsonProperty("isMine") val isMine: Boolean,
)

data class GalleryDetailResDto(
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
    val likeCount: Int,
    val likedByMe: Boolean,
    @get:JsonProperty("isMine") val isMine: Boolean,
) {
    companion object {
        fun from(
            media: MediaAsset,
            uploaderDisplayName: String,
            view: DownloadTarget?,
            thumbnailUrl: String?,
            likedByMe: Boolean,
            isMine: Boolean,
        ): GalleryDetailResDto =
            GalleryDetailResDto(
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
                likeCount = media.likeCount,
                likedByMe = likedByMe,
                isMine = isMine,
            )
    }
}

data class LikeResDto(
    val mediaId: Long,
    val likedByMe: Boolean,
    val likeCount: Int,
)
