package com.momentlink.api.event.port.`in`

import io.swagger.v3.oas.annotations.Operation
import com.momentlink.api.common.web.CommonResponse
import com.momentlink.api.common.web.CommonResponse.Companion.success
import com.momentlink.api.event.application.PublicEventService
import com.momentlink.api.event.dto.PublicEventResDto
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/events")
class PublicEventController(
    private val publicEventService: PublicEventService,
) {
    @Operation(
        summary = "접근 키로 공개 행사 조회",
        description = "공유 링크의 shareKey를 X-Event-Access-Key 헤더에 전달해 행사 정보를 조회합니다. 호스트 로그인은 필요 없으며, 응답 eventId를 참가자 세션 발급에 사용합니다.",
    )
    @GetMapping("/access")
    fun getPublicEvent(
        @RequestHeader("X-Event-Access-Key") accessKey: String,
    ): CommonResponse<PublicEventResDto> = success(publicEventService.getEventForPublicAccess(accessKey))
}
