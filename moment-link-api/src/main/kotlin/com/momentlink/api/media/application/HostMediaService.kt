package com.momentlink.api.media.application

import com.momentlink.api.auth.common.BusinessException
import com.momentlink.api.common.Validator
import com.momentlink.api.common.web.ErrorCode
import com.momentlink.api.common.web.SliceResDto
import com.momentlink.api.event.application.HostEventReader
import com.momentlink.api.media.dto.HostMediaDetailResDto
import com.momentlink.api.media.dto.HostMediaItemResDto
import com.momentlink.api.media.dto.MediaDeleteResDto
import com.momentlink.api.media.dto.PublicationChangeReqDto
import com.momentlink.api.media.dto.PublicationChangeResDto
import com.momentlink.api.media.port.out.MediaAssetRepository
import com.momentlink.api.moment.port.out.MomentRepository
import com.momentlink.domain.media.MediaType
import com.momentlink.domain.media.PublicationStatus
import com.momentlink.domain.participant.UploadStatus
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class HostMediaService(
    private val hostEventReader: HostEventReader,
    private val mediaAssetRepository: MediaAssetRepository,
    private val momentRepository: MomentRepository,
    private val uploaderNameReader: UploaderNameReader,
    private val urlIssuer: MediaUrlIssuer,
    private val properties: MediaUploadProperties,
) {

    @Transactional(readOnly = true)
    fun getMedia(
        eventId: Long,
        publicationStatus: PublicationStatus?,
        uploadStatus: UploadStatus?,
        mediaType: MediaType?,
        momentId: Long?,
        page: Int,
        limit: Int,
        sort: MediaSort,
    ): SliceResDto<HostMediaItemResDto> {
        Validator.pagination(page, limit)
        hostEventReader.getOwned(eventId)
        val moment = momentId?.let {
            momentRepository.findByIdAndEventId(it, eventId) ?: throw BusinessException(ErrorCode.MOMENT_NOT_FOUND)
        }

        val slice = mediaAssetRepository.searchForHost(
            eventId, publicationStatus, uploadStatus, mediaType, moment?.startsAt, moment?.endsAt,
            PageRequest.of(page, limit, sort.toSort()),
        )
        val names = uploaderNameReader.read(slice.content)
        return SliceResDto.from(slice) {
            HostMediaItemResDto.from(it, names[it.participantSessionId].orEmpty(), urlIssuer.thumbnailUrl(it))
        }
    }

    @Transactional(readOnly = true)
    fun getMediaDetail(eventId: Long, mediaId: Long): HostMediaDetailResDto {
        hostEventReader.getOwned(eventId)
        val media = mediaAssetRepository.findByIdAndEventIdAndDeletedAtIsNull(mediaId, eventId)
            ?: throw BusinessException(ErrorCode.MEDIA_NOT_FOUND)
        return HostMediaDetailResDto.from(
            media, uploaderNameReader.read(media), urlIssuer.viewUrl(media), urlIssuer.thumbnailUrl(media),
        )
    }

    // 네 상태 사이를 자유롭게 바꿀 수 있고, 행사 종료 후에도 검수할 수 있다.
    @Transactional
    fun changePublication(eventId: Long, mediaId: Long, req: PublicationChangeReqDto): PublicationChangeResDto {
        hostEventReader.getOwned(eventId)
        val media = mediaAssetRepository.findInEventForUpdate(mediaId, eventId)
            ?.takeUnless { it.isDeleted() }
            ?: throw BusinessException(ErrorCode.MEDIA_NOT_FOUND)
        if (media.uploadStatus != UploadStatus.UPLOADED) throw BusinessException(ErrorCode.INVALID_UPLOAD_STATE)

        media.changePublicationStatus(req.publicationStatus)
        return PublicationChangeResDto(media.requireId(), media.publicationStatus)
    }

    // 반복 삭제도 성공한다. 외부 저장소로 이미 보낸 파일은 지우지 않는다.
    @Transactional
    fun deleteMedia(eventId: Long, mediaId: Long): MediaDeleteResDto {
        hostEventReader.getOwned(eventId)
        val media = mediaAssetRepository.findInEventForUpdate(mediaId, eventId)
            ?: throw BusinessException(ErrorCode.MEDIA_NOT_FOUND)
        val now = Instant.now()
        media.delete(now, properties.purgeAt(media.uploadExpiresAt, now))
        return MediaDeleteResDto(media.requireId(), deleted = true)
    }
}
