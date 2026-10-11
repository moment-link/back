package com.momentlink.api.user.port.`in`

import io.swagger.v3.oas.annotations.Operation
import com.momentlink.api.common.web.CommonResponse
import com.momentlink.api.common.web.CommonResponse.Companion.success
import com.momentlink.api.common.web.TokenCookie
import com.momentlink.api.common.web.TokenCookieProvider
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import com.momentlink.api.user.application.UserService
import com.momentlink.api.user.dto.UserResDto
import com.momentlink.api.user.dto.UserUpdateReqDto
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/users")
class UserController(
    private val userService: UserService,
    private val cookieProvider: TokenCookieProvider,
) {
    @Operation(
        summary = "내 정보 조회",
        description = "호스트 Bearer 인증으로 현재 로그인한 사용자의 정보를 조회합니다.",
    )
    @GetMapping("/me")
    fun getMe(): CommonResponse<UserResDto> {
        return success(userService.getUser())
    }

    @Operation(
        summary = "내 정보 변경",
        description = "호스트 Bearer 인증으로 현재 사용자의 이름을 변경합니다. 요청 본문에 name을 전달합니다.",
    )
    @PatchMapping("/me")
    fun updateMe(@RequestBody req: UserUpdateReqDto): CommonResponse<UserResDto> {
        return success(userService.updateUser(req))
    }

    @Operation(
        summary = "회원 탈퇴",
        description = "호스트 Bearer 인증으로 현재 사용자를 탈퇴 처리하고 Refresh Token을 삭제하며 쿠키를 만료시킵니다.",
    )
    @DeleteMapping("/me")
    fun deleteMe(): ResponseEntity<CommonResponse<Unit>> {
        userService.deleteUser()
        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, cookieProvider.expire(TokenCookie.REFRESH_TOKEN).toString())
            .body(success())
    }
}
