package com.momentlink.transfer

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.env.YamlPropertySourceLoader
import org.springframework.core.io.ClassPathResource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MediaTransferApplicationTest {
    @Test
    fun `application entry point is configured`() {
        assertTrue(MediaTransferApplication::class.java.isAnnotationPresent(SpringBootApplication::class.java))
    }

    @Test
    fun `application name is configured`() {
        val propertySource = YamlPropertySourceLoader()
            .load("application", ClassPathResource("application.yaml"))
            .single()

        assertEquals("moment-link-sync", propertySource.getProperty("spring.application.name"))
    }
}
