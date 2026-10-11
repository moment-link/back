package com.momentlink.api.auth.common.web

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "auth.frontend")
class AuthProperties(
    val successRedirectUrl: String,
)