package com.momentlink.api.transfer.port.`in`

import com.momentlink.api.common.web.CommonResponse
import com.momentlink.api.common.web.CommonResponse.Companion.success
import com.momentlink.api.common.web.SliceResDto
import com.momentlink.api.transfer.application.TransferService
import com.momentlink.api.transfer.dto.TransferDetailResDto
import com.momentlink.api.transfer.dto.TransferItemResDto
import com.momentlink.api.transfer.dto.TransferRetryResDto
import com.momentlink.domain.media.TransferStatus
import io.swagger.v3.oas.annotations.Operation
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/events/{eventId}/transfers")
class TransferController(
    private val transferService: TransferService,
) {

    @Operation(
        summary = "외부 저장소 전송 목록",
        description = "최신 전송 작업부터 조회합니다. status는 PENDING, IN_PROGRESS, RETRY_WAIT, SUCCEEDED, PERMANENT_FAILED입니다.",
    )
    @GetMapping
    fun getTransfers(
        @PathVariable eventId: Long,
        @RequestParam(required = false) status: TransferStatus?,
        @RequestParam(required = false, defaultValue = "0") page: Int,
        @RequestParam(required = false, defaultValue = "20") limit: Int,
    ): CommonResponse<SliceResDto<TransferItemResDto>>
        = success(transferService.getTransfers(eventId, status, page, limit))

    @Operation(
        summary = "외부 저장소 전송 상세",
        description = "전송 시도 이력을 함께 조회합니다. 외부 제공자 원문 대신 정제된 안내 메시지를 반환합니다.",
    )
    @GetMapping("/{transferId}")
    fun getTransfer(
        @PathVariable eventId: Long,
        @PathVariable transferId: Long,
    ): CommonResponse<TransferDetailResDto>
        = success(transferService.getTransfer(eventId, transferId))

    @Operation(
        summary = "외부 저장소 전송 수동 재시도",
        description = "RETRY_WAIT, PERMANENT_FAILED 작업을 대기열에 다시 넣습니다. 응답은 접수(202)이며 전송 완료를 의미하지 않습니다.",
    )
    @PostMapping("/{transferId}/retry")
    fun retry(
        @PathVariable eventId: Long,
        @PathVariable transferId: Long,
    ): ResponseEntity<CommonResponse<TransferRetryResDto>> =
        ResponseEntity.status(HttpStatus.ACCEPTED).body(success(transferService.retry(eventId, transferId)))
}
