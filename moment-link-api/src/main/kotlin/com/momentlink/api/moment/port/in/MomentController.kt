package com.momentlink.api.moment.port.`in`

import com.momentlink.api.common.web.CommonResponse
import com.momentlink.api.common.web.CommonResponse.Companion.success
import com.momentlink.api.moment.application.MomentService
import com.momentlink.api.moment.dto.MomentCreateReqDto
import com.momentlink.api.moment.dto.MomentDeleteResDto
import com.momentlink.api.moment.dto.MomentListResDto
import com.momentlink.api.moment.dto.MomentResDto
import com.momentlink.api.moment.dto.MomentUpdateReqDto
import io.swagger.v3.oas.annotations.Operation
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/events/{eventId}/moments")
class MomentController(
    private val momentService: MomentService,
) {

    @Operation(
        summary = "주최자 모먼트 목록",
        description = "본인 소유 행사의 모먼트 전체를 sortOrder, momentId 오름차순으로 조회합니다.",
    )
    @GetMapping
    fun getMoments(@PathVariable eventId: Long): CommonResponse<MomentListResDto>
        = success(momentService.getMoments(eventId))

    @Operation(
        summary = "모먼트 생성",
        description = "OPEN 행사에 행사 기간 안의 모먼트를 생성합니다. place와 sortOrder는 생략할 수 있습니다.",
    )
    @PostMapping
    fun createMoment(
        @PathVariable eventId: Long,
        @RequestBody req: MomentCreateReqDto,
    ): CommonResponse<MomentResDto>
        = success(momentService.createMoment(eventId, req))

    @Operation(
        summary = "모먼트 수정",
        description = "변경할 필드만 전달합니다. 명시적 null은 허용하지 않으며 기존 값과 합친 최종 상태로 검증합니다.",
    )
    @PatchMapping("/{momentId}")
    fun updateMoment(
        @PathVariable eventId: Long,
        @PathVariable momentId: Long,
        @RequestBody req: MomentUpdateReqDto,
    ): CommonResponse<MomentResDto>
        = success(momentService.updateMoment(eventId, momentId, req))

    @Operation(
        summary = "모먼트 삭제",
        description = "OPEN 행사의 모먼트를 삭제합니다. 미디어는 삭제하지 않습니다.",
    )
    @DeleteMapping("/{momentId}")
    fun deleteMoment(
        @PathVariable eventId: Long,
        @PathVariable momentId: Long,
    ): CommonResponse<MomentDeleteResDto>
        = success(momentService.deleteMoment(eventId, momentId))
}
