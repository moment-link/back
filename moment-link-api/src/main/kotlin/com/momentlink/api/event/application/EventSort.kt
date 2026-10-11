package com.momentlink.api.event.application

import org.springframework.data.domain.Sort

enum class EventSort {
    LATEST,
    OLDEST,
    NAME;

    fun toSort(): Sort =
        when (this) {
            LATEST -> Sort.by(Sort.Order.desc("startsAt"))
            OLDEST -> Sort.by(Sort.Order.asc("startsAt"))
            NAME -> Sort.by(Sort.Order.asc("name"), Sort.Order.asc("startsAt"))
        }
}
