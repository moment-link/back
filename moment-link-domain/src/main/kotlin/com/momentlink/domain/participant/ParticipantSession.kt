package com.momentlink.domain.participant

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.Instant

@Entity
@Table(
    name = "participant_sessions",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_participant_event_display_name",
            columnNames = ["event_id", "display_name"]
        )
    ]
)
class ParticipantSession private constructor(
    eventId: Long,
    sessionTokenHash: String,
    expiresAt: Instant,
    displayName: String,
){
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @Column(name = "event_id", nullable = false, updatable = false)
    var eventId: Long = eventId
        protected set

    @Column(name = "session_token_hash", unique = true, nullable = false, updatable = false)
    var sessionTokenHash: String = sessionTokenHash
        protected set

    @Column(name = "expires_at", nullable = false, updatable = false)
    var expiresAt: Instant = expiresAt
        protected set

    @Column(name = "display_name", unique = true, nullable = false, updatable = true)
    var displayName: String = displayName
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    var role: ParticipantRole = ParticipantRole.PARTICIPANT

    fun requireId(): Long
        = checkNotNull(this.id) { "영속화되지 않은 ParticipantSession입니다" }

    fun isExpired(now: Instant): Boolean
        = !now.isBefore(expiresAt)

    fun promoteToCoHost() {
        role = ParticipantRole.CO_HOST
    }

    fun demoteToParticipant() {
        role = ParticipantRole.PARTICIPANT
    }
    companion object {
        fun create(
            eventId: Long,
            sessionTokenHash: String,
            expiresAt: Instant,
            displayName: String,
        ): ParticipantSession = ParticipantSession(
            eventId,
            sessionTokenHash,
            expiresAt,
            displayName
        )
    }
}