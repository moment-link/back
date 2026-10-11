package com.momentlink.api.auth.port.out

import com.momentlink.domain.common.RefreshToken
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface RefreshTokenRepository: JpaRepository<RefreshToken, Long> {

    fun findByToken(token: String): RefreshToken?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT rt
        FROM RefreshToken rt
        WHERE rt.token = :token
    """)
    fun findByTokenForUpdate(token: String): RefreshToken?
    fun deleteByUserId(userId: Long)
}