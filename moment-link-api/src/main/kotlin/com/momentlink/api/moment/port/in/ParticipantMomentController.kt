package com.momentlink.api.moment.port.`in`

import com.momentlink.api.common.web.CommonResponse
import com.momentlink.api.common.web.CommonResponse.Companion.success
import com.momentlink.api.moment.application.MomentService
import com.momentlink.api.moment.dto.MomentListResDto
import io.swagger.v3.oas.annotations.Operation
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/participant/{eventId}/moments")
class ParticipantMomentController(
    private val momentService: MomentService,
) {

    @Operation(
        summary = "참가자 모먼트 목록",
        description = "participant_token 쿠키로 현재 행사의 모먼트 전체를 sortOrder, momentId 오름차순으로 조회합니다.",
    )
    @GetMapping
    fun getMoments(@PathVariable eventId: Long): CommonResponse<MomentListResDto>
        = success(momentService.getParticipantMoments())
}
