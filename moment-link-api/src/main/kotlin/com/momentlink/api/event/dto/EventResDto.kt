package com.momentlink.api.event.dto

import com.momentlink.domain.event.Event
import com.momentlink.domain.event.EventStatus
import com.momentlink.domain.event.PublicAccessStatus
import org.springframework.data.domain.Slice
import java.time.Instant


data class EventListResDto (
    val items: List<EventListItemResDto>,
    val nextPage: Int?,
    val hasNext: Boolean
) {
    companion object {
        fun from(slice: Slice<Event>): EventListResDto {
            return EventListResDto(
                items = slice.content.map(EventListItemResDto::from),
                nextPage = if (slice.hasNext()) { slice.number + 1} else { null },
                hasNext = slice.hasNext(),
            )
        }
    }
}

data class EventListItemResDto(
    val id: Long,
    val name: String,
    val startsAt: Instant,
    val endsAt: Instant,
    val status: EventStatus,
)  {
    companion object {
        fun from(event: Event): EventListItemResDto {
            return EventListItemResDto(
                id = event.requiredId(),
                name = event.name,
                startsAt = event.startsAt,
                endsAt = event.endsAt,
                status = event.status,
            )
        }
    }
}

data class EventResDto (
    val id: Long,
    val name: String,
    val startsAt: Instant,
    val endsAt: Instant,
    val status: EventStatus,
) {
    companion object {
        fun from(event: Event): EventResDto {
            return EventResDto(
                id = event.requiredId(),
                name = event.name,
                startsAt = event.startsAt,
                endsAt = event.endsAt,
                status = event.status,
            )
        }
    }
}

data class EventCreateResDto (
    val id: Long,
    val name: String,
    val startsAt: Instant,
    val endsAt: Instant,
    val access: PublicAccessResDto
) {
    companion object {
        fun from(savedEvent: Event, accessKey: String, shareUrl: String): EventCreateResDto {
            return EventCreateResDto(
                id = savedEvent.requiredId(),
                name = savedEvent.name,
                startsAt = savedEvent.startsAt,
                endsAt = savedEvent.endsAt,
                access = PublicAccessResDto(
                    accessKey = accessKey,
                    shareUrl = shareUrl
                )
            )
        }
    }
}

data class PublicAccessResDto (
    val accessKey: String,
    val shareUrl: String,
)

data class EventUpdateResDto (
    val id: Long,
    val name: String,
    val startsAt: Instant,
    val endsAt: Instant,
) {
    companion object {
        fun from(event: Event): EventUpdateResDto {
            return EventUpdateResDto(
                id = event.requiredId(),
                name = event.name,
                startsAt = event.startsAt,
                endsAt = event.endsAt
            )
        }
    }
}

data class EventCloseResDto (
    val id: Long,
    val status: EventStatus,
) {
    companion object {
        fun from(event: Event): EventCloseResDto {
            return EventCloseResDto(
                event.requiredId(),
                event.status
            )
        }
    }
}

data class EventAccessResDto (
    val status: PublicAccessStatus,
    val accessKey: String,
    val shareUrl: String,
) {
    companion object {
        fun from(event: Event, accessKey: String, shareUrl: String): EventAccessResDto {
            return EventAccessResDto(event.publicAccess.status, accessKey, shareUrl)
        }
    }
}

data class EventAccessEnableResDto (
 val id: Long,
) {
    companion object {
        fun from(event: Event): EventAccessEnableResDto {
            return EventAccessEnableResDto(event.requiredId())
        }
    }
}

data class EventAccessDisableResDto (
    val id: Long,
) {
    companion object {
        fun from(event: Event): EventAccessDisableResDto {
            return EventAccessDisableResDto(event.requiredId())
        }
    }
}
