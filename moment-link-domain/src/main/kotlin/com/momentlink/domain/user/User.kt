package com.momentlink.domain.user

import com.momentlink.domain.common.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "users")
class User private constructor(
    email: String, // email로만, user객체를 생성할 수 있음
    loginType: LoginProvider,
    role: UserRole,
): BaseEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set

    @Column(unique = true, nullable = false, updatable = false)
    var email: String = email.lowercase().trim()
        protected set

    @Column(nullable = false)
    var name: String = "" // TODO 랜덤 이름 생성해서 넣기

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    var status: UserStatus = UserStatus.ACTIVE
        protected set

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    var loginType: LoginProvider = loginType
        protected set

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    var role: UserRole = role
        protected set

    // 생성자로
    init {
        require(this.email.isNotBlank()) { "이메일을 비어있을 수 없습니다" }
    }

    fun requiredId(): Long
        = checkNotNull(this.id) { "영속화되지 않은 객체입니다." }

    fun suspend() {
        check(status == UserStatus.ACTIVE) { "활성 사용자만 정지할 수 있습니다"}
        status = UserStatus.SUSPENDED
    }

    fun activate() {
        check(status == UserStatus.SUSPENDED) { "정지된 사용자만 활성화할 수 있습니다" }
        status = UserStatus.ACTIVE
    }

    fun withdraw() {
        check(status != UserStatus.WITHDRAWN) { "이미 탈퇴한 사용자입니다" }
        status = UserStatus.WITHDRAWN
    }

    fun updateName(name: String) {
        this.name = name
    }

    companion object {
        fun create(email: String, loginProvider: LoginProvider): User = User(email, loginProvider, UserRole.USER)
    }
}
