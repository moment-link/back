package com.momentlink.domain.media

import com.momentlink.domain.common.BaseEntity
import com.momentlink.domain.participant.UploadStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.Instant

@Entity
@Table(name = "media_assets",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_media_object_key",
            columnNames = ["object_key"],
        ),
        UniqueConstraint(
            name = "uk_media_session_prepare_idempotency",
            columnNames = ["upload_session_id", "prepare_idempotency_key"],
        ),
    ],)
class MediaAsset private constructor(
    eventId: Long,
    participantSessionId: Long,
    prepareIdempotencyKey: String,
    objectKey: String,
    fileName: String,
    mediaType: MediaType,
    mimeType: String,
    sizeBytes: Long,
    checkSum: String,
    capturedAt: Instant?,
): BaseEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @Column(name = "event_id", nullable = false, updatable = false)
    var eventId: Long = eventId
        protected set

    @Column(name = "upload_session_id", nullable = false, updatable = false)
    var participantSessionId: Long = participantSessionId
        protected set

    @Column(name = "prepare_idempotency_key", nullable = false, updatable = false, length = 128)
    var prepareIdempotencyKey: String = prepareIdempotencyKey
        protected set

    @Column(name = "complete_idempotency_key", length = 128)
    var completeIdempotencyKey: String? = null
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "publication_status", nullable = false)
    var publicationStatus: PublicationStatus = PublicationStatus.PENDING_REVIEW
        protected set

    @Column(name = "completion_etag")
    var completionETag: String? = null
        protected set

    @Column(name = "object_key", nullable = false, updatable = false)
    var objectKey: String = objectKey
        protected set

    @Column(name = "file_name", nullable = false, updatable = false)
    var fileName: String = fileName
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "media_type", nullable = false, updatable = false)
    var mediaType: MediaType = mediaType
        protected set

    @Column(name = "mime_type", nullable = false, updatable = false)
    var mimeType: String = mimeType
        protected set

    @Column(name = "size_bytes", nullable = false, updatable = false)
    var sizeBytes: Long = sizeBytes
        protected set

    @Column(name = "checksum", updatable = false)
    var checkSum: String = checkSum
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "upload_status", nullable = false)
    var uploadStatus: UploadStatus = UploadStatus.PENDING
        protected set

    @Column(name = "uploaded_at")
    var uploadedAt: Instant? = null
        protected set

    @Column(name = "thumbnail_object_key")
    var thumbnailObjectKey: String? = null
        protected set

    @Column(name = "captured_at", updatable = false)
    var capturedAt: Instant? = capturedAt
        protected set

    @Column(name = "delete_after")
    var deleteAfter: Instant? = null
        protected set

    @Column(name = "deleted_at")
    var deletedAt: Instant? = null
        protected set

    init {
        require(fileName.isNotBlank())
        require(sizeBytes > 0)
        require(checkSum.isNotBlank())
    }

    fun delete(now: Instant) {
        if (deletedAt != null) return
        if (uploadStatus == UploadStatus.PENDING) {
            uploadStatus = UploadStatus.CANCELED
        }
        deletedAt = now
    }

    fun isDeleted(): Boolean = deletedAt != null


    fun requireId(): Long =
        checkNotNull(this.id) { "영속화되지 않은 객체입니다." }

    fun markUploaded(
        uploadedAt: Instant,
        completeIdempotencyKey: String,
        completionETag: String,
    ) {
        require(completeIdempotencyKey.isNotBlank())
        require(completionETag.isNotBlank())
        check(!isDeleted()) {
            "삭제된 미디어는 업로드 완료 처리할 수 없습니다."
        }
        check(uploadStatus == UploadStatus.PENDING) {
            "대기 중인 미디어만 업로드 완료 처리할 수 있습니다."
        }

        this.completeIdempotencyKey = completeIdempotencyKey
        this.completionETag = completionETag
        this.uploadStatus = UploadStatus.UPLOADED
        this.uploadedAt = uploadedAt
    }

    fun expire(deleteAfter: Instant) {
        check(uploadStatus == UploadStatus.PENDING) {
            "대기 중인 미디어만 만료 처리할 수 있습니다."
        }

        uploadStatus = UploadStatus.EXPIRED
        this.deleteAfter = deleteAfter
    }

    fun cancelUpload(now: Instant) {
        if(uploadStatus == UploadStatus.CANCELED) return
        check(uploadStatus == UploadStatus.PENDING) {
            "대기 중인 업로드만 취소할 수 있습니다."
        }
        uploadStatus = UploadStatus.CANCELED
        deletedAt = now
    }

    fun changePublicationStatus(status: PublicationStatus) {
        check(!isDeleted()) {
            "삭제된 미디어의 공개 상태는 변경할 수 없습니다."
        }
        check(uploadStatus == UploadStatus.UPLOADED) {
            "업로드가 완료된 미디어만 공개 상태를 변경할 수 있습니다."
        }
        publicationStatus = status
    }

    fun isPubliclyVisible(): Boolean {
        return !isDeleted() &&
                uploadStatus == UploadStatus.UPLOADED &&
                publicationStatus == PublicationStatus.APPROVED
    }

    companion object {
        fun prepare(eventId: Long,
                    uploadSessionId: Long,
                    prepareIdempotencyKey: String,
                    objectKey: String,
                    fileName: String,
                    mediaType: MediaType,
                    mimeType: String,
                    sizeBytes: Long,
                    checkSum: String,
                    capturedAt: Instant): MediaAsset
        = MediaAsset(eventId, uploadSessionId, prepareIdempotencyKey, objectKey, fileName, mediaType, mimeType, sizeBytes, checkSum, capturedAt)
    }
}
