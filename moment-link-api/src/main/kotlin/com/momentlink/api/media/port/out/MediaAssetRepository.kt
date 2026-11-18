package com.momentlink.api.media.port.out

import com.momentlink.domain.media.MediaAsset
import com.momentlink.domain.media.PublicationStatus
import com.momentlink.domain.participant.UploadStatus
import jakarta.persistence.LockModeType
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface MediaAssetRepository : JpaRepository<MediaAsset, Long> {

    fun findByParticipantSessionIdAndPrepareIdempotencyKey(participantSessionId: Long, prepareIdempotencyKey: String): MediaAsset?

    fun findByIdAndEventIdAndParticipantSessionId(id: Long, eventId: Long, participantSessionId: Long): MediaAsset?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT m FROM MediaAsset m WHERE m.id = :id AND m.eventId = :eventId AND m.participantSessionId = :sessionId")
    fun findOwnedForUpdate(id: Long, eventId: Long, sessionId: Long): MediaAsset?

    @Query(
        """
        SELECT m FROM MediaAsset m
        WHERE m.participantSessionId = :sessionId
          AND m.deletedAt IS NULL
          AND (:uploadStatus IS NULL OR m.uploadStatus = :uploadStatus)
          AND (:publicationStatus IS NULL OR m.publicationStatus = :publicationStatus)
    """
    )
    fun searchMine(
        sessionId: Long,
        uploadStatus: UploadStatus?,
        publicationStatus: PublicationStatus?,
        pageable: Pageable,
    ): Slice<MediaAsset>
}
