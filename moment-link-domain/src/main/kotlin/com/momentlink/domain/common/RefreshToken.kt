package com.momentlink.domain.common

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "refresh_token")
class RefreshToken private constructor(
    expiresAt: Instant,
    userId: Long,
    token: String,
): BaseEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set

    @Column(nullable = false, unique = true, updatable = false)
    var token: String = token
        protected set

    @Column(nullable = false)
    var expiresAt: Instant? = expiresAt
        protected set

    @Column(nullable = false, updatable = false)
    var userId: Long = userId
        protected set

    companion object {
        fun create(expiresAt: Instant, userId: Long, token: String): RefreshToken
            = RefreshToken(expiresAt, userId, token)
    }
}