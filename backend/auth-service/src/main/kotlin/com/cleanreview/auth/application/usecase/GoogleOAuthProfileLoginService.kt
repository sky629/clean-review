package com.cleanreview.auth.application.usecase

import com.cleanreview.auth.application.port.out.AuthUserRepository
import com.cleanreview.auth.application.port.out.GoogleOAuthProfile
import com.cleanreview.auth.application.port.out.JwtTokenIssuer
import com.cleanreview.auth.application.port.out.RefreshTokenStore
import com.cleanreview.auth.domain.SocialProvider
import com.cleanreview.auth.domain.User
import com.cleanreview.auth.domain.UserId
import java.util.UUID
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class GoogleOAuthProfileLoginService(
    private val authUserRepository: AuthUserRepository,
    private val jwtTokenIssuer: JwtTokenIssuer,
    private val refreshTokenStore: RefreshTokenStore,
) {
    @Transactional
    fun login(profile: GoogleOAuthProfile): GoogleOAuthLoginResult {
        val user = authUserRepository.findBySocialAccount(
            provider = SocialProvider.GOOGLE,
            providerUserId = profile.providerUserId,
        ) ?: run {
            val existingUser = authUserRepository.findByEmail(profile.email)
            val userToSave = (existingUser ?: User.register(
                id = UserId(UUID.randomUUID().toString()),
                email = profile.email,
                displayName = profile.displayName,
            )).linkSocialAccount(
                provider = SocialProvider.GOOGLE,
                providerUserId = profile.providerUserId,
            )
            authUserRepository.save(userToSave)
        }

        val tokens = jwtTokenIssuer.issue(user)
        refreshTokenStore.saveRefreshToken(
            tokenHash = tokens.refreshTokenHash,
            userId = user.id.value,
            ttlSeconds = tokens.refreshTokenTtlSeconds,
        )

        return GoogleOAuthLoginResult(
            accessToken = tokens.accessToken,
            refreshToken = tokens.refreshToken,
            user = user,
        )
    }
}
