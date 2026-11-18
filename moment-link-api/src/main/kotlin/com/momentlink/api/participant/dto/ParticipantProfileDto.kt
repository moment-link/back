package com.momentlink.api.participant.dto

import com.momentlink.domain.participant.ParticipantRole
import com.momentlink.domain.participant.ParticipantSession
import java.time.Instant

data class ParticipantProfileResDto(
    val sessionId: Long,
    val eventId: Long,
    val displayName: String,
    val role: ParticipantRole,
    val expiresAt: Instant,
) {
    companion object {
        fun from(session: ParticipantSession): ParticipantProfileResDto =
            ParticipantProfileResDto(
                sessionId = session.requireId(),
                eventId = session.eventId,
                displayName = session.displayName,
                role = session.role,
                expiresAt = session.expiresAt,
            )
    }
}

data class DisplayNameUpdateReqDto(
    val displayName: String,
)

data class DisplayNameUpdateResDto(
    val sessionId: Long,
    val displayName: String,
)
