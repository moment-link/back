package com.momentlink.domain.event

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "public_access")
class PublicAccess private constructor(
    event: Event,
    accessKeyHash: String,
    accessKey: String? = null,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id")
    var event: Event = event
        protected set

    @Column(name = "access_key_hash", nullable = false, unique = true)
    var accessKeyHash: String = accessKeyHash
        protected set

    // 호스트의 공유 링크 재조회용. 이전 해시 전용 데이터는 재발급 전까지 null이다.
    @Column(name = "access_key", length = 43)
    var accessKey: String? = accessKey
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    var status: PublicAccessStatus = PublicAccessStatus.ACTIVE
        protected set

    @Column(name = "disabled_at")
    var disabledAt: Instant? = null // 해당 필드는 생성자에 없기 때문에 선언할 때 초기화 (null로 초기화하므로 ? 붙이기)
        protected set

    fun isAccessible(now: Instant): Boolean {
        return status == PublicAccessStatus.ACTIVE
    }

    fun enable(now: Instant) {
        check(status == PublicAccessStatus.DISABLED) { "이미 활성화된 공개 접근입니다." }

        this.status = PublicAccessStatus.ACTIVE
        this.disabledAt = null
    }

    fun disable(disabledAt: Instant) {
        check(status == PublicAccessStatus.ACTIVE) { "이미 만료된 공개 접근입니다." }

        this.status = PublicAccessStatus.DISABLED
        this.disabledAt = disabledAt
    }

    fun rotate(newAccessKeyHash: String, newAccessKey: String? = null) {
        require(newAccessKeyHash.isNotBlank()) { "공개 접근 키 해시는 비어 있을 수 없습니다." }
        this.accessKeyHash = newAccessKeyHash
        this.accessKey = newAccessKey
    }

    companion object {
        fun issue(event: Event, accessKeyHash: String, accessKey: String? = null): PublicAccess {
            require(accessKeyHash.isNotBlank()) { "공개 접근 키는 비어있을 수 없습니다" }
            return PublicAccess(event, accessKeyHash, accessKey)
        }
    }
}
