package com.momentlink.api.auth

import com.momentlink.api.auth.common.BusinessException
import com.momentlink.api.auth.infra.jpa.JpaOAuthLoginStateStore
import com.momentlink.api.auth.port.out.OAuthLoginStateRepository
import com.momentlink.api.common.web.ErrorCode
import com.momentlink.domain.common.OAuthLoginState
import com.momentlink.domain.user.LoginProvider
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.persistence.autoconfigure.EntityScan
import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.test.context.ContextConfiguration
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.Instant
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

@DataJpaTest
@ContextConfiguration(classes = [JpaOAuthLoginStateStoreTest.Config::class, JpaOAuthLoginStateStore::class])
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class JpaOAuthLoginStateStoreTest {
    @Configuration(proxyBeanMethods = false)
    @EntityScan(basePackages = ["com.momentlink.domain"])
    @EnableJpaRepositories(basePackageClasses = [OAuthLoginStateRepository::class])
    class Config

    @Autowired
    private lateinit var store: JpaOAuthLoginStateStore

    @Autowired
    private lateinit var repository: OAuthLoginStateRepository

    @AfterTest
    fun cleanUp() = repository.deleteAllInBatch()

    @Test
    fun `issued state is random and stored for five minutes`() {
        val before = Instant.now()
        val state = store.issue(LoginProvider.GOOGLE)
        val after = Instant.now()
        val stored = repository.findById(state).orElseThrow()

        assertTrue(state.matches(Regex("[A-Za-z0-9_-]{43}")))
        assertNotEquals(state, store.issue(LoginProvider.GOOGLE))
        assertEquals(LoginProvider.GOOGLE, stored.provider)
        assertEquals(Duration.ofMinutes(5), store.ttl)
        assertTrue(stored.expiresAt >= before.plus(store.ttl).minusMillis(1))
        assertTrue(stored.expiresAt <= after.plus(store.ttl).plusMillis(1))
    }

    @Test
    fun `valid state can only be consumed once`() {
        val state = store.issue(LoginProvider.GOOGLE)

        store.validateAndConsume("GOOGLE", state, state)

        assertFalse(repository.existsById(state))
        assertInvalid { store.validateAndConsume("GOOGLE", state, state) }
    }

    @Test
    fun `expired state is rejected and consumed`() {
        val state = "expired"
        repository.save(OAuthLoginState(state, LoginProvider.GOOGLE, Instant.now().minusSeconds(1)))

        assertInvalid { store.validateAndConsume("GOOGLE", state, state) }
        assertFalse(repository.existsById(state))
    }

    @Test
    fun `wrong provider is rejected and still consumes the state`() {
        val state = store.issue(LoginProvider.GOOGLE)

        assertInvalid { store.validateAndConsume("NAVER", state, state) }
        assertFalse(repository.existsById(state))
        assertInvalid { store.validateAndConsume("GOOGLE", state, state) }
    }

    @Test
    fun `missing blank or mismatched cookies do not consume the state`() {
        val state = store.issue(LoginProvider.GOOGLE)

        for ((query, cookie) in listOf(null to state, "" to state, " " to state, state to null, state to "", state to "other")) {
            assertInvalid { store.validateAndConsume("GOOGLE", query, cookie) }
        }

        store.validateAndConsume("GOOGLE", state, state)
    }

    @Test
    fun `unknown state is rejected`() {
        assertInvalid { store.validateAndConsume("GOOGLE", "unknown", "unknown") }
    }

    @Test
    fun `issuing a state cleans up expired states and preserves pending logins`() {
        val pending = store.issue(LoginProvider.GOOGLE)
        repository.save(OAuthLoginState("expired", LoginProvider.GOOGLE, Instant.now().minusSeconds(1)))

        val issued = store.issue(LoginProvider.NAVER)

        assertFalse(repository.existsById("expired"))
        store.validateAndConsume("GOOGLE", pending, pending)
        store.validateAndConsume("NAVER", issued, issued)
    }

    @Test
    fun `concurrent callbacks only consume the state once`() {
        val state = store.issue(LoginProvider.GOOGLE)
        val ready = CountDownLatch(2)
        val start = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(2)
        try {
            val results = List(2) {
                executor.submit<Boolean> {
                    ready.countDown()
                    check(start.await(5, TimeUnit.SECONDS))
                    try {
                        store.validateAndConsume("GOOGLE", state, state)
                        true
                    } catch (ex: BusinessException) {
                        assertEquals(ErrorCode.INVALID_OAUTH_STATE, ex.errorCode)
                        false
                    }
                }
            }
            assertTrue(ready.await(5, TimeUnit.SECONDS))
            start.countDown()

            assertEquals(1, results.count { it.get(10, TimeUnit.SECONDS) })
            assertFalse(repository.existsById(state))
        } finally {
            start.countDown()
            executor.shutdownNow()
        }
    }

    private fun assertInvalid(block: () -> Unit) {
        assertEquals(ErrorCode.INVALID_OAUTH_STATE, assertFailsWith<BusinessException>(block = block).errorCode)
    }
}
