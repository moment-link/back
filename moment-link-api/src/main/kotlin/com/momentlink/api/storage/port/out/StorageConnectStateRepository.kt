package com.momentlink.api.storage.port.out

import com.momentlink.domain.storage.StorageConnectState
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

interface StorageConnectStateRepository : JpaRepository<StorageConnectState, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM StorageConnectState s WHERE s.state = :state")
    fun findByStateForUpdate(state: String): StorageConnectState?

    @Transactional
    @Modifying
    @Query("DELETE FROM StorageConnectState s WHERE s.expiresAt <= :now")
    fun deleteExpired(now: Instant): Int
}
