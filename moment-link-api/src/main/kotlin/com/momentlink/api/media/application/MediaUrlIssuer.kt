package com.momentlink.api.media.application

import com.momentlink.api.media.port.out.DownloadTarget
import com.momentlink.api.media.port.out.MediaObjectStorage
import com.momentlink.domain.media.MediaAsset
import com.momentlink.domain.media.MediaType
import com.momentlink.domain.participant.UploadStatus
import org.springframework.stereotype.Component

@Component
class MediaUrlIssuer(
    private val storage: MediaObjectStorage,
    private val properties: MediaUploadProperties,
) {
    // 업로드 완료 전에는 열람 URL이 없다.
    fun viewUrl(media: MediaAsset): DownloadTarget? =
        if (media.uploadStatus == UploadStatus.UPLOADED) {
            storage.presignDownload(media.objectKey, properties.viewUrlValidity)
        } else {
            null
        }

    // 썸네일 생성 전에는 이미지 원본으로 대신한다.
    fun thumbnailUrl(media: MediaAsset): String? {
        media.thumbnailObjectKey?.let { return storage.presignDownload(it, properties.viewUrlValidity).url }
        if (media.mediaType != MediaType.IMAGE) return null
        return viewUrl(media)?.url
    }
}
