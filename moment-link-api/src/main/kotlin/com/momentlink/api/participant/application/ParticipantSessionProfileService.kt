package com.momentlink.api.participant.application

import com.momentlink.api.auth.common.BusinessException
import com.momentlink.api.common.web.ErrorCode
import com.momentlink.api.participant.common.ParticipantSessionProvider
import com.momentlink.api.participant.dto.DisplayNameUpdateReqDto
import com.momentlink.api.participant.dto.DisplayNameUpdateResDto
import com.momentlink.api.participant.dto.ParticipantProfileResDto
import com.momentlink.api.participant.port.out.ParticipantSessionRepository
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ParticipantSessionProfileService(
    private val participantSessionRepository: ParticipantSessionRepository,
    private val sessionProvider: ParticipantSessionProvider,
) {

    @Transactional(readOnly = true)
    fun getMe(): ParticipantProfileResDto {
        val session = participantSessionRepository.findByIdOrNull(sessionProvider.get().sessionId)
            ?: throw BusinessException(ErrorCode.INVALID_PARTICIPANT_SESSION)
        return ParticipantProfileResDto.from(session)
    }

    @Transactional
    fun rename(req: DisplayNameUpdateReqDto): DisplayNameUpdateResDto {
        val displayName = normalize(req.displayName)
        val session = participantSessionRepository.findByIdForUpdate(sessionProvider.get().sessionId)
            ?: throw BusinessException(ErrorCode.INVALID_PARTICIPANT_SESSION)

        if (session.displayName != displayName) {
            if (participantSessionRepository.existsByEventIdAndDisplayNameAndIdNot(session.eventId, displayName, session.requireId())) {
                throw BusinessException(ErrorCode.DISPLAY_NAME_CONFLICT)
            }
            session.rename(displayName)
            flushOrConflict()
        }
        return DisplayNameUpdateResDto(session.requireId(), session.displayName)
    }

    // 중복 검사와 저장 사이에 다른 세션이 같은 닉네임을 가져가면 유일 제약이 막는다.
    private fun flushOrConflict() {
        try {
            participantSessionRepository.flush()
        } catch (ex: DataIntegrityViolationException) {
            throw BusinessException(ErrorCode.DISPLAY_NAME_CONFLICT, reason = ex.mostSpecificCause.message)
        }
    }

    private fun normalize(displayName: String): String {
        val trimmed = displayName.trim()
        if (trimmed.length !in DISPLAY_NAME_LENGTH || trimmed.any(Char::isISOControl)) {
            throw BusinessException(ErrorCode.INVALID_DISPLAY_NAME)
        }
        return trimmed
    }

    companion object {
        private val DISPLAY_NAME_LENGTH = 2..20
    }
}
