package com.momentlink.api.media.port.out

import com.momentlink.domain.media.MediaAsset
import jakarta.persistence.LockModeType
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
}
