package com.momentlink.api.transfer.application

import com.momentlink.api.auth.common.BusinessException
import com.momentlink.api.common.Validator
import com.momentlink.api.common.web.ErrorCode
import com.momentlink.api.common.web.SliceResDto
import com.momentlink.api.event.application.HostEventReader
import com.momentlink.api.media.port.out.MediaAssetRepository
import com.momentlink.api.storage.port.out.StorageRepository
import com.momentlink.api.transfer.dto.TransferDetailResDto
import com.momentlink.api.transfer.dto.TransferItemResDto
import com.momentlink.api.transfer.dto.TransferRetryResDto
import com.momentlink.api.transfer.port.out.MediaTransferAttemptRepository
import com.momentlink.api.transfer.port.out.MediaTransferRepository
import com.momentlink.domain.media.TransferStatus
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

// 전송 작업에는 eventId가 없지만 행사당 저장소가 하나이고 storageId가 바뀌지 않아 storageId로 거른다.
@Service
class TransferService(
    private val hostEventReader: HostEventReader,
    private val storageRepository: StorageRepository,
    private val mediaTransferRepository: MediaTransferRepository,
    private val attemptRepository: MediaTransferAttemptRepository,
    private val mediaAssetRepository: MediaAssetRepository,
) {

    @Transactional(readOnly = true)
    fun getTransfers(eventId: Long, status: TransferStatus?, page: Int, limit: Int): SliceResDto<TransferItemResDto> {
        Validator.pagination(page, limit)
        hostEventReader.getOwned(eventId)
        val storage = storageRepository.findByEventId(eventId) ?: return SliceResDto.empty()

        val pageable = PageRequest.of(page, limit, Sort.by(Sort.Order.desc("id")))
        val slice = if (status == null) {
            mediaTransferRepository.findAllByStorageId(storage.requiredId(), pageable)
        } else {
            mediaTransferRepository.findAllByStorageIdAndStatus(storage.requiredId(), status, pageable)
        }
        val fileNames = mediaAssetRepository.findAllById(slice.content.map { it.mediaAssetId })
            .associate { it.requireId() to it.fileName }
        return SliceResDto.from(slice) { TransferItemResDto.from(it, fileNames[it.mediaAssetId].orEmpty()) }
    }

    @Transactional(readOnly = true)
    fun getTransfer(eventId: Long, transferId: Long): TransferDetailResDto {
        hostEventReader.getOwned(eventId)
        val storage = storageRepository.findByEventId(eventId)
            ?: throw BusinessException(ErrorCode.TRANSFER_NOT_FOUND)
        val transfer = mediaTransferRepository.findByIdAndStorageId(transferId, storage.requiredId())
            ?: throw BusinessException(ErrorCode.TRANSFER_NOT_FOUND)

        val fileName = mediaAssetRepository.findByIdOrNull(transfer.mediaAssetId)?.fileName.orEmpty()
        val attempts = attemptRepository.findAllByMediaTransferIdOrderByAttemptNumberDesc(transferId)
        return TransferDetailResDto.from(transfer, fileName, attempts)
    }

    @Transactional
    fun retry(eventId: Long, transferId: Long): TransferRetryResDto {
        hostEventReader.getOwned(eventId)
        val storage = storageRepository.findByEventId(eventId)
            ?: throw BusinessException(ErrorCode.TRANSFER_NOT_FOUND)
        val transfer = mediaTransferRepository.findByIdAndStorageIdForUpdate(transferId, storage.requiredId())
            ?: throw BusinessException(ErrorCode.TRANSFER_NOT_FOUND)

        if (transfer.status == TransferStatus.PENDING) return TransferRetryResDto(transferId, transfer.status)
        if (transfer.status != TransferStatus.RETRY_WAIT && transfer.status != TransferStatus.PERMANENT_FAILED) {
            throw BusinessException(ErrorCode.TRANSFER_NOT_RETRYABLE)
        }
        if (!storage.isConnected()) throw BusinessException(ErrorCode.STORAGE_NOT_CONNECTED)
        val media = mediaAssetRepository.findByIdOrNull(transfer.mediaAssetId)
        if (media == null || !media.isPubliclyVisible()) throw BusinessException(ErrorCode.TRANSFER_MEDIA_NOT_ELIGIBLE)

        transfer.requeue()
        return TransferRetryResDto(transferId, transfer.status)
    }
}
