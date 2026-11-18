package com.momentlink.api.media.application

import com.momentlink.api.auth.common.BusinessException
import com.momentlink.api.common.Validator
import com.momentlink.api.common.web.ErrorCode
import com.momentlink.api.common.web.SliceResDto
import com.momentlink.api.media.dto.GalleryDetailResDto
import com.momentlink.api.media.dto.GalleryItemResDto
import com.momentlink.api.media.dto.LikeResDto
import com.momentlink.api.media.port.out.MediaAssetRepository
import com.momentlink.api.media.port.out.MediaReactionRepository
import com.momentlink.api.moment.port.out.MomentRepository
import com.momentlink.api.participant.common.ParticipantSessionProvider
import com.momentlink.domain.media.MediaAsset
import com.momentlink.domain.media.MediaReaction
import com.momentlink.domain.media.MediaType
import com.momentlink.domain.participant.UploadStatus
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class GalleryService(
    private val sessionProvider: ParticipantSessionProvider,
    private val mediaAssetRepository: MediaAssetRepository,
    private val mediaReactionRepository: MediaReactionRepository,
    private val momentRepository: MomentRepository,
    private val uploaderNameReader: UploaderNameReader,
    private val urlIssuer: MediaUrlIssuer,
) {

    @Transactional(readOnly = true)
    fun getGallery(
        mediaType: MediaType?,
        momentId: Long?,
        page: Int,
        limit: Int,
        sort: GallerySort,
    ): SliceResDto<GalleryItemResDto> {
        Validator.pagination(page, limit)
        val participant = sessionProvider.get()
        val moment = momentId?.let {
            momentRepository.findByIdAndEventId(it, participant.eventId) ?: throw BusinessException(ErrorCode.MOMENT_NOT_FOUND)
        }

        val slice = mediaAssetRepository.searchGallery(
            participant.eventId, mediaType, moment?.startsAt, moment?.endsAt, PageRequest.of(page, limit, sort.toSort()),
        )
        val names = uploaderNameReader.read(slice.content)
        val liked = likedMediaIds(participant.sessionId, slice.content)

        return SliceResDto.from(slice) { media ->
            GalleryItemResDto(
                mediaId = media.requireId(),
                mediaType = media.mediaType,
                thumbnailUrl = urlIssuer.thumbnailUrl(media),
                capturedAt = media.capturedAt,
                uploadedAt = media.uploadedAt,
                uploaderDisplayName = names[media.participantSessionId].orEmpty(),
                likeCount = media.likeCount,
                likedByMe = media.requireId() in liked,
                isMine = media.participantSessionId == participant.sessionId,
            )
        }
    }

    @Transactional(readOnly = true)
    fun getGalleryDetail(mediaId: Long): GalleryDetailResDto {
        val participant = sessionProvider.get()
        val media = mediaAssetRepository.findByIdAndEventIdAndDeletedAtIsNull(mediaId, participant.eventId)
            ?: throw BusinessException(ErrorCode.MEDIA_NOT_FOUND)

        // 미승인·거절·숨김은 올린 본인에게만 보인다.
        val isMine = media.participantSessionId == participant.sessionId
        val canView = media.isPubliclyVisible() || (isMine && media.uploadStatus == UploadStatus.UPLOADED)
        if (!canView) throw BusinessException(ErrorCode.MEDIA_NOT_FOUND)

        val likedByMe = mediaReactionRepository.existsByMediaAssetIdAndParticipantSessionId(mediaId, participant.sessionId)
        return GalleryDetailResDto.from(
            media, uploaderNameReader.read(media), urlIssuer.viewUrl(media), urlIssuer.thumbnailUrl(media), likedByMe, isMine,
        )
    }

    // 미디어 행을 잠그고 처리해 좋아요 수 유실과 중복 등록을 함께 막는다.
    @Transactional
    fun like(mediaId: Long): LikeResDto {
        val participant = sessionProvider.get()
        val media = mediaAssetRepository.findInEventForUpdate(mediaId, participant.eventId)
            ?.takeIf { it.isPubliclyVisible() }
            ?: throw BusinessException(ErrorCode.MEDIA_NOT_FOUND)

        if (!mediaReactionRepository.existsByMediaAssetIdAndParticipantSessionId(mediaId, participant.sessionId)) {
            mediaReactionRepository.save(MediaReaction.create(mediaId, participant.sessionId))
            media.increaseLikeCount()
        }
        return LikeResDto(mediaId, likedByMe = true, likeCount = media.likeCount)
    }

    // 숨김·거절된 미디어라도 본인의 기존 좋아요는 취소할 수 있다.
    @Transactional
    fun unlike(mediaId: Long): LikeResDto {
        val participant = sessionProvider.get()
        val media = mediaAssetRepository.findInEventForUpdate(mediaId, participant.eventId)
            ?: throw BusinessException(ErrorCode.MEDIA_NOT_FOUND)

        if (mediaReactionRepository.deleteLike(mediaId, participant.sessionId) > 0) {
            media.decreaseLikeCount()
        }
        return LikeResDto(mediaId, likedByMe = false, likeCount = media.likeCount)
    }

    private fun likedMediaIds(sessionId: Long, media: List<MediaAsset>): Set<Long> {
        if (media.isEmpty()) return emptySet()
        return mediaReactionRepository.findLikedMediaIds(sessionId, media.map { it.requireId() }).toSet()
    }
}
