package com.momentlink.api.media.port.`in`

import com.momentlink.api.common.web.CommonResponse
import com.momentlink.api.common.web.CommonResponse.Companion.success
import com.momentlink.api.media.application.MediaUploadService
import com.momentlink.api.media.dto.CompleteUploadResDto
import com.momentlink.api.media.dto.PrepareUploadReqDto
import com.momentlink.api.media.dto.PrepareUploadResDto
import io.swagger.v3.oas.annotations.Operation
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
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
}
