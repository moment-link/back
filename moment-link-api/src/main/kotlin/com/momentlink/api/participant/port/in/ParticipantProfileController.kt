package com.momentlink.api.participant.port.`in`

import com.momentlink.api.common.web.CommonResponse
import com.momentlink.api.common.web.CommonResponse.Companion.success
import com.momentlink.api.participant.application.ParticipantSessionProfileService
import com.momentlink.api.participant.dto.DisplayNameUpdateReqDto
import com.momentlink.api.participant.dto.DisplayNameUpdateResDto
import com.momentlink.api.participant.dto.ParticipantProfileResDto
import io.swagger.v3.oas.annotations.Operation
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/participant/{eventId}/me")
class ParticipantProfileController(
    private val participantProfileService: ParticipantSessionProfileService,
) {

    @Operation(
        summary = "내 참가자 정보 조회",
        description = "participant_token 쿠키로 현재 세션의 닉네임과 역할을 조회합니다. 세션 만료 시각을 연장하지 않습니다.",
    )
    @GetMapping
    fun getMe(@PathVariable eventId: Long): CommonResponse<ParticipantProfileResDto>
        = success(participantProfileService.getMe())

    @Operation(
        summary = "내 닉네임 변경",
        description = "앞뒤 공백을 제거한 2~20자 닉네임으로 변경합니다. 같은 행사 안에서는 중복될 수 없고, 현재 닉네임과 같으면 성공합니다.",
    )
    @PatchMapping
    fun rename(
        @PathVariable eventId: Long,
        @RequestBody req: DisplayNameUpdateReqDto,
    ): CommonResponse<DisplayNameUpdateResDto>
        = success(participantProfileService.rename(req))
}
