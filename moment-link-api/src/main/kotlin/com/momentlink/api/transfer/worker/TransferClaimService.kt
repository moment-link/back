package com.momentlink.api.transfer.worker

import com.momentlink.api.transfer.port.out.MediaTransferAttemptRepository
import com.momentlink.api.transfer.port.out.MediaTransferRepository
import com.momentlink.domain.media.MediaTransferAttempt
import com.momentlink.domain.media.TransferFailureType
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

data class ClaimedTransfer(
    val transferId: Long,
    val attemptId: Long,
    val mediaAssetId: Long,
    val storageId: Long,
)

sealed interface TransferOutcome {
    data class Succeeded(val externalFileId: String) : TransferOutcome
    data class Failed(val failureType: TransferFailureType, val retryable: Boolean, val message: String?) : TransferOutcome
}

// 짧은 트랜잭션으로 작업 하나를 IN_PROGRESS로 점유하고 시도 이력을 남긴다. 실제 전송은 트랜잭션 밖에서 한다.
// 점유 쿼리가 공개 상태·삭제 여부·연결 상태를 확인하므로, 전송 직전에 한 건씩 잡는다.
@Service
class TransferClaimService(
    private val transferRepository: MediaTransferRepository,
    private val attemptRepository: MediaTransferAttemptRepository,
) {
    @Transactional
    fun claimNext(now: Instant): ClaimedTransfer? {
        val transfer = transferRepository.findClaimable(now, PageRequest.of(0, 1)).firstOrNull() ?: return null
        val attemptNumber = transfer.beginAttempt()
        val attempt = attemptRepository.save(MediaTransferAttempt.start(transfer.requiredId(), attemptNumber, now))
        return ClaimedTransfer(transfer.requiredId(), attempt.requiredId(), transfer.mediaAssetId, transfer.storageId)
    }
}
