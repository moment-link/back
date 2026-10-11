package com.momentlink.api.auth.application

import com.momentlink.api.auth.application.dto.LoginResResult
import com.momentlink.api.auth.common.BusinessException
import com.momentlink.api.common.web.ErrorCode
import com.momentlink.api.auth.application.dto.RefreshResResult
import com.momentlink.api.auth.application.login.OAuthLoginClient
import com.momentlink.domain.common.RefreshToken
import com.momentlink.api.auth.common.security.InvalidJwtTokenException
import com.momentlink.api.auth.common.security.JwtTokenType
import com.momentlink.domain.user.LoginProvider
import com.momentlink.api.auth.port.out.RefreshTokenRepository
import com.momentlink.api.common.security.UserProvider
import com.momentlink.api.user.port.out.UserRepository
import com.momentlink.domain.user.UserStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AuthService(
    clients: List<OAuthLoginClient>,
    private val authLoginService: AuthLoginService,
    private val jwtProvider: JwtProvider,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val userRepository: UserRepository,
    private val userProvider: UserProvider,
) {
    // 각각의 로그인 provider 구현체를 spring bean으로 등록
    private val clients: Map<LoginProvider, OAuthLoginClient> =
        clients.associateBy { it.provider }

    // 로그인 url 제공
    fun createAuthorizationUrl(provider: LoginProvider, state: String): String =
        resolveClient(provider).createAuthorizationUrl(state)

    // 유저 create or find 후 인증token 반환
    fun getUserAndLogin(provider:LoginProvider, code: String): LoginResResult {
        val email = resolveClient(provider).getUserProfile(code).email.trim().lowercase()
        return authLoginService.login(email, provider)
    }

    // refreshToken 검증 후, 새로운 token생성
    @Transactional
    fun refresh(token: String): RefreshResResult {
        val userId = try {
            jwtProvider.extractUserId(token, JwtTokenType.REFRESH)
        } catch (_: InvalidJwtTokenException) {
            throw RuntimeException("유효하지 않은 refresh token입니다.")
        }

        val storedToken = refreshTokenRepository.findByTokenForUpdate(token)
            ?: throw BusinessException(ErrorCode.REFRESH_TOKEN_NOT_FOUND)

        if(storedToken.userId != userId) {
            throw RuntimeException("사용자 정보가 일치하지 않습니다.")
        }

        val user = userRepository.findById(userId)
            .orElseThrow { RuntimeException("존재하지 않는 사용자입니다.") }

        if(user.status != UserStatus.ACTIVE) {
            throw RuntimeException("토큰을 재발급할 수 없습니다.")
        }

        refreshTokenRepository.delete(storedToken)

        val newAccessToken = jwtProvider.createAccessToken(userId)
        val newRefreshToken = jwtProvider.createRefreshToken(userId)

        refreshTokenRepository.save(RefreshToken.create(newRefreshToken.expiresAt, userId, newRefreshToken.token))

        return RefreshResResult.from(
            newAccessToken.token,
            newAccessToken.expiresAt,
            newRefreshToken.token,
            newRefreshToken.expiresAt)
    }

    // 유저 검증 후, refreshToken 삭제
    @Transactional
    fun logout() {
        val userId = userProvider.getUserId()
        refreshTokenRepository.deleteByUserId(userId)
    }

    // provider 구현체 resolver
    private fun resolveClient(provider: LoginProvider): OAuthLoginClient {
        return requireNotNull(clients[provider]) { "지원하지 않는 OAuth provider입니다: $provider" }
    }
}
