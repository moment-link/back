package com.momentlink.api.participant.application

import com.momentlink.api.auth.common.BusinessException
import com.momentlink.api.common.security.key.KeyGenerator
import com.momentlink.api.common.security.key.KeyHasher
import com.momentlink.api.common.web.ErrorCode
import com.momentlink.api.event.port.out.EventRepository
import com.momentlink.api.event.port.out.PublicAccessRepository
import com.momentlink.api.participant.dto.SessionResult
import com.momentlink.api.participant.port.out.ParticipantSessionRepository
import com.momentlink.api.participant.security.AuthenticatedParticipant
import com.momentlink.domain.participant.ParticipantSession
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.Instant
import java.util.UUID

@Service
class ParticipantSessionAuthService(
    private val participantSessionRepository: ParticipantSessionRepository,
    private val publicAccessRepository: PublicAccessRepository,
    private val keyGenerator: KeyGenerator,
    private val keyHasher: KeyHasher,
    private val eventRepository: EventRepository,
) {
    @Transactional(readOnly = true)
    fun authenticate(token: String, eventId: Long): AuthenticatedParticipant {
        val now = Instant.now()
        val hashedKey = keyHasher.hash(token)

        val uploadSession = participantSessionRepository.findBySessionTokenHash(hashedKey)
            ?: throw BusinessException(ErrorCode.INVALID_PARTICIPANT_SESSION)

        if (uploadSession.isExpired(now)) {
            throw BusinessException(ErrorCode.PARTICIPANT_SESSION_EXPIRED)
        }

        if (uploadSession.eventId != eventId) {
            throw BusinessException(ErrorCode.PARTICIPANT_EVENT_MISMATCH)
        }

        val event = eventRepository.findByIdOrNull(uploadSession.eventId)
            ?: throw BusinessException(ErrorCode.NOT_FOUND_EVENT)

        if (!event.publicAccess.isAccessible(now)) {
            throw BusinessException(ErrorCode.EVENT_ACCESS_DISABLED)
        }

        return AuthenticatedParticipant.create(
            sessionId = uploadSession.requireId(),
            eventId = event.requiredId(),
            expiresAt = uploadSession.expiresAt
        )
    }

    /**
     * accessKey가 event와 연결되는지 validation
     * existingToken으로 세션 찾아 반환
     * 없다면, 세션 생성해서 반화
     */
    @Transactional
    fun getOrCreateSession(
        eventId: Long,
        accessKey: String,
        existingToken: String?,
    ): SessionResult {
        val now = Instant.now()

        validateEventAccess(eventId, accessKey, now)
        val session = findSession(eventId, existingToken, now)

        if(session != null) {
            return SessionResult(
                sessionId = session.requireId(),
                eventId = eventId,
                expiresAt = session.expiresAt,
                newToken = null
            )
        }

        return createSession(eventId, now)
    }

    private fun validateEventAccess(eventId: Long, accessKey: String, now: Instant) {
        val publicAccess = publicAccessRepository.findByAccessKeyHash(keyHasher.hash(accessKey))
            ?: throw BusinessException(ErrorCode.INVALID_EVENT_ACCESS_KEY)

        if( publicAccess.event.requiredId() != eventId) {
            throw BusinessException(ErrorCode.INVALID_EVENT_ACCESS_KEY)
        }

        if (!publicAccess.isAccessible(now)) {
            throw BusinessException(ErrorCode.EVENT_ACCESS_DISABLED)
        }
    }

    private fun findSession(eventId: Long, token: String?, now: Instant): ParticipantSession? {
        if (token.isNullOrBlank()) return null

        val session = participantSessionRepository.findBySessionTokenHash(keyHasher.hash(token))
            ?: return null

        if(session.eventId != eventId) return null
        if(!now.isBefore(session.expiresAt)) return null

        return session
    }

    private fun createSession(eventId: Long, now: Instant): SessionResult {
        val token = keyGenerator.generate()
        val expiresAt = now.plus(Duration.ofHours(12))

        val session = participantSessionRepository.save(
            ParticipantSession.create(
                eventId = eventId,
                sessionTokenHash = token.hashedKey,
                displayName = "참가자_${UUID.randomUUID()}",
                expiresAt = expiresAt,
            )
        )

        return SessionResult(
            sessionId = session.requireId(),
            eventId = eventId,
            expiresAt = expiresAt,
            newToken = token.rawKey
        )
    }
}