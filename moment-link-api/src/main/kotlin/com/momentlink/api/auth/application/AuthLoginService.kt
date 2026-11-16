package com.momentlink.api.auth.application

import com.momentlink.api.auth.application.dto.LoginResResult
import com.momentlink.api.auth.common.BusinessException
import com.momentlink.api.common.web.ErrorCode
import com.momentlink.domain.user.UserStatus
import com.momentlink.domain.common.RefreshToken
import com.momentlink.api.auth.port.out.RefreshTokenRepository
import com.momentlink.api.user.port.out.UserRepository
import com.momentlink.domain.user.LoginProvider
import com.momentlink.domain.user.User
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AuthLoginService(
    private val userRepository: UserRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val jwtProvider: JwtProvider
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun login(email: String, provider: LoginProvider): LoginResResult {
        // email에 대해 유저가 있으면 사용, 없으면 생성
        val existingUser = userRepository.findByEmail(email)
        val user = existingUser ?: userRepository.save(User.create(email, provider))
        if (user.status != UserStatus.ACTIVE) {
            throw BusinessException(ErrorCode.INACTIVE_ACCOUNT, reason = "userId=${user.id}, status=${user.status}")
        }

        val result = createAuthLoginRes(user)
        log.info("로그인 성공: userId={}, provider={}, newUser={}", user.id, provider, existingUser == null)
        return result
    }

    // user정보로 access, refresh 토큰 생성 및 refresh token 저장
    private fun createAuthLoginRes(user: User): LoginResResult {
        val userId = checkNotNull(user.id) { "JWT 발급 전에 User가 저장되어 있어야 합니다." }
        val refreshToken = jwtProvider.createRefreshToken(userId)

        refreshTokenRepository.save(RefreshToken.create(refreshToken.expiresAt, userId, refreshToken.token))
        return LoginResResult.from(refreshToken.token, refreshToken.expiresAt)
    }
}
