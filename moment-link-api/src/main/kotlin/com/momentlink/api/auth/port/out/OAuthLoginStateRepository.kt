package com.momentlink.api.auth.port.out

import com.momentlink.domain.common.OAuthLoginState
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

interface OAuthLoginStateRepository: JpaRepository<OAuthLoginState, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM OAuthLoginState s WHERE s.state = :state")
    fun findByStateForUpdate(state: String): OAuthLoginState?

    @Transactional
    @Modifying
    @Query("DELETE FROM OAuthLoginState s WHERE s.expiresAt <= :now")
    fun deleteExpired(now: Instant): Int
}
