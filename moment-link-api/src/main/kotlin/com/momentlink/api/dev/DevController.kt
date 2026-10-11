package com.momentlink.api.dev

import com.momentlink.api.common.web.CommonResponse
import com.momentlink.api.common.web.CommonResponse.Companion.success
import io.swagger.v3.oas.annotations.Operation
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/dev")
class DevController(private val devEventService: DevEventService) {

    @Operation(summary = "개발용 참가 행사 발급", description = "인증·요청 본문 없이 테스트 행사와 공유 링크를 발급합니다.")
    @PostMapping("/events")
    fun createEvent(): CommonResponse<DevEventResDto> = success(devEventService.createEvent())
}
