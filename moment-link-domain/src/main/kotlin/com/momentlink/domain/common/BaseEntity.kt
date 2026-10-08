package com.momentlink.domain.common

import jakarta.persistence.Column
import jakarta.persistence.MappedSuperclass
import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import java.time.Instant

@MappedSuperclass
abstract class BaseEntity {

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant? = Instant.now()
        protected set // setter의 접근범위를 protected로 설정하여, 상속받은 클래스에서는 값을 변경할 수 있도록 함 (생성자 매개변수에는 선언 불가능)

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant? = Instant.now()
        protected set


    @PrePersist
    protected fun prePersist() {
        val now = Instant.now()
        createdAt = now
        updatedAt = now
    }

    @PreUpdate
    protected fun preUpdate() {
        updatedAt = Instant.now()
    }
}