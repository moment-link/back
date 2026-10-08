package com.momentlink.admin

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.env.YamlPropertySourceLoader
import org.springframework.core.io.ClassPathResource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AdminApplicationTest {
    @Test
    fun `application entry point is configured`() {
        assertTrue(AdminApplication::class.java.isAnnotationPresent(SpringBootApplication::class.java))
    }

    @Test
    fun `application name is configured`() {
        val propertySource = YamlPropertySourceLoader()
            .load("application", ClassPathResource("application.yaml"))
            .single()

        assertEquals("moment-link-admin", propertySource.getProperty("spring.application.name"))
    }
}
