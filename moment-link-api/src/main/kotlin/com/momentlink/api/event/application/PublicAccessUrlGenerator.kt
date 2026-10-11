package com.momentlink.api.event.application

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.util.UriComponentsBuilder

@Component
class PublicAccessUrlGenerator(
    @Value("\${frontend-url}")
    private val shareBaseUrl: String,
) {

    fun generate(accessKey: String): String {
        return UriComponentsBuilder
            .fromUriString(shareBaseUrl.trimEnd('/'))
            .pathSegment("event", accessKey, "join")
            .build()
            .encode()
            .toUriString()
    }
}
