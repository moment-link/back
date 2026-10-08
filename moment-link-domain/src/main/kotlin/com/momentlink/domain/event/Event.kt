package com.momentlink.domain.event

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "events")
class Event private constructor(
    userId: Long,
    name: String,
    startsAt: Instant,
    endsAt: Instant,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set

    @Column(name = "user_id", nullable = false, updatable = false)
    var userId: Long = userId
        protected set

    @Column(name = "name")
    var name: String = name.trim()
        protected set

    @Column(name = "starts_at", nullable = false)
    var startsAt: Instant = startsAt
        protected set

    @Column(name = "ends_at", nullable = false)
    var endsAt: Instant = endsAt
        protected set

    @Column(name = "first_upload_at")
    var firstUploadAt: Instant? = null
        protected set

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: EventStatus = EventStatus.OPEN
        protected set

    @Column(name = "participant_download_allowed", nullable = false)
    var participantDownloadAllowed: Boolean = false
        protected set

    @Column(name = "retention_days", nullable = false)
    var retentionDays: Int = 30
        protected set


    @Column(name = "closed_at")
    var closedAt: Instant? = null
        protected set

    @OneToOne(mappedBy = "event", cascade = [CascadeType.ALL], orphanRemoval = true, optional = false)
    lateinit var publicAccess: PublicAccess
        protected set

    init {
        require(name.isNotBlank()) { "행사명은 비어있을 수 없습니다." }
        require(endsAt.isAfter(startsAt)) { "종료 시각은 시작 시각보다 앞설 수 없습니다." }
    }

    fun requiredId(): Long
        = checkNotNull(this.id) { "영속화되지 않은 객체입니다." }

    // 이름 변경
    fun rename(name: String) {
        check(status == EventStatus.OPEN) { "종료된 행사는 수정할 수 없습니다." }
        require(name.isNotBlank()) { "행사명은 비어 있을 수 없습니다." }
        this.name = name.trim()
    }

    // 시작·종료시간 변경
    fun reschedule(startsAt: Instant, endsAt: Instant) {
        check(this.status == EventStatus.OPEN) { "종료된 행사는 수정할 수 없습니다." }
        check(Instant.now().isBefore(this.startsAt)) { "이미 시작한 행사는 일정을 변경할 수 없습니다." }
        require(endsAt.isAfter(startsAt)) { "종료시각은 시작시각보다 뒤여야 합니다." }

        this.startsAt = startsAt
        this.endsAt = endsAt
    }

    fun recordFirstUpload(uploadAt: Instant) {
        check(status == EventStatus.OPEN) { "종료된 행사에는 업로드할 수 없습니다." }
        if(firstUploadAt == null) {
            firstUploadAt = uploadAt
        }
    }

    fun rotateAccessKey(newAccessKey: String) {
        check(status == EventStatus.OPEN) { "종료된 행사의 공개 키는 교체할 수 없습니다." }
        this.publicAccess.rotate(newAccessKey)
    }

    fun isAccessible(now: Instant): Boolean {
        if(this.status == EventStatus.CLOSED) return false
        if(now.isBefore(this.startsAt) || now.isAfter(this.endsAt)) return false

        return true
    }

    fun close(now: Instant = Instant.now()) {
        check(status == EventStatus.OPEN) { "이미 종료된 행사입니다." }
        status = EventStatus.CLOSED
        closedAt = now
    }

    fun isClosed(): Boolean {
        return this.status == EventStatus.CLOSED
    }

    fun enablePublicAccess() {
        this.publicAccess.enable(Instant.now())
    }

    fun disablePublicAccess() {
        this.publicAccess.disable(Instant.now())
    }

    fun validateStorageChangeable(now: Instant) {
        check(status == EventStatus.OPEN && now.isBefore(startsAt)) {
            "시작된 행사의 저장소는 변경할 수 없습니다."
        }
    }

    fun changeParticipantDownloadAllowed(allowed: Boolean) {
        this.participantDownloadAllowed = allowed
    }

    companion object {
        fun create(userId: Long, name: String, startsAt: Instant, endsAt: Instant, accessKeyHash: String): Event {
            val event = Event(userId, name, startsAt, endsAt)
            event.publicAccess = PublicAccess.issue(event, accessKeyHash)
            return event
        }
    }
}
