package com.momentlink.api.storage.application

import com.momentlink.api.auth.common.BusinessException
import com.momentlink.api.common.security.key.KeyGenerator
import com.momentlink.api.common.web.ErrorCode
import com.momentlink.api.storage.port.out.StorageConnectStateRepository
import com.momentlink.domain.storage.StorageConnectState
import com.momentlink.domain.storage.StorageProvider
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.Instant

@Component
class StorageConnectStateStore(
    private val repository: StorageConnectStateRepository,
    private val keyGenerator: KeyGenerator,
) {
    val ttl: Duration = Duration.ofMinutes(5)

    @Transactional
    fun issue(userId: Long, eventId: Long, provider: StorageProvider): StorageConnectState {
        val now = Instant.now()
        repository.deleteExpired(now)
        return repository.save(StorageConnectState(keyGenerator.generateState(), userId, eventId, provider, now.plus(ttl)))
    }

    // provider가 틀리거나 만료된 state도 읽은 즉시 소비한다.
    @Transactional(noRollbackFor = [BusinessException::class])
    fun consume(provider: String, state: String?, cookieState: String?): StorageConnectState {
        if (state.isNullOrBlank() || cookieState.isNullOrBlank() || state != cookieState) {
            throw BusinessException(ErrorCode.INVALID_STORAGE_STATE, reason = "state 누락 또는 쿠키 불일치")
        }
        val stored = repository.findByStateForUpdate(state)
            ?: throw BusinessException(ErrorCode.INVALID_STORAGE_STATE, reason = "저장된 state 없음 (이미 사용됐거나 만료돼 정리됨)")
        repository.delete(stored)

        if (StorageProvider.resolve(provider) != stored.provider) {
            throw BusinessException(ErrorCode.INVALID_STORAGE_STATE, reason = "provider 불일치")
        }
        if (!stored.expiresAt.isAfter(Instant.now())) {
            throw BusinessException(ErrorCode.INVALID_STORAGE_STATE, reason = "만료된 state")
        }
        return stored
    }
}
