package com.momentlink.api.participant.dto

import java.time.Instant

data class SessionResDto(
    val sessionId: Long,
    val eventId: Long,
    val expiresAt: Instant,
)

data class SessionResult(
    val sessionId: Long,
    val eventId: Long,
    val expiresAt: Instant,
    val newToken: String?,
)