package com.momentlink.api.event.application

import com.momentlink.api.auth.common.BusinessException
import com.momentlink.api.common.security.UserProvider
import com.momentlink.api.common.web.ErrorCode
import com.momentlink.api.event.port.out.EventRepository
import com.momentlink.domain.event.Event
import org.springframework.stereotype.Component

@Component
class HostEventReader(
    private val eventRepository: EventRepository,
    private val userProvider: UserProvider,
) {
    // 남의 행사도 없는 행사와 같은 응답을 줘서 존재 여부를 숨긴다.
    fun getOwned(eventId: Long): Event =
        eventRepository.findByIdAndUserId(eventId, userProvider.getUserId())
            ?: throw BusinessException(ErrorCode.NOT_FOUND_EVENT)

    fun getOwnedOpen(eventId: Long): Event =
        getOwned(eventId).also {
            if (it.isClosed()) throw BusinessException(ErrorCode.EVENT_CLOSED)
        }
}
