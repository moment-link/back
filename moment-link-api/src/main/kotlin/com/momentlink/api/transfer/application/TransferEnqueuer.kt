package com.momentlink.api.transfer.application

import com.momentlink.api.media.port.out.MediaAssetRepository
import com.momentlink.api.storage.port.out.StorageRepository
import com.momentlink.api.transfer.port.out.MediaTransferRepository
import com.momentlink.domain.media.MediaAsset
import com.momentlink.domain.media.MediaTransfer
import org.springframework.stereotype.Component

// 같은 미디어의 전송 작업은 하나만 둔다. (media_asset_id UNIQUE)
@Component
class TransferEnqueuer(
    private val storageRepository: StorageRepository,
    private val mediaTransferRepository: MediaTransferRepository,
    private val mediaAssetRepository: MediaAssetRepository,
) {
    // 승인 시점: 연결된 저장소가 있고 아직 작업이 없을 때만 만든다.
    fun enqueue(media: MediaAsset) {
        val storage = storageRepository.findByEventId(media.eventId)?.takeIf { it.isConnected() } ?: return
        if (mediaTransferRepository.existsByMediaAssetId(media.requireId())) return
        mediaTransferRepository.save(MediaTransfer.create(media.requireId(), storage.requiredId()))
    }

    // 저장소 연결 직후: 승인됐지만 작업이 없는 미디어를 모은다.
    fun enqueueBacklog(eventId: Long, storageId: Long) {
        val transfers = mediaAssetRepository.findTransferBacklog(eventId)
            .map { MediaTransfer.create(it.requireId(), storageId) }
        mediaTransferRepository.saveAll(transfers)
    }
}
