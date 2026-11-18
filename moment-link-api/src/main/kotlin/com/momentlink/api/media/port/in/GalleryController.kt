package com.momentlink.api.media.port.`in`

import com.momentlink.api.common.web.CommonResponse
import com.momentlink.api.common.web.CommonResponse.Companion.success
import com.momentlink.api.common.web.SliceResDto
import com.momentlink.api.media.application.GallerySort
import com.momentlink.api.media.application.GalleryService
import com.momentlink.api.media.dto.GalleryDetailResDto
import com.momentlink.api.media.dto.GalleryItemResDto
import com.momentlink.api.media.dto.LikeResDto
import com.momentlink.domain.media.MediaType
import io.swagger.v3.oas.annotations.Operation
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/participant/{eventId}/media")
class GalleryController(
    private val galleryService: GalleryService,
) {

    @Operation(
        summary = "갤러리 목록 조회",
        description = "승인된 미디어만 조회합니다. sort는 LATEST·OLDEST(업로드 완료 시각)·POPULAR(좋아요 수)이며 momentId는 촬영 시각으로 분류합니다.",
    )
    @GetMapping
    fun getGallery(
        @PathVariable eventId: Long,
        @RequestParam(required = false) mediaType: MediaType?,
        @RequestParam(required = false) momentId: Long?,
        @RequestParam(required = false, defaultValue = "0") page: Int,
        @RequestParam(required = false, defaultValue = "20") limit: Int,
        @RequestParam(required = false, defaultValue = "LATEST") sort: GallerySort,
    ): CommonResponse<SliceResDto<GalleryItemResDto>>
        = success(galleryService.getGallery(mediaType, momentId, page, limit, sort))

    @Operation(
        summary = "갤러리 상세 조회",
        description = "승인 미디어는 행사 참가자 모두, 그 외 상태는 업로더 본인만 조회합니다. 유효기간이 있는 열람 URL을 반환합니다.",
    )
    @GetMapping("/{mediaId}")
    fun getGalleryDetail(
        @PathVariable eventId: Long,
        @PathVariable mediaId: Long,
    ): CommonResponse<GalleryDetailResDto>
        = success(galleryService.getGalleryDetail(mediaId))

    @Operation(
        summary = "좋아요 등록",
        description = "공개된 미디어에 좋아요를 등록합니다. 반복 등록해도 세션당 하나만 유지합니다.",
    )
    @PutMapping("/{mediaId}/like")
    fun like(
        @PathVariable eventId: Long,
        @PathVariable mediaId: Long,
    ): CommonResponse<LikeResDto>
        = success(galleryService.like(mediaId))

    @Operation(
        summary = "좋아요 취소",
        description = "등록하지 않은 좋아요 취소도 성공합니다. 숨김 처리된 미디어도 취소할 수 있습니다.",
    )
    @DeleteMapping("/{mediaId}/like")
    fun unlike(
        @PathVariable eventId: Long,
        @PathVariable mediaId: Long,
    ): CommonResponse<LikeResDto>
        = success(galleryService.unlike(mediaId))
}
