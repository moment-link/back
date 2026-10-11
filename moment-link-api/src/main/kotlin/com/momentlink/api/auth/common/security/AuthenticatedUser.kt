package com.momentlink.api.auth.common.security

class AuthenticatedUser private constructor(
    val userId: Long,
) {
    companion object {
        fun create(userId: Long): AuthenticatedUser = AuthenticatedUser(userId)
    }
}