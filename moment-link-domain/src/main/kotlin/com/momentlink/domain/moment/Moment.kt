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
    var eventId: Long? = eventId
        protected set

    @Column(name = "title", nullable = false, length = 200)
    var title: String = title.trim()
        protected set

    @Column(name = "place", length = 200)
    var place: String = place
        protected set

    @Column(name = "starts_at", nullable = false)
    var startsAt: Instant? = startsAt
        protected set

    @Column(name = "ends_at", nullable = false)
    var endsAt: Instant? = endsAt
        protected set

    @Column(name = "sort_order", nullable = false)
    var sortOrder = sortOrder
        protected set

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