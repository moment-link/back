package com.momentlink.api.auth.port.out

import com.momentlink.domain.user.LoginProvider
import java.time.Duration

interface OAuthLoginStateStore {

    val ttl: Duration

    fun issue(provider: LoginProvider): String

    /**
     * 저장된 provider를 반환하며 state를 원자적으로 삭제.
     * 존재하지 않거나 소비된 state는 null을 반환
     */
    fun validateAndConsume(provider: String, state: String?, cookieState: String?)
}