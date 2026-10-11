package com.momentlink.api.event.port.out

import com.momentlink.domain.event.Event
import com.momentlink.domain.event.EventStatus
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface EventRepository: JpaRepository<Event, Long> {
    fun findByIdAndUserId(id: Long, userId: Long): Event?
    fun findAllByUserId(userId: Long, pageable: Pageable): Slice<Event>
    fun findAllByUserIdAndStatus(userId: Long, status: EventStatus, pageable: Pageable): Slice<Event>

}