package com.momentlink.api.storage.application

import com.momentlink.api.auth.common.BusinessException
import com.momentlink.api.common.LogSanitizer
import com.momentlink.api.common.security.TokenCipher
import com.momentlink.api.common.web.ErrorCode
import com.momentlink.api.event.application.HostEventReader
import com.momentlink.api.event.port.out.EventRepository
import com.momentlink.api.storage.dto.StorageConnectReqDto
import com.momentlink.api.storage.dto.StorageConnectResult
import com.momentlink.api.storage.dto.StorageDisconnectResDto
import com.momentlink.api.storage.dto.StorageResDto
import com.momentlink.api.storage.dto.StorageStatusResDto
import com.momentlink.api.storage.infra.google.DriveGrant
import com.momentlink.api.storage.infra.google.GoogleDriveOAuthClient
import com.momentlink.api.storage.port.out.StorageRepository
import com.momentlink.api.transfer.application.TransferEnqueuer
import com.momentlink.domain.storage.Storage
import com.momentlink.domain.storage.StorageConnectState
import com.momentlink.domain.storage.StorageProvider
import com.momentlink.domain.storage.StorageStatus
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionTemplate

@Service
class StorageService(
    private val hostEventReader: HostEventReader,
    private val eventRepository: EventRepository,
    private val storageRepository: StorageRepository,
    private val stateStore: StorageConnectStateStore,
    private val driveClient: GoogleDriveOAuthClient,
    private val tokenCipher: TokenCipher,
    private val transferEnqueuer: TransferEnqueuer,
    private val transactionTemplate: TransactionTemplate,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    // 기존 연결은 OAuth가 성공할 때까지 그대로 둔다. 재인증도 같은 흐름을 쓴다.
    fun connect(eventId: Long, req: StorageConnectReqDto): StorageConnectResult {
        val event = hostEventReader.getOwned(eventId)
        if (req.provider != StorageProvider.GOOGLE_DRIVE) {
            throw BusinessException(ErrorCode.UNSUPPORTED_STORAGE_PROVIDER)
        }
        val state = stateStore.issue(event.userId, event.requiredId(), req.provider)
        return StorageConnectResult(driveClient.createAuthorizationUrl(state.state), state.expiresAt, state.state)
    }

    fun consumeState(provider: String, state: String?, cookieState: String?): StorageConnectState =
        stateStore.consume(provider, state, cookieState)

    // 토큰 교환(네트워크)은 트랜잭션 밖에서 하고 저장만 짧게 묶는다.
    fun completeConnection(connectState: StorageConnectState, code: String?, error: String?) {
        if (error != null) {
            throw BusinessException(ErrorCode.STORAGE_CONNECT_FAILED, reason = "error=${LogSanitizer.sanitize(error)}")
        }
        if (code.isNullOrBlank()) {
            throw BusinessException(ErrorCode.STORAGE_CONNECT_FAILED, reason = "인증 코드 없음")
        }
        val grant = driveClient.exchangeCode(code)
        transactionTemplate.executeWithoutResult { saveConnection(connectState, grant) }
    }

    @Transactional(readOnly = true)
    fun getStatus(eventId: Long): StorageStatusResDto {
        hostEventReader.getOwned(eventId)
        return StorageStatusResDto(storageRepository.findByEventId(eventId)?.let(StorageResDto::from))
    }

    // 연결이 없거나 이미 해제돼도 성공. 외부 저장소에 보낸 파일은 지우지 않는다.
    // Google 폐기는 네트워크 호출이라 해제를 커밋한 뒤에 한다.
    fun disconnect(eventId: Long): StorageDisconnectResDto {
        hostEventReader.getOwned(eventId)
        transactionTemplate.execute { disconnectAndTakeToken(eventId) }
            ?.let { revokeQuietly(eventId, it) }
        return StorageDisconnectResDto(StorageStatus.DISCONNECTED)
    }

    // Google 토큰 폐기는 계정 단위 권한 전체를 회수한다.
    // 같은 계정을 연결한 다른 행사 저장소가 있으면 그쪽 전송까지 끊기므로, 이때는 우리 쪽 토큰만 지운다.
    private fun disconnectAndTakeToken(eventId: Long): String? {
        val storage = storageRepository.findByEventIdForUpdate(eventId) ?: return null
        if (storage.status == StorageStatus.DISCONNECTED) return null

        val encrypted = storage.encryptedRefreshToken
        val isSharedAccount = storageRepository.existsByExternalAccountIdAndStatusNotAndIdNot(
            storage.externalAccountId, StorageStatus.DISCONNECTED, storage.requiredId(),
        )
        storage.disconnect()
        return if (isSharedAccount) null else encrypted
    }

    // 폐기에 실패해도 우리 쪽 연결과 토큰은 이미 지웠으므로 해제는 성공으로 둔다.
    private fun revokeQuietly(eventId: Long, encryptedRefreshToken: String) {
        try {
            driveClient.revoke(tokenCipher.decrypt(encryptedRefreshToken))
            log.info("저장소 토큰 폐기: eventId={}", eventId)
        } catch (ex: Exception) {
            log.warn("저장소 토큰 폐기 실패: eventId={} - {}: {}", eventId, ex.javaClass.simpleName, LogSanitizer.sanitize(ex.message))
        }
    }

    private fun saveConnection(connectState: StorageConnectState, grant: DriveGrant) {
        // 연결을 시작한 뒤 행사 소유자가 바뀌지 않았는지 다시 확인한다.
        eventRepository.findByIdAndUserId(connectState.eventId, connectState.userId)
            ?: throw BusinessException(ErrorCode.NOT_FOUND_EVENT)

        val encrypted = tokenCipher.encrypt(grant.refreshToken)
        val storage = storageRepository.findByEventIdForUpdate(connectState.eventId)
            ?.also { it.reconnect(connectState.provider, grant.accountId, grant.email, encrypted) }
            ?: storageRepository.save(
                Storage.connect(connectState.eventId, connectState.provider, grant.accountId, grant.email, encrypted)
            )

        // 연결 전에 승인된 미디어를 전송 대상으로 모은다.
        transferEnqueuer.enqueueBacklog(connectState.eventId, storage.requiredId())
        log.info("저장소 연결: eventId={}, storageId={}, provider={}", connectState.eventId, storage.requiredId(), connectState.provider)
    }
}
