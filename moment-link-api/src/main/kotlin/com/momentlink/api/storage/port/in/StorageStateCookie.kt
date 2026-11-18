package com.momentlink.api.storage.port.`in`

import org.springframework.http.ResponseCookie
import java.time.Duration

// @CookieValue의 이름에는 컴파일 타임 상수가 필요하다.
const val STORAGE_STATE_COOKIE_NAME = "storage_connect_state"

// 연결 요청은 XHR이라 cross-site 응답으로 쿠키가 온다. SameSite=Lax면 브라우저가 저장하지 않아 None을 쓴다.
object StorageStateCookie {
    fun create(state: String, maxAge: Duration): ResponseCookie = build(state, maxAge)

    fun expire(): ResponseCookie = build("", Duration.ZERO)

    private fun build(value: String, maxAge: Duration): ResponseCookie =
        ResponseCookie.from(STORAGE_STATE_COOKIE_NAME, value)
            .httpOnly(true)
            .secure(true)
            .sameSite("None")
            .path("/api/v1/storage/callback")
            .maxAge(maxAge)
            .build()
}
