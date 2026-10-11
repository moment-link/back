package com.momentlink.api.auth.common.security

import com.momentlink.api.auth.application.JwtProvider
import com.momentlink.api.user.port.out.UserRepository
import com.momentlink.domain.user.UserStatus
import org.springframework.data.repository.findByIdOrNull
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthFilter(
    private val jwtProvider: JwtProvider,
    private val userRepository: UserRepository,
): OncePerRequestFilter() {

    override fun shouldNotFilter(request: HttpServletRequest): Boolean =
        request.servletPath.startsWith("/api/v1/upload")

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val token = resolveToken(request)

        if(token != null && SecurityContextHolder.getContext().authentication == null) {
            authenticate(token, request)
        }

        filterChain.doFilter(request, response)
    }

    private fun authenticate(token: String, request: HttpServletRequest) {
        try {
            val userId = jwtProvider.extractUserId(token, JwtTokenType.ACCESS)
            val user = userRepository.findByIdOrNull(userId)
            if (user == null || user.status != UserStatus.ACTIVE) {
                SecurityContextHolder.clearContext()
                return
            }

            val principal = AuthenticatedUser.create(userId)
            val authentication = UsernamePasswordAuthenticationToken(principal, null, listOf(SimpleGrantedAuthority("ROLE_HOST")))
                .apply {
                    details = WebAuthenticationDetailsSource().buildDetails(request)
                }

            val context = SecurityContextHolder.createEmptyContext()
            context.authentication = authentication
            SecurityContextHolder.setContext(context)
        } catch (_: InvalidJwtTokenException) {
            SecurityContextHolder.clearContext()
        }
    }

    private fun resolveToken(request: HttpServletRequest): String? =
        request.getHeader(HttpHeaders.AUTHORIZATION)
            ?.takeIf { it.startsWith(prefix = BEARER_PREFIX, ignoreCase = true) }
            ?.drop(BEARER_PREFIX.length)
            ?.trim()
            ?.takeIf(String::isNotBlank)

    companion object {
        private const val BEARER_PREFIX = "Bearer "
    }
}
