package com.momentlink.api.event.port.out

import com.momentlink.domain.event.Event
import com.momentlink.domain.event.EventStatus
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
interface EventRepository: JpaRepository<Event, Long> {
    fun findByIdAndUserId(id: Long, userId: Long): Event?
    fun findAllByUserId(userId: Long, pageable: Pageable): Slice<Event>
    fun findAllByUserIdAndStatus(userId: Long, status: EventStatus, pageable: Pageable): Slice<Event>

    // 종료 전에 준비한 업로드가 종료 후 완료돼도 기록한다. 동시 완료는 먼저 커밋한 쪽이 남는다.
    @Modifying
    @Query("UPDATE Event e SET e.firstUploadAt = :uploadedAt WHERE e.id = :eventId AND e.firstUploadAt IS NULL")
    fun recordFirstUploadIfAbsent(eventId: Long, uploadedAt: Instant): Int
}
