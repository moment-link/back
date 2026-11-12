package com.momentlink.domain.common

import com.momentlink.domain.user.LoginProvider
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "oauth_login_state", indexes = [Index(name = "idx_oauth_login_state_expires_at", columnList = "expires_at")])
class OAuthLoginState(
    @Id
    @Column(length = 43)
    val state: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val provider: LoginProvider,

    @Column(name = "expires_at", nullable = false)
    val expiresAt: Instant,
)
