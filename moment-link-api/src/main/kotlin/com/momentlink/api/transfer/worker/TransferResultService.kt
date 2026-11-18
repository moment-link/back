package com.momentlink.api.transfer.worker

import com.momentlink.api.storage.port.out.StorageRepository
import com.momentlink.api.transfer.port.out.MediaTransferAttemptRepository
import com.momentlink.api.transfer.port.out.MediaTransferRepository
import com.momentlink.domain.media.MediaTransfer
import com.momentlink.domain.media.MediaTransferAttempt
import com.momentlink.domain.media.TransferAttemptResult
import com.momentlink.domain.media.TransferFailureType
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.Instant

@Service
class TransferResultService(
    private val transferRepository: MediaTransferRepository,
    private val attemptRepository: MediaTransferAttemptRepository,
    private val storageRepository: StorageRepository,
    private val properties: TransferWorkerProperties,
) {

    @Transactional
    fun record(claimed: ClaimedTransfer, outcome: TransferOutcome, now: Instant) {
        val transfer = transferRepository.findByIdForUpdate(claimed.transferId) ?: return
        val attempt = attemptRepository.findByIdOrNull(claimed.attemptId) ?: return
        when (outcome) {
            is TransferOutcome.Succeeded -> {
                attempt.markSucceeded(now)
                transfer.markSucceeded(outcome.externalFileId, now)
            }
            is TransferOutcome.Failed -> recordFailure(transfer, attempt, claimed.storageId, outcome, now)
        }
    }

    @Transactional
    fun recoverStale(now: Instant) {
        transferRepository.findStaleInProgress(now.minus(properties.staleAfter)).forEach { transfer ->
            attemptRepository.findByMediaTransferIdAndAttemptNumber(transfer.requiredId(), transfer.attemptCount)
                ?.takeIf { it.result == null }
                ?.markFailed(TransferAttemptResult.RETRYABLE_FAILED, TransferFailureType.UNKNOWN, "작업이 중단되어 다시 시도합니다.", now)
            transfer.scheduleRetry(now, now.plus(STALE_RETRY_DELAY))
        }
    }

    private fun recordFailure(
        transfer: MediaTransfer,
        attempt: MediaTransferAttempt,
        storageId: Long,
        outcome: TransferOutcome.Failed,
        now: Instant,
    ) {
        val willRetry = outcome.retryable && transfer.attemptCount < properties.maxAttempts
        val result = if (willRetry) TransferAttemptResult.RETRYABLE_FAILED else TransferAttemptResult.PERMANENT_FAILED
        attempt.markFailed(result, outcome.failureType, outcome.message, now)

        if (willRetry) {
            transfer.scheduleRetry(now, now.plus(backoff(transfer.attemptCount)))
        } else {
            transfer.markPermanentlyFailed()
        }

        // 재연결과 겹쳐도 새 토큰을 덮어쓰지 않도록 잠그고 바꾼다.
        val storage = storageRepository.findByIdForUpdate(storageId) ?: return
        when (outcome.failureType) {
            TransferFailureType.AUTHENTICATION -> if (storage.isConnected()) storage.requireReauthentication()
            TransferFailureType.INVALID_DESTINATION -> storage.clearDestinationFolder()
            else -> Unit
        }
    }

    // 1, 2, 4, 8, 16, 32분
    private fun backoff(attemptCount: Int): Duration =
        Duration.ofMinutes(1L shl (attemptCount - 1).coerceIn(0, MAX_BACKOFF_SHIFT))

    companion object {
        private const val MAX_BACKOFF_SHIFT = 5
        private val STALE_RETRY_DELAY = Duration.ofMinutes(1)
    }
}
