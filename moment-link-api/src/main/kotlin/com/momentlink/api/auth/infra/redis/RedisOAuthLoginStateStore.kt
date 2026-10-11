package com.momentlink.api.auth.infra.redis

import com.momentlink.api.auth.port.out.OAuthLoginStateStore
import com.momentlink.api.auth.common.BusinessException
import com.momentlink.api.common.web.ErrorCode
import com.momentlink.domain.user.LoginProvider
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import java.security.SecureRandom
import java.time.Duration
import java.util.Base64

@Component
class RedisOAuthLoginStateStore(
    private val redis: StringRedisTemplate,
): OAuthLoginStateStore {

    private val random = SecureRandom()

    override val ttl: Duration = Duration.ofMinutes(5)

    override fun issue(provider: LoginProvider): String {
        val bytes = ByteArray(32)
        random.nextBytes(bytes)

        val state = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
        redis.opsForValue().set(key(state), provider.name, ttl)
        return state
    }

    override fun validateAndConsume(provider: String, state: String?, cookieState: String?) {
        if (state.isNullOrBlank() || cookieState.isNullOrBlank() || state != cookieState) {
            throw BusinessException(ErrorCode.INVALID_OAUTH_STATE)
        }

        val storedProvider = redis.opsForValue().getAndDelete(key(state))

        if (storedProvider != provider) {
            throw BusinessException(ErrorCode.INVALID_OAUTH_STATE)
        }
    }

    private fun key(state: String): String = "oauth:login:$state"

}
