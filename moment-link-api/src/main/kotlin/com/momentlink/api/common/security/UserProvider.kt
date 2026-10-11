package com.momentlink.api.common.security

import com.momentlink.api.auth.common.security.AuthenticatedUser
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component

@Component
class UserProvider {

    fun get(): AuthenticatedUser {
        val authentication =
            SecurityContextHolder.getContext().authentication

        // Authentication이 있는가? 인증 완료상태인가? anonymous 사용자가 아닌가?
        check(authentication != null &&
            authentication.isAuthenticated &&
            authentication !is AnonymousAuthenticationToken) {
            "인증된 사용자가 없습니다."
        }

        return authentication.principal as? AuthenticatedUser ?: error("올바르지 않은 인증 Principal입니다.")
    }

    fun getUserId(): Long =
        get().userId
}