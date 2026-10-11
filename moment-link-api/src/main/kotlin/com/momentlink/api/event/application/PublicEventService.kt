package com.momentlink.api.event.application

import com.momentlink.api.auth.common.BusinessException
import com.momentlink.api.common.security.key.KeyHasher
import com.momentlink.api.common.web.ErrorCode
import com.momentlink.api.event.dto.PublicEventResDto
import com.momentlink.api.event.port.out.PublicAccessRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class PublicEventService(
    private val publicAccessRepository: PublicAccessRepository,
    private val keyHasher: KeyHasher,
) {
    @Transactional(readOnly = true)
    fun getEventForPublicAccess(accessKey: String): PublicEventResDto {
        val publicAccess = publicAccessRepository.findByAccessKeyHash(keyHasher.hash(accessKey))
            ?: throw BusinessException(ErrorCode.INVALID_EVENT_ACCESS_KEY)

        if (!publicAccess.isAccessible(Instant.now())) {
            throw BusinessException(ErrorCode.EVENT_ACCESS_DISABLED)
        }
        return PublicEventResDto.from(publicAccess.event)
    }
}
