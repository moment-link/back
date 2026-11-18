package com.momentlink.api.transfer.port.out

import com.momentlink.domain.media.MediaTransfer
import com.momentlink.domain.media.TransferStatus
import jakarta.persistence.LockModeType
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface MediaTransferRepository : JpaRepository<MediaTransfer, Long> {

    fun existsByMediaAssetId(mediaAssetId: Long): Boolean

    fun findAllByStorageId(storageId: Long, pageable: Pageable): Slice<MediaTransfer>

    fun findAllByStorageIdAndStatus(storageId: Long, status: TransferStatus, pageable: Pageable): Slice<MediaTransfer>

    fun findByIdAndStorageId(id: Long, storageId: Long): MediaTransfer?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM MediaTransfer t WHERE t.id = :id AND t.storageId = :storageId")
    fun findByIdAndStorageIdForUpdate(id: Long, storageId: Long): MediaTransfer?
}
