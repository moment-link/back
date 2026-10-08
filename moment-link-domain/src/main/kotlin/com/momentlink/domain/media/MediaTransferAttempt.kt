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
@Table(name = "media_transfer_attempts")
class MediaTransferAttempt private constructor(
    mediaTransferId: Long,
    attemptNumber: Int,
    startedAt: Instant,
): BaseEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @Column(name = "media_transfer_id", nullable = false, updatable = false)
    var mediaTransferId: Long = mediaTransferId
        protected set

    @Column(name = "attempt_number", nullable = false, updatable = false)
    var attemptNumber: Int = attemptNumber
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "result")
    var result: TransferAttemptResult? = null
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "failure_type")
    var failureType: TransferFailureType? = null
        protected set

    @Column(name = "failure_message", length = 1_000)
    var failureMessage: String? = null
        protected set

    @Column(name = "started_at", nullable = false, updatable = false)
    var startedAt: Instant = startedAt
        protected set

    @Column(name = "finished_at")
    var finishedAt: Instant? = null
        protected set

    init {
        require(mediaTransferId > 0) { "media transfer id는 양수여야 합니다." }
        require(attemptNumber > 0) { "전송 시도 번호는 양수여야 합니다." }
    }

    fun requiredId(): Long =
        checkNotNull(id) {
            "영속화되지 않은 MediaTransferAttempt입니다."
        }

    fun markSucceeded(finishedAt: Instant) {
        check(result == null) {
            "이미 종료된 전송 시도입니다."
        }
        require(!finishedAt.isBefore(startedAt)) {
            "종료 시각은 시작 시각보다 앞설 수 없습니다."
        }

        result = TransferAttemptResult.SUCCEEDED
        this.finishedAt = finishedAt
    }

    fun markFailed(
        result: TransferAttemptResult,
        failureType: TransferFailureType,
        failureMessage: String?,
        finishedAt: Instant,
    ) {
        check(this.result == null) {
            "이미 종료된 전송 시도입니다."
        }
        require(
            result == TransferAttemptResult.RETRYABLE_FAILED ||
                    result == TransferAttemptResult.PERMANENT_FAILED
        ) {
            "실패 결과만 지정할 수 있습니다."
        }
        require(!finishedAt.isBefore(startedAt)) {
            "종료 시각은 시작 시각보다 앞설 수 없습니다."
        }

        this.result = result
        this.failureType = failureType
        this.failureMessage = failureMessage
            ?.trim()
            ?.takeIf(String::isNotEmpty)
        this.finishedAt = finishedAt
    }

    companion object {
        fun start(
            mediaTransferId: Long,
            attemptNumber: Int,
            startedAt: Instant,
        ): MediaTransferAttempt =
            MediaTransferAttempt(
                mediaTransferId = mediaTransferId,
                attemptNumber = attemptNumber,
                startedAt = startedAt,
            )
    }
}