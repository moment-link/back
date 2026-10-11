package com.momentlink.api.participant.common

import com.momentlink.api.auth.common.BusinessException
import com.momentlink.api.common.web.ErrorCode
import com.momentlink.api.participant.security.AuthenticatedParticipant
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component

@Component
class ParticipantSessionProvider {

    fun get(): AuthenticatedParticipant {
        val authentication = SecurityContextHolder.getContext().authentication

        return authentication?.principal as? AuthenticatedParticipant
            ?: throw BusinessException(ErrorCode.PARTICIPANT_SESSION_REQUIRED)
    }
}