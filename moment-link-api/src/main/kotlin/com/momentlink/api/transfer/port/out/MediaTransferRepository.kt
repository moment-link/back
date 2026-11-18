package com.momentlink.api.transfer.port.out

import com.momentlink.domain.media.MediaTransfer
import com.momentlink.domain.media.TransferStatus
import jakarta.persistence.LockModeType
import jakarta.persistence.QueryHint
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.jpa.repository.QueryHints
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
interface MediaTransferRepository : JpaRepository<MediaTransfer, Long> {

    fun existsByMediaAssetId(mediaAssetId: Long): Boolean

    fun findAllByStorageId(storageId: Long, pageable: Pageable): Slice<MediaTransfer>

    fun findAllByStorageIdAndStatus(storageId: Long, status: TransferStatus, pageable: Pageable): Slice<MediaTransfer>

    fun findByIdAndStorageId(id: Long, storageId: Long): MediaTransfer?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM MediaTransfer t WHERE t.id = :id AND t.storageId = :storageId")
    fun findByIdAndStorageIdForUpdate(id: Long, storageId: Long): MediaTransfer?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM MediaTransfer t WHERE t.id = :id")
    fun findByIdForUpdate(id: Long): MediaTransfer?

    // 업로드 완료·승인·미삭제 미디어와 CONNECTED 저장소인 작업만 잡는다. 나머지는 조건이 맞을 때까지 대기한다.
    // lock.timeout = -2는 Hibernate에서 SKIP LOCKED라 워커가 여러 개여도 같은 작업을 두 번 잡지 않는다.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2"))
    @Query(
        """
        SELECT t FROM MediaTransfer t
        WHERE t.status IN (com.momentlink.domain.media.TransferStatus.PENDING, com.momentlink.domain.media.TransferStatus.RETRY_WAIT)
          AND (t.nextRetryAt IS NULL OR t.nextRetryAt <= :now)
          AND EXISTS (
            SELECT s FROM Storage s
            WHERE s.id = t.storageId AND s.status = com.momentlink.domain.storage.StorageStatus.CONNECTED
          )
          AND EXISTS (
            SELECT m FROM MediaAsset m
            WHERE m.id = t.mediaAssetId
              AND m.deletedAt IS NULL
              AND m.uploadStatus = com.momentlink.domain.participant.UploadStatus.UPLOADED
              AND m.publicationStatus = com.momentlink.domain.media.PublicationStatus.APPROVED
          )
        ORDER BY t.id
    """
    )
    fun findClaimable(now: Instant, pageable: Pageable): List<MediaTransfer>

    // 서버가 전송 도중 재시작되면 IN_PROGRESS로 남는다.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM MediaTransfer t WHERE t.status = com.momentlink.domain.media.TransferStatus.IN_PROGRESS AND t.updatedAt < :threshold")
    fun findStaleInProgress(threshold: Instant): List<MediaTransfer>
}
