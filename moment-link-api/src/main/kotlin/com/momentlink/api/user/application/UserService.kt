package com.momentlink.api.user.application

import com.momentlink.api.auth.common.BusinessException
import com.momentlink.api.auth.port.out.RefreshTokenRepository
import com.momentlink.api.common.security.UserProvider
import com.momentlink.api.common.web.ErrorCode
import com.momentlink.api.user.dto.UserResDto
import com.momentlink.api.user.dto.UserUpdateReqDto
import com.momentlink.api.user.port.out.UserRepository
import com.momentlink.domain.user.User
import com.momentlink.domain.user.UserStatus
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class UserService(
    private val userRepository: UserRepository,
    private val userProvider: UserProvider,
    private val refreshTokenRepository: RefreshTokenRepository,
) {

    fun getUser(): UserResDto {
        return UserResDto.from(getCurrentUser())
    }

    @Transactional
    fun updateUser(req: UserUpdateReqDto): UserResDto {
        val user = getCurrentUserForUpdate()

        user.updateName(req.name)
        return UserResDto.from(user)
    }

    @Transactional
    fun deleteUser() {
        val user = getCurrentUserForUpdate()
        user.withdraw()
        refreshTokenRepository.deleteByUserId(user.requiredId())
    }

    fun getCurrentUser(): User =
        requireActiveUser(userRepository.findByIdOrNull(userProvider.getUserId()))

    private fun getCurrentUserForUpdate(): User =
        requireActiveUser(userRepository.findByIdForUpdate(userProvider.getUserId()))

    private fun requireActiveUser(user: User?): User {
        if (user == null) throw BusinessException(ErrorCode.NOT_FOUND_USER)
        if (user.status != UserStatus.ACTIVE) {
            throw BusinessException(ErrorCode.INACTIVE_ACCOUNT)
        }
        return user
    }
}
