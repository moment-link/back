package com.momentlink.api.moment.application

import com.momentlink.api.auth.common.BusinessException
import com.momentlink.api.common.web.ErrorCode
import com.momentlink.api.event.application.HostEventReader
import com.momentlink.api.moment.dto.MomentCreateReqDto
import com.momentlink.api.moment.dto.MomentDeleteResDto
import com.momentlink.api.moment.dto.MomentListResDto
import com.momentlink.api.moment.dto.MomentResDto
import com.momentlink.api.moment.dto.MomentUpdateReqDto
import com.momentlink.api.moment.port.out.MomentRepository
import com.momentlink.api.participant.common.ParticipantSessionProvider
import com.momentlink.domain.moment.Moment
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class MomentService(
    private val hostEventReader: HostEventReader,
    private val momentRepository: MomentRepository,
    private val sessionProvider: ParticipantSessionProvider,
) {

    @Transactional(readOnly = true)
    fun getMoments(eventId: Long): MomentListResDto {
        hostEventReader.getOwned(eventId)
        return MomentListResDto.from(momentRepository.findAllByEventIdOrderBySortOrderAscIdAsc(eventId))
    }

    @Transactional(readOnly = true)
    fun getParticipantMoments(): MomentListResDto =
        MomentListResDto.from(momentRepository.findAllByEventIdOrderBySortOrderAscIdAsc(sessionProvider.get().eventId))

    @Transactional
    fun createMoment(eventId: Long, req: MomentCreateReqDto): MomentResDto {
        val event = hostEventReader.getOwnedOpen(eventId)
        val draft = MomentDraft(req.title.trim(), req.place.trim(), req.startsAt, req.endsAt, req.sortOrder)
        MomentPolicy.validate(draft, event)

        val moment = momentRepository.save(
            Moment.create(event.requiredId(), draft.title, draft.place, draft.startsAt, draft.endsAt, draft.sortOrder)
        )
        return MomentResDto.from(moment)
    }

    @Transactional
    fun updateMoment(eventId: Long, momentId: Long, req: MomentUpdateReqDto): MomentResDto {
        val event = hostEventReader.getOwnedOpen(eventId)
        val moment = getMoment(momentId, eventId)

        // 생략한 필드는 기존 값을 쓰고, 합친 최종 상태로 검증한다.
        val draft = MomentDraft(
            title = req.title?.trim() ?: moment.title,
            place = req.place?.trim() ?: moment.place,
            startsAt = req.startsAt ?: moment.startsAt,
            endsAt = req.endsAt ?: moment.endsAt,
            sortOrder = req.sortOrder ?: moment.sortOrder,
        )
        MomentPolicy.validate(draft, event)
        moment.change(draft.title, draft.place, draft.startsAt, draft.endsAt, draft.sortOrder)
        return MomentResDto.from(moment)
    }

    @Transactional
    fun deleteMoment(eventId: Long, momentId: Long): MomentDeleteResDto {
        hostEventReader.getOwnedOpen(eventId)
        // 미디어와 직접 연결되지 않으므로 모먼트만 지운다.
        momentRepository.delete(getMoment(momentId, eventId))
        return MomentDeleteResDto(momentId, deleted = true)
    }

    private fun getMoment(momentId: Long, eventId: Long): Moment =
        momentRepository.findByIdAndEventId(momentId, eventId)
            ?: throw BusinessException(ErrorCode.MOMENT_NOT_FOUND)
}
