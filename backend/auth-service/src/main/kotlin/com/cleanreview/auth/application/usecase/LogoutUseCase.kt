package com.cleanreview.auth.application.usecase

import com.cleanreview.auth.application.port.out.RefreshTokenStore
import org.springframework.stereotype.Service

data class LogoutCommand(
    val refreshToken: String,
)

@Service
class LogoutUseCase(
    private val refreshTokenStore: RefreshTokenStore,
) {
    fun execute(command: LogoutCommand) {
        require(command.refreshToken.isNotBlank()) { "Refresh token must not be blank." }
        refreshTokenStore.deleteRefreshToken(RefreshTokenHasher.sha256(command.refreshToken))
    }
}
