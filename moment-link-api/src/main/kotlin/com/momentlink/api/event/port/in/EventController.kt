package com.momentlink.api.event.port.`in`

import io.swagger.v3.oas.annotations.Operation
import com.momentlink.api.common.web.CommonResponse
import com.momentlink.api.common.web.CommonResponse.Companion.success
import com.momentlink.api.event.application.EventService
import com.momentlink.api.event.application.EventSort
import com.momentlink.api.event.dto.EventAccessDisableResDto
import com.momentlink.api.event.dto.EventAccessEnableResDto
import com.momentlink.api.event.dto.EventCloseResDto
import com.momentlink.api.event.dto.EventCreateReqDto
import com.momentlink.api.event.dto.EventCreateResDto
import com.momentlink.api.event.dto.EventListResDto
import com.momentlink.api.event.dto.EventResDto
import com.momentlink.api.event.dto.EventAccessResDto
import com.momentlink.api.event.dto.EventUpdateReqDto
import com.momentlink.api.event.dto.EventUpdateResDto
import com.momentlink.domain.event.EventStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/events")
class EventController(
    private val eventService: EventService,
) {

    /**
     * 행사 생성
     */
    @Operation(
        summary = "행사 생성",
        description = "호스트 Bearer 인증으로 행사를 생성하고 공유 키와 링크를 함께 발급합니다. name, startsAt, endsAt을 요청 본문에 전달합니다.",
    )
    @PostMapping
    fun createEvent(@RequestBody req: EventCreateReqDto)
    : CommonResponse<EventCreateResDto>
        = success(eventService.createEvent(req))

    /**
     * 행사 목록 조회
     */
    @Operation(
        summary = "내 행사 목록 조회",
        description = "현재 호스트 소유 행사를 조회합니다. page는 0부터 시작하며, limit은 1~100, sort는 LATEST·OLDEST·NAME입니다. status를 생략하면 전체 상태를 조회합니다.",
    )
    @GetMapping
    fun getEvents(
        @RequestParam(required = false) status: EventStatus?,
        @RequestParam(required = false, defaultValue = "0") page: Int,
        @RequestParam(required = false, defaultValue = "20") limit: Int,
        @RequestParam(required = false, defaultValue = "LATEST") sort: EventSort,
    ): CommonResponse<EventListResDto>
        = success(eventService.getEvents(page, limit, status, sort))

    /**
     * 행사 상세 조회
     */
    @Operation(
        summary = "행사 상세 조회",
        description = "호스트 Bearer 인증으로 본인 소유 행사의 상세 정보를 조회합니다. 공유 키와 링크는 공유 정보 조회 API에서 제공합니다.",
    )
    @GetMapping("/{eventId}")
    fun getEvent(@PathVariable eventId: Long): CommonResponse<EventResDto>
        = success(eventService.getEvent(eventId))


    /**
     * 행사 수정
     */
    @Operation(
        summary = "행사 기본 정보 변경",
        description = "호스트가 본인 소유 OPEN 행사의 이름이나 일정을 변경합니다. 변경할 필드만 전달하며, 일정 변경은 행사 시작 전에만 가능합니다.",
    )
    @PatchMapping("/{eventId}")
    fun updateEvent(@PathVariable eventId: Long,
                    @RequestBody event: EventUpdateReqDto
    ): CommonResponse<EventUpdateResDto>
        = success(eventService.updateEvent(eventId, event))

    /**
     * 행사 종료
     */
    @Operation(
        summary = "행사 종료",
        description = "호스트가 본인 소유 행사를 CLOSED로 변경합니다. 공개 접근은 자동으로 비활성화되지 않으므로 참가자 접근 차단은 별도로 요청해야 합니다.",
    )
    @PostMapping("/{eventId}/close")
    fun closeEvent(@PathVariable eventId: Long): CommonResponse<EventCloseResDto>
        = success(eventService.closeEvent(eventId))

    /**
     * 행사 공개 접근 활성화
     */
    @Operation(
        summary = "공개 접근 활성화",
        description = "호스트가 본인 소유 행사의 공개 접근을 활성화합니다. 기존 공유 키는 유지됩니다.",
    )
    @PostMapping("/{eventId}/access/enable")
    fun enableEvent(@PathVariable eventId: Long): CommonResponse<EventAccessEnableResDto>
        = success(eventService.enableEvent(eventId))

    /**
     * 행사 공개 접근 비활성화
     */
    @Operation(
        summary = "공개 접근 비활성화",
        description = "호스트가 본인 소유 행사의 공개 접근을 비활성화합니다. 공개 행사 조회와 참가자 인증을 차단하며 기존 공유 키는 유지됩니다.",
    )
    @PostMapping("/{eventId}/access/disable")
    fun disableEvent(@PathVariable eventId: Long): CommonResponse<EventAccessDisableResDto>
        = success(eventService.disableEvent(eventId))

    /** 호스트의 현재 공유 키 및 링크 조회 */
    @Operation(
        summary = "공유 정보 조회",
        description = "호스트가 본인 소유 행사의 기존 공유 키와 링크를 조회합니다. 키를 변경하지 않으며, 키 원문이 없는 이전 데이터는 재발급이 필요합니다.",
    )
    @GetMapping("/{eventId}/access")
    fun getAccess(@PathVariable eventId: Long): CommonResponse<EventAccessResDto>
        = success(eventService.getAccess(eventId))

    /**
     * 행사 공개 키 교체
     */
    @Operation(
        summary = "공유 키 재발급",
        description = "호스트가 본인 소유 OPEN 행사의 공유 키와 링크를 새로 발급합니다. 이전 키는 무효화되지만 기존 참가자 세션과 공개 접근 상태는 유지됩니다.",
    )
    @PostMapping("/{eventId}/access/rotate")
    fun rotateEvent(@PathVariable eventId: Long): CommonResponse<EventAccessResDto>
        = success(eventService.rotateEvent(eventId))


}
