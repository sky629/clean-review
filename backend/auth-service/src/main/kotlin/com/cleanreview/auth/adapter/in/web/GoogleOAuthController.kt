package com.cleanreview.auth.adapter.`in`.web

import com.cleanreview.auth.application.usecase.BuildGoogleOAuthAuthorizeUrlCommand
import com.cleanreview.auth.application.usecase.BuildGoogleOAuthAuthorizeUrlUseCase
import com.cleanreview.auth.application.usecase.ExchangeOAuthLoginCodeCommand
import com.cleanreview.auth.application.usecase.ExchangeOAuthLoginCodeUseCase
import com.cleanreview.auth.application.usecase.GoogleOAuthLoginCommand
import com.cleanreview.auth.application.usecase.GoogleOAuthLoginUseCase
import com.cleanreview.auth.infrastructure.cookie.AuthCookieFactory
import jakarta.servlet.http.HttpServletResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

data class GoogleOAuthLoginRequest(
    val code: String,
    val redirectUri: String,
)

data class GoogleOAuthLoginResponse(
    val accessToken: String,
    val user: GoogleOAuthUserResponse,
)

data class ExchangeOAuthLoginCodeRequest(
    val code: String,
)

data class ExchangeOAuthLoginCodeResponse(
    val accessToken: String,
    val user: GoogleOAuthUserResponse,
)

data class GoogleOAuthUserResponse(
    val id: String,
    val email: String,
    val role: String,
)

data class GoogleOAuthAuthorizeUrlResponse(
    val authorizationUrl: String,
)

@RestController
@RequestMapping("/api/v1/auth/google")
class GoogleOAuthController(
    private val googleOAuthLoginUseCase: GoogleOAuthLoginUseCase,
    private val buildGoogleOAuthAuthorizeUrlUseCase: BuildGoogleOAuthAuthorizeUrlUseCase,
    private val exchangeOAuthLoginCodeUseCase: ExchangeOAuthLoginCodeUseCase,
    private val authCookieFactory: AuthCookieFactory,
) {
    @GetMapping("/authorize-url")
    fun authorizeUrl(@RequestParam redirectUri: String): GoogleOAuthAuthorizeUrlResponse {
        val result = buildGoogleOAuthAuthorizeUrlUseCase.execute(
            BuildGoogleOAuthAuthorizeUrlCommand(redirectUri),
        )
        return GoogleOAuthAuthorizeUrlResponse(result.authorizationUrl)
    }

    @PostMapping("/login")
    fun login(
        @RequestBody request: GoogleOAuthLoginRequest,
        response: HttpServletResponse,
    ): GoogleOAuthLoginResponse {
        val result = googleOAuthLoginUseCase.execute(
            GoogleOAuthLoginCommand(
                code = request.code,
                redirectUri = request.redirectUri,
            ),
        )
        authCookieFactory.addRefreshTokenCookie(result.refreshToken, response)

        return GoogleOAuthLoginResponse(
            accessToken = result.accessToken,
            user = GoogleOAuthUserResponse(
                id = result.user.id.value,
                email = result.user.email,
                role = result.user.role.name,
            ),
        )
    }

    @PostMapping("/login-code/exchange")
    fun exchangeLoginCode(@RequestBody request: ExchangeOAuthLoginCodeRequest): ExchangeOAuthLoginCodeResponse {
        val result = exchangeOAuthLoginCodeUseCase.execute(ExchangeOAuthLoginCodeCommand(request.code))

        return ExchangeOAuthLoginCodeResponse(
            accessToken = result.accessToken,
            user = GoogleOAuthUserResponse(
                id = result.userId,
                email = result.email,
                role = result.role,
            ),
        )
    }
}
