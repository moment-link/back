package com.momentlink.api.common.web

// @CookieValue의 이름에는 컴파일 타임 상수가 필요하다.
const val REFRESH_TOKEN_COOKIE_NAME = "refresh_token"
const val PARTICIPANT_TOKEN_COOKIE_NAME = "participant_token"

enum class TokenCookie(val cookieName: String) {
    REFRESH_TOKEN(REFRESH_TOKEN_COOKIE_NAME),
    PARTICIPANT_TOKEN(PARTICIPANT_TOKEN_COOKIE_NAME);

    fun path(eventId: Long? = null): String = when (this) {
        REFRESH_TOKEN -> "/api/v1/auth"

        PARTICIPANT_TOKEN -> {
            requireNotNull(eventId) {
                "참가자 쿠키에는 eventId가 필요합니다."
            }

            "/api/v1/participant/$eventId"
        }
    }
}