package com.cleanreview.auth.application.usecase

import com.cleanreview.auth.application.port.out.AuthUserRepository
import com.cleanreview.auth.application.port.out.GoogleOAuthClient
import com.cleanreview.auth.application.port.out.GoogleOAuthProfile
import com.cleanreview.auth.application.port.out.IssuedTokens
import com.cleanreview.auth.application.port.out.JwtTokenIssuer
import com.cleanreview.auth.application.port.out.RefreshTokenStore
import com.cleanreview.auth.domain.SocialProvider
import com.cleanreview.auth.domain.User
import com.cleanreview.auth.domain.UserId
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import org.junit.jupiter.api.Test

class GoogleOAuthLoginUseCaseTest {
    @Test
    fun `registers new user links google account issues tokens and stores refresh token`() {
        val userRepository = InMemoryAuthUserRepository()
        val refreshTokenStore = RecordingRefreshTokenStore()
        val useCase = GoogleOAuthLoginUseCase(
            googleOAuthClient = FakeGoogleOAuthClient(),
            googleOAuthProfileLoginService = GoogleOAuthProfileLoginService(
                authUserRepository = userRepository,
                jwtTokenIssuer = FakeJwtTokenIssuer(),
                refreshTokenStore = refreshTokenStore,
            ),
        )

        val result = useCase.execute(
            GoogleOAuthLoginCommand(
                code = "oauth-code",
                redirectUri = "http://localhost:5173/oauth/google/callback",
            ),
        )

        assertEquals("access-token", result.accessToken)
        assertEquals("refresh-token", result.refreshToken)
        assertEquals("owner@example.com", result.user.email)
        assertEquals("Owner", result.user.displayName)
        assertEquals(SocialProvider.GOOGLE, result.user.socialAccounts.single().provider)
        assertEquals("google-123", result.user.socialAccounts.single().providerUserId)
        assertEquals(listOf("refresh-hash" to result.user.id.value), refreshTokenStore.saved)
    }

    @Test
    fun `reuses existing user by google social account`() {
        val existing = User.register(
            id = UserId("user-1"),
            email = "owner@example.com",
            displayName = "Owner",
        ).linkSocialAccount(SocialProvider.GOOGLE, "google-123")
        val userRepository = InMemoryAuthUserRepository(existing)

        val result = GoogleOAuthLoginUseCase(
            googleOAuthClient = FakeGoogleOAuthClient(),
            googleOAuthProfileLoginService = GoogleOAuthProfileLoginService(
                authUserRepository = userRepository,
                jwtTokenIssuer = FakeJwtTokenIssuer(),
                refreshTokenStore = RecordingRefreshTokenStore(),
            ),
        ).execute(GoogleOAuthLoginCommand("oauth-code", "redirect-uri"))

        assertEquals("user-1", result.user.id.value)
        assertEquals(1, userRepository.findAll().size)
    }
}

private class FakeGoogleOAuthClient : GoogleOAuthClient {
    override fun fetchProfile(code: String, redirectUri: String): GoogleOAuthProfile =
        GoogleOAuthProfile(
            providerUserId = "google-123",
            email = "owner@example.com",
            displayName = "Owner",
            profileImageUrl = "https://example.com/profile.png",
        )
}

private class FakeJwtTokenIssuer : JwtTokenIssuer {
    override fun issue(user: User): IssuedTokens =
        IssuedTokens(
            accessToken = "access-token",
            refreshToken = "refresh-token",
            refreshTokenHash = "refresh-hash",
            refreshTokenTtlSeconds = 2_592_000,
        )
}

private class RecordingRefreshTokenStore : RefreshTokenStore {
    val saved = mutableListOf<Pair<String, String>>()

    override fun saveRefreshToken(tokenHash: String, userId: String, ttlSeconds: Long) {
        saved.add(tokenHash to userId)
    }

    override fun findUserIdByRefreshTokenHash(tokenHash: String): String? = null

    override fun deleteRefreshToken(tokenHash: String) = Unit
}

private class InMemoryAuthUserRepository(
    initialUser: User? = null,
) : AuthUserRepository {
    private val users = linkedMapOf<String, User>()

    init {
        if (initialUser != null) {
            users[initialUser.id.value] = initialUser
        }
    }

    override fun findBySocialAccount(provider: SocialProvider, providerUserId: String): User? =
        users.values.firstOrNull {
            it.socialAccounts.any { account ->
                account.provider == provider && account.providerUserId == providerUserId
            }
        }

    override fun findById(userId: UserId): User? = users[userId.value]

    override fun findByEmail(email: String): User? =
        users.values.firstOrNull { it.email == email }

    override fun save(user: User): User {
        users[user.id.value] = user
        return user
    }

    fun findAll(): List<User> = users.values.toList()
}
