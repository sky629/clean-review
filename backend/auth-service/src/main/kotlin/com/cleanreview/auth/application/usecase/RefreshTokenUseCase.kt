package com.cleanreview.auth.application.usecase

import com.cleanreview.auth.application.port.out.AuthUserRepository
import com.cleanreview.auth.application.port.out.JwtTokenIssuer
import com.cleanreview.auth.application.port.out.RefreshTokenStore
import com.cleanreview.auth.domain.User
import com.cleanreview.auth.domain.UserId
import java.security.MessageDigest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

data class RefreshTokenCommand(
    val refreshToken: String,
)

data class RefreshTokenResult(
    val accessToken: String,
    val refreshToken: String,
    val user: User,
)

object RefreshTokenHasher {
    fun sha256(value: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray())
            .joinToString("") { "%02x".format(it) }
}

@Service
class RefreshTokenUseCase(
    private val authUserRepository: AuthUserRepository,
    private val jwtTokenIssuer: JwtTokenIssuer,
    private val refreshTokenStore: RefreshTokenStore,
) {
    @Transactional
    fun execute(command: RefreshTokenCommand): RefreshTokenResult {
        require(command.refreshToken.isNotBlank()) { "Refresh token must not be blank." }

        val oldTokenHash = RefreshTokenHasher.sha256(command.refreshToken)
        val userId = refreshTokenStore.findUserIdByRefreshTokenHash(oldTokenHash)
            ?: throw InvalidRefreshTokenException()
        val user = authUserRepository.findById(UserId(userId))
            ?: throw InvalidRefreshTokenException()
        val tokens = jwtTokenIssuer.issue(user)

        refreshTokenStore.deleteRefreshToken(oldTokenHash)
        refreshTokenStore.saveRefreshToken(
            tokenHash = tokens.refreshTokenHash,
            userId = user.id.value,
            ttlSeconds = tokens.refreshTokenTtlSeconds,
        )

        return RefreshTokenResult(
            accessToken = tokens.accessToken,
            refreshToken = tokens.refreshToken,
            user = user,
        )
    }
}

class InvalidRefreshTokenException : RuntimeException("Invalid refresh token.")
