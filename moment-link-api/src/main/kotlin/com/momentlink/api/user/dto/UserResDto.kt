package com.momentlink.api.user.dto

import com.momentlink.domain.user.User
import com.momentlink.domain.user.UserStatus

data class UserResDto(
    val id: Long?,
    val email: String,
    val name: String?,
    val status: UserStatus
) {
    companion object {
        fun from(
            user: User
        ): UserResDto = UserResDto(
            user.id,
            user.email,
            user.name,
            user.status
        )
    }
}
