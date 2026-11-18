package com.momentlink.api.transfer.worker

import com.momentlink.api.common.security.TokenCipher
import com.momentlink.api.event.port.out.EventRepository
import com.momentlink.api.media.port.out.MediaAssetRepository
import com.momentlink.api.media.port.out.MediaObjectStorage
import com.momentlink.api.storage.application.StorageProperties
import com.momentlink.api.storage.infra.google.DriveTransferException
import com.momentlink.api.storage.infra.google.GoogleDriveOAuthClient
import com.momentlink.api.storage.infra.google.GoogleDriveUploader
import com.momentlink.api.storage.port.out.StorageRepository
import com.momentlink.domain.media.TransferFailureType
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionTemplate
import java.security.GeneralSecurityException

// 토큰 갱신, 폴더 확인, S3 → Drive 업로드. 네트워크 작업이라 트랜잭션을 잡지 않는다.
@Component
class DriveTransferExecutor(
    private val mediaAssetRepository: MediaAssetRepository,
    private val storageRepository: StorageRepository,
    private val eventRepository: EventRepository,
    private val objectStorage: MediaObjectStorage,
    private val driveClient: GoogleDriveOAuthClient,
    private val uploader: GoogleDriveUploader,
    private val tokenCipher: TokenCipher,
    private val storageProperties: StorageProperties,
    private val transactionTemplate: TransactionTemplate,
) {
    fun transfer(claimed: ClaimedTransfer): String {
        val media = mediaAssetRepository.findByIdOrNull(claimed.mediaAssetId)
            ?: throw DriveTransferException(TransferFailureType.UNKNOWN, retryable = false, "미디어 없음")
        val storage = storageRepository.findByIdOrNull(claimed.storageId)
            ?: throw DriveTransferException(TransferFailureType.UNKNOWN, retryable = false, "저장소 없음")

        val accessToken = driveClient.refreshAccessToken(decryptRefreshToken(storage.encryptedRefreshToken))
        val folderId = storage.destinationFolderId ?: createFolder(accessToken, storage.requiredId(), storage.eventId)

        return objectStorage.open(media.objectKey).use { content ->
            uploader.upload(accessToken, folderId, media.fileName, media.mimeType, media.sizeBytes, content)
        }
    }

    // 암호화 키가 바뀌었거나 값이 깨졌으면 다시 연결해야 한다.
    private fun decryptRefreshToken(encrypted: String): String =
        try {
            tokenCipher.decrypt(encrypted)
        } catch (ex: GeneralSecurityException) {
            throw DriveTransferException(TransferFailureType.AUTHENTICATION, retryable = true, "저장소 토큰을 읽을 수 없습니다. 암호화 키를 확인하세요.")
        } catch (ex: IllegalArgumentException) {
            throw DriveTransferException(TransferFailureType.AUTHENTICATION, retryable = true, "저장소 토큰 형식이 올바르지 않습니다.")
        }

    private fun createFolder(accessToken: String, storageId: Long, eventId: Long): String {
        val eventName = eventRepository.findByIdOrNull(eventId)?.name.orEmpty()
        val folderId = uploader.createFolder(accessToken, "${storageProperties.googleDrive.folderPrefix} - $eventName")
        transactionTemplate.executeWithoutResult {
            storageRepository.findByIdForUpdate(storageId)?.assignDestinationFolder(folderId)
        }
        return folderId
    }
}
