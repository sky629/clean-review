package com.cleanreview.auth.adapter.`in`.web

import com.cleanreview.auth.application.usecase.LogoutCommand
import com.cleanreview.auth.application.usecase.LogoutUseCase
import com.cleanreview.auth.application.usecase.RefreshTokenCommand
import com.cleanreview.auth.application.usecase.RefreshTokenUseCase
import com.cleanreview.auth.infrastructure.cookie.AuthCookieFactory
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.CookieValue
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

data class AuthTokenResponse(
    val accessToken: String,
    val user: GoogleOAuthUserResponse,
)

@RestController
@RequestMapping("/api/v1/auth")
class AuthTokenController(
    private val refreshTokenUseCase: RefreshTokenUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val authCookieFactory: AuthCookieFactory,
) {
    @PostMapping("/refresh")
    fun refresh(
        @CookieValue(name = AuthCookieFactory.REFRESH_TOKEN_COOKIE_NAME, required = false) refreshToken: String?,
        response: HttpServletResponse,
    ): AuthTokenResponse {
        val result = refreshTokenUseCase.execute(RefreshTokenCommand(refreshToken.orEmpty()))
        authCookieFactory.addRefreshTokenCookie(result.refreshToken, response)
        return AuthTokenResponse(
            accessToken = result.accessToken,
            user = GoogleOAuthUserResponse(
                id = result.user.id.value,
                email = result.user.email,
                role = result.user.role.name,
            ),
        )
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun logout(
        @CookieValue(name = AuthCookieFactory.REFRESH_TOKEN_COOKIE_NAME, required = false) refreshToken: String?,
        response: HttpServletResponse,
    ) {
        if (!refreshToken.isNullOrBlank()) {
            logoutUseCase.execute(LogoutCommand(refreshToken))
        }
        authCookieFactory.deleteRefreshTokenCookie(response)
    }
}
