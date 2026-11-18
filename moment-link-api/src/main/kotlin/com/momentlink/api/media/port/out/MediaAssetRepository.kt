package com.momentlink.api.media.port.out

import com.momentlink.domain.media.MediaAsset
import com.momentlink.domain.media.MediaType
import com.momentlink.domain.media.PublicationStatus
import com.momentlink.domain.participant.UploadStatus
import jakarta.persistence.LockModeType
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.Instant

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

    fun findByIdAndEventIdAndDeletedAtIsNull(id: Long, eventId: Long): MediaAsset?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT m FROM MediaAsset m WHERE m.id = :id AND m.eventId = :eventId")
    fun findInEventForUpdate(id: Long, eventId: Long): MediaAsset?

    // momentId 필터는 모먼트 시간대(capturedFrom <= capturedAt < capturedTo)로 바꿔서 넘긴다.
    @Query(
        """
        SELECT m FROM MediaAsset m
        WHERE m.eventId = :eventId
          AND m.deletedAt IS NULL
          AND (:publicationStatus IS NULL OR m.publicationStatus = :publicationStatus)
          AND (:uploadStatus IS NULL OR m.uploadStatus = :uploadStatus)
          AND (:mediaType IS NULL OR m.mediaType = :mediaType)
          AND (:capturedFrom IS NULL OR (m.capturedAt >= :capturedFrom AND m.capturedAt < :capturedTo))
    """
    )
    fun searchForHost(
        eventId: Long,
        publicationStatus: PublicationStatus?,
        uploadStatus: UploadStatus?,
        mediaType: MediaType?,
        capturedFrom: Instant?,
        capturedTo: Instant?,
        pageable: Pageable,
    ): Slice<MediaAsset>
}
