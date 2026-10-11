package com.momentlink.api.participant

import com.momentlink.api.participant.application.ParticipantSessionAuthService
import com.momentlink.api.participant.port.out.ParticipantSessionRepository
import com.momentlink.api.event.port.out.EventRepository
import com.momentlink.api.event.port.out.PublicAccessRepository
import com.momentlink.api.common.security.key.KeyGenerator
import com.momentlink.api.common.security.key.KeyHasher
import com.momentlink.api.auth.common.BusinessException
import com.momentlink.domain.participant.ParticipantSession
import com.momentlink.domain.event.Event
import java.lang.reflect.Proxy
import java.time.Instant
import java.util.Optional
import kotlin.test.*

class ParticipantSessionAuthServiceTest {
    private inline fun <reified T> fake(crossinline call: (String) -> Any?): T =
        Proxy.newProxyInstance(T::class.java.classLoader, arrayOf(T::class.java)) { _, method, _ -> call(method.name) } as T

    private fun service(expired: Boolean = false, missing: Boolean = false): ParticipantSessionAuthService {
        val now = Instant.now()
        val event = Event.create(1, "행사", now.minusSeconds(60), now.plusSeconds(3600), "key")
        Event::class.java.getDeclaredField("id").apply { isAccessible = true }.set(event, 1L)
        val session = ParticipantSession.create(1, "hash", now.plusSeconds(if (expired) -1 else 3600), "참가자")
        session.id = 10L
        val hasher = KeyHasher()
        return ParticipantSessionAuthService(
            fake<ParticipantSessionRepository> { if (missing) null else session },
            fake<PublicAccessRepository> { null }, KeyGenerator(hasher), hasher,
            fake<EventRepository> { Optional.of(event) },
        )
    }

    @Test fun `same event session is accepted`() {
        assertEquals(1L, service().authenticate("token", 1).eventId)
    }
    @Test fun `different event session is forbidden`() {
        val ex = assertFailsWith<BusinessException> { service().authenticate("token", 2) }
        assertEquals("PARTICIPANT_EVENT_MISMATCH", ex.errorCode.name)
        assertEquals(403, ex.errorCode.httpStatus.value())
    }
    @Test fun `expired session is unauthorized`() {
        val ex = assertFailsWith<BusinessException> { service(expired = true).authenticate("token", 1) }
        assertEquals("PARTICIPANT_SESSION_EXPIRED", ex.errorCode.name)
        assertEquals(401, ex.errorCode.httpStatus.value())
    }
    @Test fun `unknown session is unauthorized`() {
        val ex = assertFailsWith<BusinessException> { service(missing = true).authenticate("token", 1) }
        assertEquals("INVALID_PARTICIPANT_SESSION", ex.errorCode.name)
    }
    @Test fun `invalid access key has a client error`() {
        val ex = assertFailsWith<BusinessException> { service().getOrCreateSession(1, "wrong", null) }
        assertEquals("INVALID_EVENT_ACCESS_KEY", ex.errorCode.name)
        assertEquals(403, ex.errorCode.httpStatus.value())
    }
    @Test fun `filter blocks a cookie replayed against a different event`() {
        val request = fake<jakarta.servlet.http.HttpServletRequest> { method ->
            when (method) {
                "getServletPath" -> "/api/v1/participant/2/session"
                "getMethod" -> "GET"
                "getCookies" -> arrayOf(jakarta.servlet.http.Cookie("participant_token", "token"))
                "getDispatcherType" -> jakarta.servlet.DispatcherType.REQUEST
                else -> null
            }
        }
        var status: Int? = null
        val response = Proxy.newProxyInstance(
            jakarta.servlet.http.HttpServletResponse::class.java.classLoader,
            arrayOf(jakarta.servlet.http.HttpServletResponse::class.java),
        ) { _, method, args ->
            if (method.name == "sendError") status = args[0] as Int
            null
        } as jakarta.servlet.http.HttpServletResponse
        var proceeded = false
        try {
            com.momentlink.api.participant.security.ParticipantSessionAuthFilter(service())
                .doFilter(request, response, jakarta.servlet.FilterChain { _, _ -> proceeded = true })
            assertEquals(403, status)
            assertFalse(proceeded)
            assertNull(org.springframework.security.core.context.SecurityContextHolder.getContext().authentication)
        } finally {
            org.springframework.security.core.context.SecurityContextHolder.clearContext()
        }
    }

}
