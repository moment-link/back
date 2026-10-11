package com.momentlink.api.participant.port.`in`

import io.swagger.v3.oas.annotations.Operation
import com.momentlink.api.common.web.PARTICIPANT_TOKEN_COOKIE_NAME
import com.momentlink.api.common.web.TokenCookie
import com.momentlink.api.common.web.TokenCookieProvider
import com.momentlink.api.participant.application.ParticipantSessionAuthService
import com.momentlink.api.participant.common.ParticipantSessionProvider
import com.momentlink.api.participant.dto.SessionResDto
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CookieValue
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController


@RestController
@RequestMapping("/api/v1/participant/{eventId}/session")
class ParticipantSessionController(
    private val participantSessionAuthService: ParticipantSessionAuthService,
    private val cookieProvider: TokenCookieProvider,
    private val sessionProvider: ParticipantSessionProvider
) {

    /**
     * 헤더의 값을 검증하여,
     * 해당 행사에서만 사용가능한 업로드 세션 토큰 발급
     */
    @Operation(
        summary = "참가자 세션 발급·재사용",
        description = "X-Event-Access-Key 헤더로 행사 접근 권한을 확인하고 12시간 유효한 참가자 쿠키를 발급합니다. 유효한 동일 행사 세션은 만료 연장 없이 재사용하며, 본문 없이 쿠키를 포함해 요청합니다.",
    )
    @PostMapping
    fun participantSession(
        @PathVariable eventId: Long,
        @RequestHeader("X-Event-Access-Key") accessKey: String,
        @CookieValue(value = PARTICIPANT_TOKEN_COOKIE_NAME,
            required = false) existingToken: String?,): ResponseEntity<SessionResDto> {
        val session = participantSessionAuthService.getOrCreateSession(eventId, accessKey, existingToken)

        val response = ResponseEntity.ok()

        session.newToken?.let { token ->
            val cookie = cookieProvider.create(TokenCookie.PARTICIPANT_TOKEN, token, session.expiresAt, session.eventId)
            response.header(HttpHeaders.SET_COOKIE, cookie.toString())
        }

        return response.body(
            SessionResDto(
                sessionId = session.sessionId,
                eventId = eventId,
                expiresAt = session.expiresAt,
            )
        )
    }

    @Operation(
        summary = "현재 참가자 세션 조회",
        description = "participant_token 쿠키로 현재 행사의 세션 정보를 조회합니다. 접근 키와 Bearer 토큰은 필요 없으며, 세션 만료 시각을 연장하지 않습니다.",
    )
    @GetMapping
    fun getSession(
        @PathVariable eventId: Long,
    ): ResponseEntity<SessionResDto> {
        val participantSession = sessionProvider.get()

        return ResponseEntity.ok(
            SessionResDto(
                sessionId = participantSession.sessionId,
                eventId = participantSession.eventId,
                expiresAt = participantSession.expiresAt
            )
        )
    }
}