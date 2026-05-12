package com.cleanreview.auth.infrastructure.security

import com.cleanreview.auth.application.port.out.OAuthLoginCodePayload
import com.cleanreview.auth.application.port.out.OAuthLoginCodeStore
import com.cleanreview.auth.application.usecase.GoogleOAuthUserLoginUseCase
import com.cleanreview.auth.application.usecase.OAuthLoginCodeHasher
import com.cleanreview.auth.infrastructure.cookie.AuthCookieFactory
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import java.util.UUID
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler
import org.springframework.stereotype.Component
import org.springframework.web.util.UriComponentsBuilder

@Component
class OAuth2LoginSuccessHandler(
    private val googleOAuthUserLoginUseCase: GoogleOAuthUserLoginUseCase,
    private val authCookieFactory: AuthCookieFactory,
    private val oauthLoginCodeStore: OAuthLoginCodeStore,
    @Value("\${clean-review.frontend.auth-success-uri:http://localhost:5173/oauth/google/callback}")
    private val authSuccessUri: String,
) : SimpleUrlAuthenticationSuccessHandler() {
    override fun onAuthenticationSuccess(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authentication: Authentication,
    ) {
        val oauth2User = authentication.principal as OAuth2User
        val loginResult = googleOAuthUserLoginUseCase.execute(oauth2User)
        val loginCode = UUID.randomUUID().toString()

        authCookieFactory.addRefreshTokenCookie(loginResult.refreshToken, response)
        oauthLoginCodeStore.saveLoginCode(
            codeHash = OAuthLoginCodeHasher.sha256(loginCode),
            payload = OAuthLoginCodePayload(
                accessToken = loginResult.accessToken,
                userId = loginResult.user.id.value,
                email = loginResult.user.email,
                role = loginResult.user.role,
            ),
            ttlSeconds = 60,
        )

        response.sendRedirect(
            UriComponentsBuilder.fromUriString(authSuccessUri)
                .queryParam("code", loginCode)
                .build()
                .toUriString(),
        )
    }
}
