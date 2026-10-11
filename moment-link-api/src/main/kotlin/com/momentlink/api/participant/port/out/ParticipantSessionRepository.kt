package com.momentlink.api.participant.port.out

import com.momentlink.domain.participant.ParticipantSession
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface ParticipantSessionRepository: JpaRepository<ParticipantSession, Long> {

    fun findBySessionTokenHash(sessionTokenHash: String): ParticipantSession?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
        """
        SELECT us
        FROM ParticipantSession us
        WHERE us.id = :sessionId
    """
    )
    fun findByIdForUpdate(sessionId: Long): ParticipantSession?
}