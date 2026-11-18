package com.momentlink.api.media.port.`in`

import com.momentlink.api.common.web.CommonResponse
import com.momentlink.api.common.web.CommonResponse.Companion.success
import com.momentlink.api.common.web.SliceResDto
import com.momentlink.api.media.application.HostMediaService
import com.momentlink.api.media.application.MediaSort
import com.momentlink.api.media.dto.HostMediaDetailResDto
import com.momentlink.api.media.dto.HostMediaItemResDto
import com.momentlink.api.media.dto.MediaDeleteResDto
import com.momentlink.api.media.dto.PublicationChangeReqDto
import com.momentlink.api.media.dto.PublicationChangeResDto
import com.momentlink.domain.media.MediaType
import com.momentlink.domain.media.PublicationStatus
import com.momentlink.domain.participant.UploadStatus
import io.swagger.v3.oas.annotations.Operation
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/events/{eventId}/media")
class HostMediaController(
    private val hostMediaService: HostMediaService,
) {

    @Operation(
        summary = "행사 미디어 목록",
        description = "본인 소유 행사의 미디어를 공개 상태와 관계없이 조회합니다. 삭제된 미디어는 제외하며 momentId는 모먼트 시간대로 필터링합니다.",
    )
    @GetMapping
    fun getMedia(
        @PathVariable eventId: Long,
        @RequestParam(required = false) publicationStatus: PublicationStatus?,
        @RequestParam(required = false) uploadStatus: UploadStatus?,
        @RequestParam(required = false) mediaType: MediaType?,
        @RequestParam(required = false) momentId: Long?,
        @RequestParam(required = false, defaultValue = "0") page: Int,
        @RequestParam(required = false, defaultValue = "20") limit: Int,
        @RequestParam(required = false, defaultValue = "LATEST") sort: MediaSort,
    ): CommonResponse<SliceResDto<HostMediaItemResDto>>
        = success(hostMediaService.getMedia(eventId, publicationStatus, uploadStatus, mediaType, momentId, page, limit, sort))

    @Operation(
        summary = "행사 미디어 상세",
        description = "삭제되지 않은 미디어를 공개 상태와 관계없이 조회합니다. 업로드 완료 전에는 mediaUrl, urlExpiresAt, uploadedAt이 null입니다.",
    )
    @GetMapping("/{mediaId}")
    fun getMediaDetail(
        @PathVariable eventId: Long,
        @PathVariable mediaId: Long,
    ): CommonResponse<HostMediaDetailResDto>
        = success(hostMediaService.getMediaDetail(eventId, mediaId))

    @Operation(
        summary = "미디어 공개 상태 변경",
        description = "업로드 완료된 미디어의 공개 상태를 PENDING_REVIEW, APPROVED, REJECTED, HIDDEN 중 하나로 바꿉니다. 행사 종료 후에도 가능합니다.",
    )
    @PatchMapping("/{mediaId}/publication")
    fun changePublication(
        @PathVariable eventId: Long,
        @PathVariable mediaId: Long,
        @RequestBody req: PublicationChangeReqDto,
    ): CommonResponse<PublicationChangeResDto>
        = success(hostMediaService.changePublication(eventId, mediaId, req))

    @Operation(
        summary = "주최자 미디어 삭제",
        description = "행사 내 미디어를 논리 삭제합니다. 반복 삭제도 성공하며 외부 저장소로 이미 보낸 파일은 지우지 않습니다.",
    )
    @DeleteMapping("/{mediaId}")
    fun deleteMedia(
        @PathVariable eventId: Long,
        @PathVariable mediaId: Long,
    ): CommonResponse<MediaDeleteResDto>
        = success(hostMediaService.deleteMedia(eventId, mediaId))
}
