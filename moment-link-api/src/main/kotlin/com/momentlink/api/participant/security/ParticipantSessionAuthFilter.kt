package com.momentlink.api.participant.security

import com.momentlink.api.auth.common.BusinessException
import com.momentlink.api.common.web.PARTICIPANT_TOKEN_COOKIE_NAME
import com.momentlink.api.common.web.ErrorCode
import com.momentlink.api.participant.application.ParticipantSessionAuthService
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class ParticipantSessionAuthFilter(
    private val participantSessionAuthService: ParticipantSessionAuthService
): OncePerRequestFilter() {

    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        val path = request.servletPath

        val isParticipantApi = path.startsWith("/api/v1/participant/")
        val isSessionIssue = request.method == "POST" &&
                Regex("^/api/v1/participant/[0-9]+/session$").matches(path)

        return !isParticipantApi || isSessionIssue
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val token = resolveCookieToken(request)

        if (token != null) {
            try {
                val eventId = request.servletPath.removePrefix("/api/v1/participant/")
                    .substringBefore('/').toLongOrNull()
                    ?: throw BusinessException(ErrorCode.PARTICIPANT_EVENT_MISMATCH)
                val principal = participantSessionAuthService.authenticate(token, eventId)

                val authentication = UsernamePasswordAuthenticationToken(
                    principal, null, listOf(SimpleGrantedAuthority("ROLE_PARTICIPANT"))
                )

                val context = SecurityContextHolder.createEmptyContext()
                context.authentication = authentication
                SecurityContextHolder.setContext(context)
            } catch (ex: BusinessException) {
                SecurityContextHolder.clearContext()
                response.sendError(ex.errorCode.httpStatus.value(), ex.errorCode.message)
                return
            }
        }

        filterChain.doFilter(request, response)
    }

    private fun resolveCookieToken(request: HttpServletRequest): String? =
        request.cookies
            ?.firstOrNull { it.name == PARTICIPANT_TOKEN_COOKIE_NAME }
            ?.value
            ?.takeIf(String::isNotBlank)
}
