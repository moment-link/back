package com.momentlink.api.media.application

import com.momentlink.api.auth.common.BusinessException
import com.momentlink.api.media.port.out.MediaAssetRepository
import com.momentlink.api.media.port.out.MediaObjectStorage
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionTemplate
import java.time.Instant

// 기한 지난 업로드를 만료시키고, 삭제·취소·만료된 미디어의 S3 객체를 지운다.
@Component
class MediaCleanupJob(
    private val mediaAssetRepository: MediaAssetRepository,
    private val storage: MediaObjectStorage,
    private val properties: MediaUploadProperties,
    private val transactionTemplate: TransactionTemplate,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(fixedDelay = CLEANUP_INTERVAL_MS)
    fun run() {
        expirePendingUploads()
        purgeObjects()
    }

    private fun expirePendingUploads() {
        transactionTemplate.executeWithoutResult {
            val now = Instant.now()
            mediaAssetRepository.findExpiredPending(now.minus(properties.cleanupDelay), PageRequest.of(0, BATCH_SIZE))
                .forEach { it.expire(deleteAfter = now) }
        }
    }

    // deleteAfter가 URL 만료 + 유예 이후라 늦게 도착하는 PUT은 없다. 실패하면 purgedAt을 남기지 않아 다음 주기에 다시 지운다.
    private fun purgeObjects() {
        mediaAssetRepository.findPurgeCandidates(Instant.now(), PageRequest.of(0, BATCH_SIZE)).forEach { media ->
            try {
                storage.delete(media.objectKey)
                media.thumbnailObjectKey?.let(storage::delete)
                transactionTemplate.executeWithoutResult { mediaAssetRepository.markPurged(media.requireId(), Instant.now()) }
            } catch (ex: BusinessException) {
                log.warn("[{}] 미디어 객체 정리 실패, 다음 주기에 재시도: mediaId={} - {}", ex.errorCode.code, media.id, ex.reason)
            }
        }
    }

    companion object {
        private const val CLEANUP_INTERVAL_MS = 60_000L
        private const val BATCH_SIZE = 100
    }
}
