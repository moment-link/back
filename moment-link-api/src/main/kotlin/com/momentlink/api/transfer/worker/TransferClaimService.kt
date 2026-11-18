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

// 짧은 트랜잭션으로 작업을 IN_PROGRESS로 점유하고 시도 이력을 남긴다. 실제 전송은 트랜잭션 밖에서 한다.
@Service
class TransferClaimService(
    private val transferRepository: MediaTransferRepository,
    private val attemptRepository: MediaTransferAttemptRepository,
) {
    @Transactional
    fun claim(now: Instant, batchSize: Int): List<ClaimedTransfer> =
        transferRepository.findClaimable(now, PageRequest.of(0, batchSize)).map { transfer ->
            val attemptNumber = transfer.beginAttempt()
            val attempt = attemptRepository.save(MediaTransferAttempt.start(transfer.requiredId(), attemptNumber, now))
            ClaimedTransfer(transfer.requiredId(), attempt.requiredId(), transfer.mediaAssetId, transfer.storageId)
        }
}
