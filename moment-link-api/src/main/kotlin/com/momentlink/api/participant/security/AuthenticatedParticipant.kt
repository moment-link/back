package com.momentlink.api.participant.security

import java.time.Instant


data class AuthenticatedParticipant(
    val sessionId: Long,
    val eventId: Long,
    val expiresAt: Instant,
) {
    companion object {
        fun create(sessionId: Long, eventId: Long, expiresAt: Instant): AuthenticatedParticipant {
            return AuthenticatedParticipant(
                sessionId = sessionId,
                eventId = eventId,
                expiresAt = expiresAt,
            )
        }
    }
}