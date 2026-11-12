package com.momentlink.api.auth.port.out

import com.momentlink.domain.user.LoginProvider
import java.time.Duration

interface OAuthLoginStateStore {

    val ttl: Duration

    fun issue(provider: LoginProvider): String

    /**
     * state를 한 번만 소비하고 provider와 만료 시간을 검증한다.
     * 유효하지 않은 state는 INVALID_OAUTH_STATE 예외를 발생시킨다.
     */
    fun validateAndConsume(provider: String, state: String?, cookieState: String?)
}
