package com.momentlink.api.event

import com.momentlink.api.auth.common.BusinessException
import com.momentlink.api.common.security.key.KeyHasher
import com.momentlink.api.event.application.PublicEventService
import com.momentlink.api.event.port.out.PublicAccessRepository
import com.momentlink.domain.event.Event
import java.lang.reflect.Proxy
import java.time.Instant
import kotlin.test.*

class PublicEventServiceTest {
    private val hasher = KeyHasher()
    private val event = Event.create(1, "테스트 행사", Instant.parse("2026-10-11T00:00:00Z"), Instant.parse("2026-10-12T00:00:00Z"), hasher.hash("valid-key")).apply {
        Event::class.java.getDeclaredField("id").apply { isAccessible = true }.set(this, 42L)
    }
    private val repository = Proxy.newProxyInstance(
        PublicAccessRepository::class.java.classLoader, arrayOf(PublicAccessRepository::class.java),
    ) { _, method, args ->
        check(method.name == "findByAccessKeyHash")
        if (args[0] == event.publicAccess.accessKeyHash) event.publicAccess else null
    } as PublicAccessRepository
    private val service = PublicEventService(repository, hasher)

    @Test fun `valid key returns event identity and public details`() {
        val result = service.getEventForPublicAccess("valid-key")
        assertEquals(42L, result.eventId)
        assertEquals(event.name, result.name)
        assertEquals(event.startsAt, result.startsAt)
        assertEquals(event.endsAt, result.endsAt)
    }
    @Test fun `unknown key is forbidden`() {
        val error = assertFailsWith<BusinessException> { service.getEventForPublicAccess("wrong") }
        assertEquals("INVALID_EVENT_ACCESS_KEY", error.errorCode.name)
        assertEquals(403, error.errorCode.httpStatus.value())
    }
    @Test fun `disabled public access is forbidden`() {
        event.disablePublicAccess()
        val error = assertFailsWith<BusinessException> { service.getEventForPublicAccess("valid-key") }
        assertEquals("EVENT_ACCESS_DISABLED", error.errorCode.name)
        assertEquals(403, error.errorCode.httpStatus.value())
    }
}
