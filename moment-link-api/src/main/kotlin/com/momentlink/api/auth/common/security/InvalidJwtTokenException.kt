package com.momentlink.api.auth.common.security

class InvalidJwtTokenException(
    message: String,
    cause: Throwable? = null,
): RuntimeException(message, cause)