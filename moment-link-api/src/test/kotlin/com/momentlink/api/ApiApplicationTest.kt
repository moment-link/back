package com.momentlink.api

import org.springframework.boot.autoconfigure.SpringBootApplication
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ApiApplicationTest {
    @Test
    fun `application entry point is configured`() {
        assertTrue(ApiApplication::class.java.isAnnotationPresent(SpringBootApplication::class.java))
    }

    @Test
    fun `application name is configured`() {
        val applicationYaml = assertNotNull(javaClass.classLoader.getResource("application.yaml")).readText()

        assertTrue(applicationYaml.contains("name: moment-link-api"))
    }
}
