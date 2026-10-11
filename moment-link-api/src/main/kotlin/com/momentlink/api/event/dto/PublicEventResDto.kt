package com.momentlink.api.event.dto

import com.momentlink.domain.event.Event
import java.time.Instant

data class PublicEventResDto(
    val eventId: Long,
    val name: String,
    val startsAt: Instant,
    val endsAt: Instant,
) {
    companion object {
        fun from(event: Event): PublicEventResDto =
            PublicEventResDto(event.requiredId(), event.name, event.startsAt, event.endsAt)
    }
}
