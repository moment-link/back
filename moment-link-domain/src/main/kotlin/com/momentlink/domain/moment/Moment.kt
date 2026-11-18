package com.momentlink.domain.moment

import com.momentlink.domain.common.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "moments",
    indexes = [
        Index(name = "idx_moment_event_time", columnList = "event_Id,starts_at,ends_at",),
    ]
)
class Moment private constructor(
    eventId: Long,
    title: String,
    place: String,
    startsAt: Instant,
    endsAt: Instant,
    sortOrder: Int,
): BaseEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set

    @Column(name = "event_id", nullable = false, updatable = false)
    var eventId: Long = eventId
        protected set

    @Column(name = "title", nullable = false, length = 200)
    var title: String = title.trim()
        protected set

    @Column(name = "place", length = 200)
    var place: String = place
        protected set

    @Column(name = "starts_at", nullable = false)
    var startsAt: Instant = startsAt
        protected set

    @Column(name = "ends_at", nullable = false)
    var endsAt: Instant = endsAt
        protected set

    @Column(name = "sort_order", nullable = false)
    var sortOrder = sortOrder
        protected set

    init {
        validate(this.title, startsAt, endsAt, sortOrder)
    }

    fun requiredId(): Long =
        checkNotNull(id) { "영속화되지 않은 Moment입니다." }

    fun change(title: String, place: String, startsAt: Instant, endsAt: Instant, sortOrder: Int) {
        validate(title, startsAt, endsAt, sortOrder)
        this.title = title.trim()
        this.place = place
        this.startsAt = startsAt
        this.endsAt = endsAt
        this.sortOrder = sortOrder
    }

    // 서비스가 먼저 400으로 거르고, 여기서는 불변식만 지킨다.
    private fun validate(title: String, startsAt: Instant, endsAt: Instant, sortOrder: Int) {
        require(title.isNotBlank()) { "모먼트 제목은 비어 있을 수 없습니다." }
        require(endsAt.isAfter(startsAt)) { "모먼트 종료 시각은 시작 시각보다 뒤여야 합니다." }
        require(sortOrder >= 0) { "정렬 순서는 0 이상이어야 합니다." }
    }

    companion object {
        fun create(
            eventId: Long,
            title: String,
            place: String,
            startsAt: Instant,
            endsAt: Instant,
            sortOrder: Int,
        ): Moment = Moment(
            eventId = eventId,
            title = title,
            place = place,
            startsAt = startsAt,
            endsAt = endsAt,
            sortOrder = sortOrder
        )
    }
}