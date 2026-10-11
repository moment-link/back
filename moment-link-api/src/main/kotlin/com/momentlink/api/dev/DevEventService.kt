package com.momentlink.api.dev

import com.momentlink.api.common.security.key.KeyGenerator
import com.momentlink.api.event.application.PublicAccessUrlGenerator
import com.momentlink.api.event.port.out.EventRepository
import com.momentlink.api.user.port.out.UserRepository
import com.momentlink.domain.event.Event
import com.momentlink.domain.user.LoginProvider
import com.momentlink.domain.user.User
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.Instant
import java.util.UUID

@Service
class DevEventService(
    private val userRepository: UserRepository,
    private val eventRepository: EventRepository,
    private val keyGenerator: KeyGenerator,
    private val publicAccessUrlGenerator: PublicAccessUrlGenerator,
) {
    @Transactional
    fun createEvent(): DevEventResDto {
        // 요청마다 독립된 테스트 호스트를 만들어 다른 개발자의 행사와 겹치지 않게 한다.
        val host = userRepository.save(User.create("dev-${UUID.randomUUID()}@moment-link.invalid", LoginProvider.GOOGLE))
        val key = keyGenerator.generate()
        val now = Instant.now()
        val event = eventRepository.save(
            Event.create(
                userId = host.requiredId(),
                name = "참가자 개발용 행사",
                startsAt = now,
                endsAt = now.plus(Duration.ofDays(7)),
                accessKeyHash = key.hashedKey,
                accessKey = key.rawKey,
            ),
        )
        return DevEventResDto(event.requiredId(), key.rawKey, publicAccessUrlGenerator.generate(key.rawKey))
    }
}

data class DevEventResDto(
    val eventId: Long,
    val accessKey: String,
    val shareUrl: String,
)
