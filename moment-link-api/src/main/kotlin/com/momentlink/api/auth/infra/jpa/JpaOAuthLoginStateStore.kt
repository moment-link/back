package com.momentlink.api.auth.infra.jpa

import com.momentlink.api.auth.common.BusinessException
import com.momentlink.api.auth.port.out.OAuthLoginStateRepository
import com.momentlink.api.auth.port.out.OAuthLoginStateStore
import com.momentlink.api.common.web.ErrorCode
import com.momentlink.domain.common.OAuthLoginState
import com.momentlink.domain.user.LoginProvider
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.security.SecureRandom
import java.time.Duration
import java.time.Instant
import java.util.Base64

@Component
class JpaOAuthLoginStateStore(
    private val repository: OAuthLoginStateRepository,
): OAuthLoginStateStore {

    private val random = SecureRandom()

    override val ttl: Duration = Duration.ofMinutes(5)

    override fun issue(provider: LoginProvider): String {
        val bytes = ByteArray(32)
        random.nextBytes(bytes)

        val state = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
        val now = Instant.now()
        repository.deleteExpired(now)
        repository.save(OAuthLoginState(state, provider, now.plus(ttl)))
        return state
    }

    // provider가 틀리거나 만료된 state도 읽은 즉시 소비한다.
    @Transactional(noRollbackFor = [BusinessException::class])
    override fun validateAndConsume(provider: String, state: String?, cookieState: String?) {
        if (state.isNullOrBlank() || cookieState.isNullOrBlank() || state != cookieState) {
            throw BusinessException(ErrorCode.INVALID_OAUTH_STATE)
        }

        val stored = repository.findByStateForUpdate(state)
            ?: throw BusinessException(ErrorCode.INVALID_OAUTH_STATE)
        repository.delete(stored)

        if (stored.state != state || stored.provider.name != provider || !stored.expiresAt.isAfter(Instant.now())) {
            throw BusinessException(ErrorCode.INVALID_OAUTH_STATE)
        }
    }
}
