package com.momentlink.api.media.application

import com.momentlink.api.auth.common.BusinessException
import com.momentlink.api.common.web.ErrorCode
import com.momentlink.api.event.port.out.EventRepository
import com.momentlink.api.media.dto.CompleteUploadResDto
import com.momentlink.api.media.dto.PrepareUploadReqDto
import com.momentlink.api.media.dto.PrepareUploadResDto
import com.momentlink.api.media.port.out.MediaAssetRepository
import com.momentlink.api.media.port.out.MediaObjectStorage
import com.momentlink.api.media.port.out.UploadTarget
import com.momentlink.api.participant.common.ParticipantSessionProvider
import com.momentlink.api.participant.port.out.ParticipantSessionRepository
import com.momentlink.domain.media.MediaAsset
import com.momentlink.domain.participant.UploadStatus
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionTemplate
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

@Service
class MediaUploadService(
    private val sessionProvider: ParticipantSessionProvider,
    private val participantSessionRepository: ParticipantSessionRepository,
    private val mediaAssetRepository: MediaAssetRepository,
    private val eventRepository: EventRepository,
    private val policy: MediaUploadPolicy,
    private val storage: MediaObjectStorage,
    private val properties: MediaUploadProperties,
    private val transactionTemplate: TransactionTemplate,
) {

    @Transactional
    fun prepare(idempotencyKey: String, req: PrepareUploadReqDto): PrepareUploadResDto {
        val participant = sessionProvider.get()
        policy.validateIdempotencyKey(idempotencyKey)
        policy.validate(req)
        val now = Instant.now()

        // 같은 세션의 준비 요청을 직렬화해 같은 키로 두 건이 생기지 않게 한다.
        participantSessionRepository.findByIdForUpdate(participant.sessionId)
            ?: throw BusinessException(ErrorCode.INVALID_PARTICIPANT_SESSION)

        val existing = mediaAssetRepository.findByParticipantSessionIdAndPrepareIdempotencyKey(participant.sessionId, idempotencyKey)
        if (existing != null) return replayPrepare(existing, req, now)

        val event = eventRepository.findByIdOrNull(participant.eventId)
            ?: throw BusinessException(ErrorCode.NOT_FOUND_EVENT)
        if (event.isClosed() || !now.isBefore(event.endsAt)) {
            throw BusinessException(ErrorCode.EVENT_UPLOAD_CLOSED)
        }

        val media = mediaAssetRepository.save(
            MediaAsset.prepare(
                eventId = participant.eventId,
                uploadSessionId = participant.sessionId,
                prepareIdempotencyKey = idempotencyKey,
                // 사용자 파일명은 객체 키에 넣지 않는다.
                objectKey = "events/${participant.eventId}/${UUID.randomUUID()}",
                fileName = req.fileName.trim(),
                mediaType = req.mediaType,
                mimeType = policy.normalizeMimeType(req.mimeType),
                sizeBytes = req.sizeBytes,
                checkSum = req.checksum,
                capturedAt = normalizedCapturedAt(req),
                uploadExpiresAt = now.plus(properties.urlValidity),
            )
        )
        return PrepareUploadResDto.from(media, presign(media, now))
    }

    fun complete(mediaId: Long, idempotencyKey: String): CompleteUploadResDto {
        val participant = sessionProvider.get()
        policy.validateIdempotencyKey(idempotencyKey)

        val media = mediaAssetRepository.findByIdAndEventIdAndParticipantSessionId(mediaId, participant.eventId, participant.sessionId)
            ?: throw BusinessException(ErrorCode.MEDIA_NOT_FOUND)
        completedResult(media, idempotencyKey)?.let { return it }
        requireCompletable(media)
        if (media.isUploadExpired(Instant.now())) throw BusinessException(ErrorCode.UPLOAD_EXPIRED)

        // S3 조회는 DB 트랜잭션 밖에서 한다.
        val stored = storage.head(media.objectKey)
        if (stored.sizeBytes != media.sizeBytes || stored.checksum != media.checkSum) {
            throw BusinessException(
                ErrorCode.UPLOAD_VERIFICATION_FAILED,
                reason = "mediaId=$mediaId size=${stored.sizeBytes}/${media.sizeBytes} checksumPresent=${stored.checksum != null}",
            )
        }

        return checkNotNull(transactionTemplate.execute {
            // HEAD 도중 취소·삭제됐으면 완료가 덮어쓰지 않도록 잠근 뒤 다시 확인한다.
            val locked = mediaAssetRepository.findOwnedForUpdate(mediaId, participant.eventId, participant.sessionId)
                ?: throw BusinessException(ErrorCode.MEDIA_NOT_FOUND)
            completedResult(locked, idempotencyKey) ?: run {
                requireCompletable(locked)
                val now = Instant.now()
                locked.markUploaded(now, idempotencyKey, stored.eTag)
                eventRepository.recordFirstUploadIfAbsent(locked.eventId, now)
                CompleteUploadResDto.from(locked)
            }
        })
    }

    private fun replayPrepare(media: MediaAsset, req: PrepareUploadReqDto, now: Instant): PrepareUploadResDto {
        val isSameRequest = media.matchesPrepareRequest(
            req.fileName.trim(),
            req.mediaType,
            policy.normalizeMimeType(req.mimeType),
            req.sizeBytes,
            req.checksum,
            normalizedCapturedAt(req),
        )
        if (!isSameRequest) throw BusinessException(ErrorCode.IDEMPOTENCY_KEY_CONFLICT)
        if (media.isDeleted() || media.uploadStatus != UploadStatus.PENDING) throw BusinessException(ErrorCode.INVALID_UPLOAD_STATE)
        if (media.isUploadExpired(now)) throw BusinessException(ErrorCode.UPLOAD_EXPIRED)
        return PrepareUploadResDto.from(media, presign(media, now))
    }

    // 재요청이어도 처음 정한 만료 시각까지만 유효한 URL을 다시 서명한다.
    private fun presign(media: MediaAsset, now: Instant): UploadTarget =
        storage.presignUpload(
            media.objectKey,
            media.mimeType,
            media.checkSum,
            Duration.between(now, checkNotNull(media.uploadExpiresAt)),
        )

    // 같은 키 재요청이면 기존 결과, 완료된 업로드에 다른 키면 충돌
    private fun completedResult(media: MediaAsset, idempotencyKey: String): CompleteUploadResDto? {
        if (media.isDeleted() || media.uploadStatus != UploadStatus.UPLOADED) return null
        if (media.completeIdempotencyKey != idempotencyKey) throw BusinessException(ErrorCode.IDEMPOTENCY_KEY_CONFLICT)
        return CompleteUploadResDto.from(media)
    }

    private fun requireCompletable(media: MediaAsset) {
        if (media.isDeleted() || media.uploadStatus != UploadStatus.PENDING) {
            throw BusinessException(ErrorCode.INVALID_UPLOAD_STATE)
        }
    }

    // DB(DATETIME(6))에 저장된 값과 재요청 값을 비교할 수 있게 정밀도를 맞춘다.
    private fun normalizedCapturedAt(req: PrepareUploadReqDto): Instant? =
        req.capturedAt?.truncatedTo(ChronoUnit.MILLIS)
}
