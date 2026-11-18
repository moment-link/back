package com.momentlink.api.media.application

import com.momentlink.api.participant.port.out.ParticipantSessionRepository
import com.momentlink.domain.media.MediaAsset
import org.springframework.stereotype.Component

// 미디어와 세션 사이에 연관관계가 없으므로 목록당 한 번에 모아서 조회한다.
@Component
class UploaderNameReader(
    private val participantSessionRepository: ParticipantSessionRepository,
) {
    fun read(media: Collection<MediaAsset>): Map<Long, String> =
        participantSessionRepository.findAllById(media.map { it.participantSessionId }.toSet())
            .associate { it.requireId() to it.displayName }

    fun read(media: MediaAsset): String =
        read(listOf(media))[media.participantSessionId].orEmpty()
}
