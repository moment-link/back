package com.momentlink.api.storage.port.out

import com.momentlink.domain.storage.Storage
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface StorageRepository : JpaRepository<Storage, Long> {

    fun findByEventId(eventId: Long): Storage?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Storage s WHERE s.eventId = :eventId")
    fun findByEventIdForUpdate(eventId: Long): Storage?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Storage s WHERE s.id = :id")
    fun findByIdForUpdate(id: Long): Storage?
}
