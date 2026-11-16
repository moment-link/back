package com.momentlink.api.participant.security

import com.momentlink.api.auth.common.BusinessException
import com.momentlink.api.common.web.PARTICIPANT_TOKEN_COOKIE_NAME
import com.momentlink.api.common.web.ErrorCode
import com.momentlink.api.common.web.ErrorResponseWriter
import com.momentlink.api.participant.application.ParticipantSessionAuthService
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class ParticipantSessionAuthFilter(
    private val participantSessionAuthService: ParticipantSessionAuthService,
    private val errorResponseWriter: ErrorResponseWriter,
): OncePerRequestFilter() {
    private val log = LoggerFactory.getLogger(javaClass)

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
                // 필터 단계 예외는 GlobalExceptionHandler에 닿지 않으므로 같은 형식으로 직접 남기고 응답한다.
                log.warn("[{}] {} {} - {}", ex.errorCode.code, request.method, request.requestURI, ex.reason ?: ex.errorCode.message)
                SecurityContextHolder.clearContext()
                errorResponseWriter.write(response, ex.errorCode)
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
