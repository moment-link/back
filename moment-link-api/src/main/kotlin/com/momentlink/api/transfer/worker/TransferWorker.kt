package com.momentlink.api.transfer.worker

import com.momentlink.api.storage.infra.google.DriveTransferException
import com.momentlink.domain.media.TransferFailureType
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Instant

// 서버 메모리(t4g.micro) 때문에 별도 sync 앱 대신 API 프로세스 안에서 돈다.
@Component
@ConditionalOnProperty(prefix = "transfer.worker", name = ["enabled"], havingValue = "true")
class TransferWorker(
    private val claimService: TransferClaimService,
    private val resultService: TransferResultService,
    private val executor: DriveTransferExecutor,
    private val properties: TransferWorkerProperties,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(fixedDelayString = "\${transfer.worker.poll-interval-ms}")
    fun run() {
        resultService.recoverStale(Instant.now())
        repeat(properties.batchSize) {
            val claimed = claimService.claimNext(Instant.now()) ?: return
            process(claimed)
        }
    }

    // 한 작업의 실패가 다음 작업이나 스케줄러를 멈추지 않게 한다.
    private fun process(claimed: ClaimedTransfer) {
        val outcome = try {
            TransferOutcome.Succeeded(executor.transfer(claimed))
        } catch (ex: DriveTransferException) {
            log.warn("전송 실패: transferId={}, type={}, retryable={} - {}", claimed.transferId, ex.failureType, ex.retryable, ex.message)
            TransferOutcome.Failed(ex.failureType, ex.retryable, ex.message)
        } catch (ex: Exception) {
            log.error("전송 처리 중 예상하지 못한 오류: transferId={}", claimed.transferId, ex)
            TransferOutcome.Failed(TransferFailureType.UNKNOWN, retryable = true, message = null)
        }

        try {
            resultService.record(claimed, outcome, Instant.now())
        } catch (ex: Exception) {
            log.error("전송 결과 기록 실패: transferId={}, outcome={}", claimed.transferId, outcome::class.simpleName, ex)
        }
    }
}
