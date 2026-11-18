package com.momentlink.api.storage.port.`in`

import com.momentlink.api.common.web.CommonResponse
import com.momentlink.api.common.web.CommonResponse.Companion.success
import com.momentlink.api.storage.application.StorageService
import com.momentlink.api.storage.dto.StorageConnectReqDto
import com.momentlink.api.storage.dto.StorageConnectResDto
import com.momentlink.api.storage.dto.StorageDisconnectResDto
import com.momentlink.api.storage.dto.StorageStatusResDto
import io.swagger.v3.oas.annotations.Operation
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Duration
import java.time.Instant

@RestController
@RequestMapping("/api/v1/events/{eventId}/storage")
class StorageController(
    private val storageService: StorageService,
) {

    @Operation(
        summary = "저장소 연결 URL 발급",
        description = "GOOGLE_DRIVE 연결용 인증 URL과 일회용 state 쿠키를 발급합니다. 프론트는 authorizationUrl로 브라우저를 이동합니다. 재인증도 같은 API를 사용합니다.",
    )
    @PostMapping("/connect")
    fun connect(
        @PathVariable eventId: Long,
        @RequestBody req: StorageConnectReqDto,
    ): ResponseEntity<CommonResponse<StorageConnectResDto>> {
        val result = storageService.connect(eventId, req)
        val cookie = StorageStateCookie.create(result.state, Duration.between(Instant.now(), result.expiresAt))
        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, cookie.toString())
            .body(success(StorageConnectResDto(result.authorizationUrl, result.expiresAt)))
    }

    @Operation(
        summary = "저장소 연결 상태 조회",
        description = "행사에 연결된 저장소를 조회합니다. 연결 이력이 없으면 storage가 null입니다. 토큰은 응답에 포함하지 않습니다.",
    )
    @GetMapping
    fun getStatus(@PathVariable eventId: Long): CommonResponse<StorageStatusResDto>
        = success(storageService.getStatus(eventId))

    @Operation(
        summary = "저장소 연결 해제",
        description = "연결을 해제합니다. 연결이 없거나 이미 해제돼도 성공하며, 외부 저장소에 보낸 파일은 지우지 않습니다.",
    )
    @DeleteMapping
    fun disconnect(@PathVariable eventId: Long): CommonResponse<StorageDisconnectResDto>
        = success(storageService.disconnect(eventId))
}
