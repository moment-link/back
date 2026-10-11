package com.momentlink.api.auth.port.`in`

import io.swagger.v3.oas.annotations.Operation
import com.momentlink.api.auth.application.AuthService
import com.momentlink.api.auth.common.web.AuthProperties
import com.momentlink.domain.user.LoginProvider
import com.momentlink.api.auth.dto.RefreshResDto
import com.momentlink.api.auth.port.out.OAuthLoginStateStore
import com.momentlink.api.common.web.CommonResponse
import com.momentlink.api.common.web.CommonResponse.Companion.success
import com.momentlink.api.common.web.REFRESH_TOKEN_COOKIE_NAME
import com.momentlink.api.common.web.TokenCookie
import com.momentlink.api.common.web.TokenCookieProvider
import io.swagger.v3.oas.annotations.Parameter
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseCookie
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CookieValue
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.net.URI
import java.time.Duration

@RestController
@RequestMapping("/api/v1/auth")
class AuthController(
    private val authService: AuthService,
    private val stateStore: OAuthLoginStateStore,
    private val cookieProvider: TokenCookieProvider,
    private val properties: AuthProperties,
) {

    /**
     * 유저 로그인용 OAuth URL 발급
     * 로그인 화면으로 리다이렉트
     * 로그인 요청 시 state값 같이 전송
     * 이후 해당 state값이 돌아오는지 확인 (탈취를 막기 위함)
     */
    @Operation(
        summary = "Google 로그인",
        description = "Google 로그인 화면으로 리다이렉트합니다. provider는 생략 시 GOOGLE이며, 프론트는 브라우저 주소 이동으로 요청합니다.",
    )
    @GetMapping("/login")
    fun login(
        @RequestParam(defaultValue = "GOOGLE") provider: LoginProvider
    ): ResponseEntity<Void> {
        val state = stateStore.issue(provider)
        val authorizationUrl = authService.createAuthorizationUrl(provider, state)

        return ResponseEntity.status(HttpStatus.FOUND)
            .header(HttpHeaders.SET_COOKIE, stateCookie(state, stateStore.ttl).toString())
            .location(URI.create(authorizationUrl))
            .build()
    }

    private fun stateCookie(
        state: String,
        ttl: Duration
    ): ResponseCookie = ResponseCookie.from("oauth_login_state", state)
        .httpOnly(true)
        .secure(true)
        .sameSite("Lax")
        .path("/api/v1/auth/callback")
        .maxAge(ttl)
        .build()

    /**
     * 유저 로그인 OAuth callback
     */
    @Operation(
        summary = "Google OAuth 콜백",
        description = "Google 인증 후 브라우저가 호출하는 경로입니다. state 검증 및 로그인 처리 후 Refresh Token 쿠키를 설정하고 프론트 콜백 페이지로 리다이렉트합니다.",
    )
    @GetMapping("/callback/{provider}")
    fun callback(
        @PathVariable provider: String,
        @RequestParam(required = false) code: String?,
        @RequestParam(required = false) state: String?,
        @RequestParam(required = false) error: String?,
        @CookieValue(name = "oauth_login_state", required = false) cookieState: String?,
        response: HttpServletResponse,
    ): ResponseEntity<Void>  {
        // 로그인 실패 여부와 상관 없이 브라우저 쿠키 삭제.
        response.addHeader(
            HttpHeaders.SET_COOKIE,
            stateCookie("oauth_login_state", Duration.ZERO).toString()
        )

        stateStore.validateAndConsume(provider.uppercase(), state, cookieState)

        if(error != null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        }

        require(!code.isNullOrBlank()) { "Google 인증 코드가 없습니다." }

        val loginResResult = authService.getUserAndLogin(LoginProvider.valueOf(provider.uppercase()), code)
        val loginCookie = cookieProvider.create(
            TokenCookie.REFRESH_TOKEN,
            loginResResult.refreshToken,
            loginResResult.refreshTokenExpiresIn,
        )

        return ResponseEntity.status(HttpStatus.FOUND)
            .header(HttpHeaders.SET_COOKIE, loginCookie.toString())
            .location(URI.create(properties.successRedirectUrl))
            .build()
    }


    /**
     * AccessToken 갱신
     */
    @Operation(
        summary = "Access Token 갱신",
        description = "refresh_token 쿠키로 새 Access Token을 발급하고 Refresh Token을 교체합니다. 로그인 직후에도 호출하며, 요청 본문 없이 쿠키를 포함해야 합니다.",
    )
    @PostMapping("/token/refresh")
    fun refreshToken(
        @Parameter(hidden = true)
        @CookieValue(name = REFRESH_TOKEN_COOKIE_NAME) refreshToken: String
    ): ResponseEntity<CommonResponse<RefreshResDto>> {
        val refreshResResult = authService.refresh(refreshToken)
        val refreshCookie = cookieProvider.create(
            TokenCookie.REFRESH_TOKEN,
            refreshResResult.refreshToken,
            refreshResResult.refreshTokenExpiresIn,
        )

        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
            .body(success(RefreshResDto.from(refreshResResult)))
    }

    /**
     * RefreshToken 제거
     */
    @Operation(
        summary = "로그아웃",
        description = "호스트 Bearer 인증이 필요합니다. 현재 사용자의 Refresh Token을 삭제하고 Refresh Token 쿠키를 만료시킵니다.",
    )
    @PostMapping("/logout")
    fun logout(): ResponseEntity<CommonResponse<Unit>> {
        authService.logout()
        val expiredCookie = cookieProvider.expire(TokenCookie.REFRESH_TOKEN)

        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, expiredCookie.toString())
            .body(success())
    }
}
