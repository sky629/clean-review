package com.cleanreview.auth.application.usecase

import com.cleanreview.auth.application.port.out.GoogleOAuthClient
import com.cleanreview.auth.domain.User
import org.springframework.stereotype.Service

data class GoogleOAuthLoginCommand(
    val code: String,
    val redirectUri: String,
)

data class GoogleOAuthLoginResult(
    val accessToken: String,
    val refreshToken: String,
    val user: User,
)

@Service
class GoogleOAuthLoginUseCase(
    private val googleOAuthClient: GoogleOAuthClient,
    private val googleOAuthProfileLoginService: GoogleOAuthProfileLoginService,
) {
    fun execute(command: GoogleOAuthLoginCommand): GoogleOAuthLoginResult {
        require(command.code.isNotBlank()) { "OAuth code must not be blank." }
        require(command.redirectUri.isNotBlank()) { "Redirect URI must not be blank." }

        val profile = googleOAuthClient.fetchProfile(command.code, command.redirectUri)
        return googleOAuthProfileLoginService.login(profile)
    }
}
