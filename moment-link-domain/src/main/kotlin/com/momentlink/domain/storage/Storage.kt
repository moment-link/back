package com.momentlink.domain.storage

import com.momentlink.domain.common.BaseEntity
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
@Table(name = "storages",
    uniqueConstraints = [UniqueConstraint(
        name = "uk_storage_event",
        columnNames = ["event_id"])]
)
class Storage private constructor(
    eventId: Long,
    provider: StorageProvider,
    externalAccountId: String,
    accountEmail: String,
    encryptedRefreshToken: String,
): BaseEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set

    @Column(name = "event_id", nullable = false, updatable = false)
    var eventId: Long = eventId
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false)
    var provider: StorageProvider = provider
        protected set

    @Column(name = "external_account_id", nullable = false)
    var externalAccountId: String = externalAccountId
        protected set

    @Column(name = "account_email", nullable = false)
    var accountEmail: String = accountEmail
        protected set

    @Column(name = "encrypted_refresh_token", nullable = false, columnDefinition = "TEXT")
    var encryptedRefreshToken: String = encryptedRefreshToken
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    var status: StorageStatus = StorageStatus.CONNECTED
        protected set

    // 행사 전용 폴더. 첫 전송 때 워커가 만든다.
    @Column(name = "destination_folder_id")
    var destinationFolderId: String? = null
        protected set

    fun isConnected(): Boolean
        = status == StorageStatus.CONNECTED

    fun requireReauthentication() {
        check(status != StorageStatus.DISCONNECTED) {
            "해제된 저장소는 재연결이 필요합니다."
        }
        status = StorageStatus.REAUTH_REQUIRED;
    }

    fun disconnect() {
        status = StorageStatus.DISCONNECTED
    }

    fun requiredId(): Long
        = checkNotNull(this.id) { "영속화되지 않은 Storage입니다." }

    fun reconnect(
        provider: StorageProvider,
        externalAccountId: String,
        accountEmail: String,
        encryptedRefreshToken: String
    ) {
        // 다른 계정으로 바꾸면 이전 계정의 폴더를 쓰지 않는다.
        if (this.externalAccountId != externalAccountId) {
            destinationFolderId = null
        }
        this.provider = provider
        this.externalAccountId = externalAccountId
        this.accountEmail = accountEmail
        this.encryptedRefreshToken = encryptedRefreshToken
        this.status = StorageStatus.CONNECTED
    }

    fun assignDestinationFolder(folderId: String) {
        require(folderId.isNotBlank()) { "폴더 ID는 비어 있을 수 없습니다." }
        destinationFolderId = folderId
    }

    fun clearDestinationFolder() {
        destinationFolderId = null
    }

    companion object {
        fun connect(
            eventId: Long,
            provider: StorageProvider,
            externalAccountId: String,
            accountEmail: String,
            encryptedRefreshToken: String,
        ): Storage = Storage(eventId, provider, externalAccountId, accountEmail, encryptedRefreshToken)
    }
}