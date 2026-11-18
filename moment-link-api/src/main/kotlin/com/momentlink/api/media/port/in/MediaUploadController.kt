package com.momentlink.api.media.port.`in`

import com.momentlink.api.common.web.CommonResponse
import com.momentlink.api.common.web.CommonResponse.Companion.success
import com.momentlink.api.common.web.SliceResDto
import com.momentlink.api.media.application.MediaSort
import com.momentlink.api.media.application.MediaUploadService
import com.momentlink.api.media.dto.CancelUploadResDto
import com.momentlink.api.media.dto.CompleteUploadResDto
import com.momentlink.api.media.dto.MediaDeleteResDto
import com.momentlink.api.media.dto.MyMediaItemResDto
import com.momentlink.api.media.dto.PrepareUploadReqDto
import com.momentlink.api.media.dto.PrepareUploadResDto
import com.momentlink.domain.media.PublicationStatus
import com.momentlink.domain.participant.UploadStatus
import io.swagger.v3.oas.annotations.Operation
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/participant/{eventId}/media")
class MediaUploadController(
    private val mediaUploadService: MediaUploadService,
) {

    @Operation(
        summary = "업로드 준비",
        description = "파일 정보와 Idempotency-Key로 10분 유효한 S3 업로드 URL을 발급합니다. 반환된 headers를 그대로 붙여 파일을 PUT한 뒤 완료 API를 호출합니다.",
    )
    @PostMapping("/prepare")
    fun prepare(
        @PathVariable eventId: Long,
        @RequestHeader("Idempotency-Key") idempotencyKey: String,
        @RequestBody req: PrepareUploadReqDto,
    ): CommonResponse<PrepareUploadResDto>
        = success(mediaUploadService.prepare(idempotencyKey, req))

    @Operation(
        summary = "업로드 완료",
        description = "S3에 올라간 파일의 크기와 SHA-256을 확인한 뒤 UPLOADED로 변경합니다. 같은 Idempotency-Key 재요청은 기존 결과를 반환합니다.",
    )
    @PostMapping("/{mediaId}/complete")
    fun complete(
        @PathVariable eventId: Long,
        @PathVariable mediaId: Long,
        @RequestHeader("Idempotency-Key") idempotencyKey: String,
    ): CommonResponse<CompleteUploadResDto>
        = success(mediaUploadService.complete(mediaId, idempotencyKey))

    @Operation(
        summary = "업로드 취소",
        description = "현재 세션 소유 PENDING 업로드를 취소합니다. 이미 취소됐으면 성공합니다. 완료된 미디어는 삭제 API를 사용합니다.",
    )
    @PostMapping("/{mediaId}/cancel")
    fun cancel(
        @PathVariable eventId: Long,
        @PathVariable mediaId: Long,
    ): CommonResponse<CancelUploadResDto>
        = success(mediaUploadService.cancel(mediaId))

    @Operation(
        summary = "내 미디어 삭제",
        description = "현재 세션 소유 미디어를 논리 삭제합니다. 반복 삭제도 성공하며 원본 파일은 비동기로 지웁니다.",
    )
    @DeleteMapping("/{mediaId}")
    fun deleteMine(
        @PathVariable eventId: Long,
        @PathVariable mediaId: Long,
    ): CommonResponse<MediaDeleteResDto>
        = success(mediaUploadService.deleteMine(mediaId))

    @Operation(
        summary = "내 업로드 목록",
        description = "현재 세션이 올린 미디어를 공개 상태와 관계없이 조회합니다. 삭제된 미디어는 제외하며, 정렬은 업로드 준비 시각 기준입니다.",
    )
    @GetMapping("/mine")
    fun getMine(
        @PathVariable eventId: Long,
        @RequestParam(required = false) uploadStatus: UploadStatus?,
        @RequestParam(required = false) publicationStatus: PublicationStatus?,
        @RequestParam(required = false, defaultValue = "0") page: Int,
        @RequestParam(required = false, defaultValue = "20") limit: Int,
        @RequestParam(required = false, defaultValue = "LATEST") sort: MediaSort,
    ): CommonResponse<SliceResDto<MyMediaItemResDto>>
        = success(mediaUploadService.getMine(uploadStatus, publicationStatus, page, limit, sort))
}
