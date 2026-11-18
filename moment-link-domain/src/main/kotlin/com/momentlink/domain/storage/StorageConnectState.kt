package com.momentlink.domain.storage

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.Instant

// 저장소 OAuth 연결을 시작한 주최자·행사를 콜백까지 한 번만 이어 주는 state
@Entity
@Table(name = "storage_connect_state", indexes = [Index(name = "idx_storage_connect_state_expires_at", columnList = "expires_at")])
class StorageConnectState(
    @Id
    @Column(length = 43)
    val state: String,

    @Column(name = "user_id", nullable = false)
    val userId: Long,

    @Column(name = "event_id", nullable = false)
    val eventId: Long,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val provider: StorageProvider,

    @Column(name = "expires_at", nullable = false)
    val expiresAt: Instant,
)
