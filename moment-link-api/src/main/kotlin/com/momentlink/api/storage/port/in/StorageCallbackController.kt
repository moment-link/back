package com.momentlink.api.storage.port.`in`

import com.momentlink.api.auth.common.BusinessException
import com.momentlink.api.common.web.ErrorCode
import com.momentlink.api.storage.application.StorageProperties
import com.momentlink.api.storage.application.StorageService
import io.swagger.v3.oas.annotations.Operation
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CookieValue
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

@RestController
@RequestMapping("/api/v1/storage/callback")
class StorageCallbackController(
    private val storageService: StorageService,
    private val properties: StorageProperties,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Operation(
        summary = "저장소 OAuth 콜백",
        description = "Google 인증 후 브라우저가 호출합니다. state 오류는 400, 그 외 결과는 프론트 저장소 화면으로 302 이동합니다.",
    )
    @GetMapping("/{provider}")
    fun callback(
        @PathVariable provider: String,
        @RequestParam(required = false) code: String?,
        @RequestParam(required = false) state: String?,
        @RequestParam(required = false) error: String?,
        @CookieValue(name = STORAGE_STATE_COOKIE_NAME, required = false) cookieState: String?,
        response: HttpServletResponse,
    ): ResponseEntity<Void> {
        // 성공 여부와 상관없이 state 쿠키는 지운다.
        response.addHeader(HttpHeaders.SET_COOKIE, StorageStateCookie.expire().toString())

        // state가 틀리면 어느 행사인지 알 수 없으므로 리다이렉트하지 않고 400으로 끝낸다.
        val connectState = storageService.consumeState(provider, state, cookieState)

        val failure = try {
            storageService.completeConnection(connectState, code, error)
            null
        } catch (ex: BusinessException) {
            log.warn("[{}] 저장소 콜백 실패: eventId={} - {}", ex.errorCode.code, connectState.eventId, ex.reason ?: ex.errorCode.message)
            ex.errorCode
        }
        return ResponseEntity.status(HttpStatus.FOUND)
            .location(redirectUri(connectState.eventId, failure))
            .build()
    }

    private fun redirectUri(eventId: Long, failure: ErrorCode?): URI =
        UriComponentsBuilder.fromUriString(properties.frontendRedirectUrl)
            .queryParam("eventId", eventId)
            .queryParam("result", if (failure == null) "connected" else "failed")
            .apply { failure?.let { queryParam("error", it.code) } }
            .build()
            .toUri()
}
