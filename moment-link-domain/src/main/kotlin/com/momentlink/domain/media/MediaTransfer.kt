package com.momentlink.domain.media

import com.momentlink.domain.common.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "media_transfers")
class MediaTransfer private constructor(
    mediaAssetId: Long,
    storageId: Long,
): BaseEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set

    @Column(name = "media_asset_id", unique = true, nullable = false, updatable = false)
    var mediaAssetId: Long = mediaAssetId
        protected set

    @Column(name = "storage_id", nullable = false, updatable = false)
    var storageId: Long = storageId
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name =  "status", nullable = false)
    var status: TransferStatus = TransferStatus.PENDING
        protected set

    @Column(name = "attempt_count", nullable = false)
    var attemptCount: Int = 0
        protected set

    @Column(name = "next_retry_at")
    var nextRetryAt: Instant? = null
        protected set

    @Column(name = "external_file_id")
    var externalFileId: String? = null
        protected set

    @Column(name = "transferred_at")
    var transferredAt: Instant? = null
        protected set

    init {
        require(mediaAssetId > 0) { "미디어 Id는 양수여야 합니다." }
        require(storageId > 0) { "스토리지 Id는 양수여야 합니다." }
    }

    fun requiredId(): Long =
        checkNotNull(id) { "영속화되지 않은 MediaTransfer입니다." }

    // 전송 시도를 시작. 이번 시도 번호를 반환
    fun beginAttempt(): Int {
        check(status == TransferStatus.PENDING ||
        status == TransferStatus.RETRY_WAIT) { "대기 중인 전송만 시작할 수 있습니다." }

        status = TransferStatus.IN_PROGRESS
        nextRetryAt = null
        attemptCount += 1
        return attemptCount
    }

    fun scheduleRetry(
        now: Instant,
        retryAt: Instant,
    ) {
        check(status == TransferStatus.IN_PROGRESS) {
            "진행 중인 전송만 재시도 대기로 전환할 수 있습니다."
        }
        require(retryAt.isAfter(now)) {
            "재시도 시각은 현재 시각보다 뒤여야 합니다."
        }
        status = TransferStatus.RETRY_WAIT
        nextRetryAt = retryAt
    }

    fun markSucceeded(
        externalFileId: String,
        transferredAt: Instant,
    ) {
        check(status == TransferStatus.IN_PROGRESS) { "진행 중인 전송만 완료할 수 있습니다." }
        require(externalFileId.isNotBlank()) { "파일Id는 비어 있을 수 없습니다." }

        status = TransferStatus.SUCCEEDED
        this.externalFileId = externalFileId
        this.transferredAt = transferredAt
        nextRetryAt = null
    }

    fun markPermanentlyFailed() {
        check(status == TransferStatus.IN_PROGRESS) { "진행 중인 전송만 영구 실패 처리할 수 있습니다." }

        status = TransferStatus.PERMANENT_FAILED
        nextRetryAt = null
    }

    companion object {
        fun create(
            mediaAssetId: Long,
            storageId: Long,
        ): MediaTransfer =
            MediaTransfer(
                mediaAssetId = mediaAssetId,
                storageId = storageId,
            )
    }
}