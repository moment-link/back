package com.momentlink.api.event.application

import com.momentlink.api.auth.common.BusinessException
import com.momentlink.api.common.Validator
import com.momentlink.api.common.security.UserProvider
import com.momentlink.api.common.security.key.IssuedKey
import com.momentlink.api.common.security.key.KeyGenerator
import com.momentlink.api.common.web.ErrorCode
import com.momentlink.api.event.dto.EventAccessDisableResDto
import com.momentlink.api.event.dto.EventAccessEnableResDto
import com.momentlink.api.event.dto.EventCloseResDto
import com.momentlink.api.event.dto.EventCreateReqDto
import com.momentlink.api.event.dto.EventCreateResDto
import com.momentlink.api.event.dto.EventListResDto
import com.momentlink.api.event.dto.EventResDto
import com.momentlink.api.event.dto.EventAccessResDto
import com.momentlink.api.event.dto.EventUpdateReqDto
import com.momentlink.api.event.dto.EventUpdateResDto
import com.momentlink.api.event.port.out.EventRepository
import com.momentlink.domain.event.Event
import com.momentlink.domain.event.EventStatus
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class EventService(
    private val publicAccessUrlGenerator: PublicAccessUrlGenerator,
    private val eventRepository: EventRepository,
    private val userProvider: UserProvider,
    private val keyGenerator: KeyGenerator,
) {

    @Transactional
    fun createEvent(req: EventCreateReqDto): EventCreateResDto {
        val userId = userProvider.getUserId()

        // 이벤트 접근 키 발급
        val issuedKey = keyGenerator.generate()

        // Event 생성
        val newEvent = createEventEntity(req, userId, issuedKey)

        // Event 영속화
        val savedEvent = eventRepository.save(newEvent)

        // 공유URL 생성
        val shareUrl = publicAccessUrlGenerator.generate(issuedKey.rawKey)

        // resDto 반환
        return EventCreateResDto.from(savedEvent, issuedKey.rawKey, shareUrl)
    }

    private fun createEventEntity(
        req: EventCreateReqDto,
        userId: Long,
        issuedKey: IssuedKey): Event {
        return Event.create(
            userId,
            req.name,
            req.startsAt,
            req.endsAt,
            issuedKey.hashedKey,
            issuedKey.rawKey,
        )
    }

    @Transactional(readOnly = true)
    fun getEvents(page: Int, limit: Int, status: EventStatus?, sort: EventSort): EventListResDto {
        Validator.pagination(page, limit)
        val pageable = PageRequest.of(page, limit, sort.toSort())
        val userId = userProvider.getUserId()

        // status가 없으면, 전체 query
        val events = if (status == null) {
            eventRepository.findAllByUserId(userId, pageable)
        } else {
            eventRepository.findAllByUserIdAndStatus(userId, status, pageable)
        }

        return EventListResDto.from(events)
    }

    @Transactional(readOnly = true)
    fun getEvent(eventId: Long): EventResDto {
        val userId = userProvider.getUserId()
        val event = eventRepository.findByIdAndUserId(eventId, userId)
            ?: throw BusinessException(ErrorCode.NOT_FOUND_EVENT)
        return EventResDto.from(event)
    }

    @Transactional
    fun updateEvent(eventId: Long, req: EventUpdateReqDto): EventUpdateResDto {
        val userId = userProvider.getUserId()
        val event = eventRepository.findByIdAndUserId(eventId, userId)
            ?: throw BusinessException(ErrorCode.NOT_FOUND_EVENT)

        req.name?.let(event::rename)
        if (req.startsAt != null || req.endsAt != null) {
            event.reschedule(req.startsAt ?: event.startsAt, req.endsAt ?: event.endsAt)
        }

        return EventUpdateResDto.from(event)
    }

    @Transactional
    fun closeEvent(eventId: Long): EventCloseResDto {
        val userId = userProvider.getUserId()
        val event = eventRepository.findByIdAndUserId(eventId, userId)
            ?: throw BusinessException(ErrorCode.NOT_FOUND_EVENT)

        event.close()
        return EventCloseResDto.from(event)
    }

    @Transactional
    fun enableEvent(eventId: Long): EventAccessEnableResDto {
        val userId = userProvider.getUserId()
        val event = eventRepository.findByIdAndUserId(eventId, userId)
            ?: throw BusinessException(ErrorCode.NOT_FOUND_EVENT)
        event.enablePublicAccess()
        return EventAccessEnableResDto.from(event)
    }

    @Transactional
    fun disableEvent(eventId: Long): EventAccessDisableResDto {
        val userId = userProvider.getUserId()
        val event = eventRepository.findByIdAndUserId(eventId, userId)
            ?: throw BusinessException(ErrorCode.NOT_FOUND_EVENT)
        event.disablePublicAccess()
        return EventAccessDisableResDto.from(event)
    }

    @Transactional
    fun rotateEvent(eventId: Long): EventAccessResDto {
        val userId = userProvider.getUserId()
        val event = eventRepository.findByIdAndUserId(eventId, userId)
        ?: throw BusinessException(ErrorCode.NOT_FOUND_EVENT)

        // 닫힌 이벤트는 에러 throw
        if(event.isClosed()) { throw BusinessException(ErrorCode.EVENT_CLOSED) }

        // 키 발급
        val issuedKey = keyGenerator.generate()

        // 키 교체 (event -> publicAccess의 hashkey 교체)
        event.rotateAccessKey(issuedKey.hashedKey, issuedKey.rawKey)

        // 공유 URL 생성
        val shareUrl = publicAccessUrlGenerator.generate(issuedKey.rawKey)
        return EventAccessResDto.from(event, issuedKey.rawKey, shareUrl)
    }

    @Transactional(readOnly = true)
    fun getAccess(eventId: Long): EventAccessResDto {
        val event = eventRepository.findByIdAndUserId(eventId, userProvider.getUserId())
            ?: throw BusinessException(ErrorCode.NOT_FOUND_EVENT)
        val accessKey = event.publicAccess.accessKey
            ?: throw BusinessException(ErrorCode.EVENT_ACCESS_KEY_UNAVAILABLE)
        return EventAccessResDto.from(event, accessKey, publicAccessUrlGenerator.generate(accessKey))
    }

}
