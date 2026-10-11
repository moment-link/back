package com.momentlink.api.event.dto

import java.time.Instant


data class EventCreateReqDto (
    val name: String,
    val startsAt: Instant,
    val endsAt: Instant,
)

data class EventUpdateReqDto (
    val name: String?,
    val startsAt: Instant?,
    val endsAt: Instant?,
)

data class PublicEventReqDto(
    val accessKey: String,
)