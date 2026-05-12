package com.cleanreview.auth.application.usecase

import com.cleanreview.auth.application.port.out.GoogleOAuthProfile
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.stereotype.Service

@Service
class GoogleOAuthUserLoginUseCase(
    private val googleOAuthProfileLoginService: GoogleOAuthProfileLoginService,
) {
    fun execute(oauth2User: OAuth2User): GoogleOAuthLoginResult {
        val email = oauth2User.getAttribute<String>("email")
            ?: throw IllegalArgumentException("Google OAuth user did not include email.")
        val providerUserId = oauth2User.getAttribute<String>("sub")
            ?: oauth2User.name
        val displayName = oauth2User.getAttribute<String>("name") ?: email

        return googleOAuthProfileLoginService.login(
            GoogleOAuthProfile(
                providerUserId = providerUserId,
                email = email,
                displayName = displayName,
                profileImageUrl = oauth2User.getAttribute<String>("picture"),
            ),
        )
    }
}
