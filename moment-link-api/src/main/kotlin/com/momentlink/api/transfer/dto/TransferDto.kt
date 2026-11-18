package com.momentlink.api.transfer.dto

import com.momentlink.domain.media.MediaTransfer
import com.momentlink.domain.media.MediaTransferAttempt
import com.momentlink.domain.media.TransferAttemptResult
import com.momentlink.domain.media.TransferFailureType
import com.momentlink.domain.media.TransferStatus
import java.time.Instant

data class TransferItemResDto(
    val transferId: Long,
    val mediaId: Long,
    val fileName: String,
    val storageId: Long,
    val status: TransferStatus,
    val attemptCount: Int,
    val nextRetryAt: Instant?,
    val transferredAt: Instant?,
) {
    companion object {
        fun from(transfer: MediaTransfer, fileName: String): TransferItemResDto =
            TransferItemResDto(
                transferId = transfer.requiredId(),
                mediaId = transfer.mediaAssetId,
                fileName = fileName,
                storageId = transfer.storageId,
                status = transfer.status,
                attemptCount = transfer.attemptCount,
                nextRetryAt = transfer.nextRetryAt,
                transferredAt = transfer.transferredAt,
            )
    }
}

data class TransferDetailResDto(
    val transferId: Long,
    val mediaId: Long,
    val fileName: String,
    val storageId: Long,
    val status: TransferStatus,
    val attemptCount: Int,
    val nextRetryAt: Instant?,
    val transferredAt: Instant?,
    val externalFileId: String?,
    val attempts: List<TransferAttemptResDto>,
) {
    companion object {
        fun from(transfer: MediaTransfer, fileName: String, attempts: List<MediaTransferAttempt>): TransferDetailResDto =
            TransferDetailResDto(
                transferId = transfer.requiredId(),
                mediaId = transfer.mediaAssetId,
                fileName = fileName,
                storageId = transfer.storageId,
                status = transfer.status,
                attemptCount = transfer.attemptCount,
                nextRetryAt = transfer.nextRetryAt,
                transferredAt = transfer.transferredAt,
                externalFileId = transfer.externalFileId,
                attempts = attempts.map(TransferAttemptResDto::from),
            )
    }
}

// 실패 메시지는 저장된 원문 대신 유형별 고정 문장을 준다. (토큰·개인정보 노출 방지)
data class TransferAttemptResDto(
    val attemptNumber: Int,
    val result: TransferAttemptResult?,
    val failureType: TransferFailureType?,
    val failureMessage: String?,
    val startedAt: Instant,
    val finishedAt: Instant?,
) {
    companion object {
        fun from(attempt: MediaTransferAttempt): TransferAttemptResDto =
            TransferAttemptResDto(
                attemptNumber = attempt.attemptNumber,
                result = attempt.result,
                failureType = attempt.failureType,
                failureMessage = attempt.failureType?.let(::guideMessage),
                startedAt = attempt.startedAt,
                finishedAt = attempt.finishedAt,
            )

        private fun guideMessage(type: TransferFailureType): String =
            when (type) {
                TransferFailureType.RATE_LIMITED -> "외부 저장소 요청 한도를 초과했습니다."
                TransferFailureType.TIMEOUT -> "외부 저장소 응답 시간이 초과되었습니다."
                TransferFailureType.NETWORK -> "외부 저장소와 통신하지 못했습니다."
                TransferFailureType.PROVIDER_SERVER_ERROR -> "외부 저장소에서 오류가 발생했습니다."
                TransferFailureType.AUTHENTICATION -> "저장소 재인증이 필요합니다."
                TransferFailureType.PERMISSION -> "저장소에 파일을 저장할 권한이 없습니다."
                TransferFailureType.INVALID_DESTINATION -> "저장할 폴더를 찾을 수 없습니다."
                TransferFailureType.UNKNOWN -> "알 수 없는 오류로 전송하지 못했습니다."
            }
    }
}

data class TransferRetryResDto(
    val transferId: Long,
    val status: TransferStatus,
)
