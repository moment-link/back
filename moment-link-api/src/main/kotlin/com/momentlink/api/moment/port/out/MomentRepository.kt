package com.momentlink.api.moment.port.out

import com.momentlink.domain.moment.Moment
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
interface MomentRepository : JpaRepository<Moment, Long> {

    fun findAllByEventIdOrderBySortOrderAscIdAsc(eventId: Long): List<Moment>

    fun findByIdAndEventId(id: Long, eventId: Long): Moment?

    @Query(
        """
        SELECT CASE WHEN COUNT(m) > 0 THEN true ELSE false END
        FROM Moment m
        WHERE m.eventId = :eventId AND (m.startsAt < :startsAt OR m.endsAt > :endsAt)
    """
    )
    fun existsOutside(eventId: Long, startsAt: Instant, endsAt: Instant): Boolean
}
