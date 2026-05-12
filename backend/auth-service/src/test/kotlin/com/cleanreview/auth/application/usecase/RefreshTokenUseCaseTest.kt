package com.cleanreview.auth.application.usecase

import com.cleanreview.auth.application.port.out.AuthUserRepository
import com.cleanreview.auth.application.port.out.IssuedTokens
import com.cleanreview.auth.application.port.out.JwtTokenIssuer
import com.cleanreview.auth.application.port.out.RefreshTokenStore
import com.cleanreview.auth.domain.SocialProvider
import com.cleanreview.auth.domain.User
import com.cleanreview.auth.domain.UserId
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test

class RefreshTokenUseCaseTest {
    @Test
    fun `rotates refresh token and revokes old token`() {
        val user = User.register(
            id = UserId("user-1"),
            email = "owner@example.com",
            displayName = "Owner",
        )
        val userRepository = RefreshInMemoryAuthUserRepository(user)
        val refreshTokenStore = RotatingRefreshTokenStore(
            RefreshTokenHasher.sha256("old-refresh-token") to user.id.value,
        )
        val useCase = RefreshTokenUseCase(
            authUserRepository = userRepository,
            jwtTokenIssuer = RefreshFakeJwtTokenIssuer(),
            refreshTokenStore = refreshTokenStore,
        )

        val result = useCase.execute(RefreshTokenCommand("old-refresh-token"))

        assertEquals("new-access-token", result.accessToken)
        assertEquals("new-refresh-token", result.refreshToken)
        assertEquals(user, result.user)
        assertEquals(listOf(RefreshTokenHasher.sha256("old-refresh-token")), refreshTokenStore.deleted)
        assertEquals(listOf("new-refresh-hash" to "user-1"), refreshTokenStore.saved)
    }
}

private class RefreshFakeJwtTokenIssuer : JwtTokenIssuer {
    override fun issue(user: User): IssuedTokens =
        IssuedTokens(
            accessToken = "new-access-token",
            refreshToken = "new-refresh-token",
            refreshTokenHash = "new-refresh-hash",
            refreshTokenTtlSeconds = 2_592_000,
        )
}

private class RotatingRefreshTokenStore(
    initial: Pair<String, String>,
) : RefreshTokenStore {
    private val tokens = mutableMapOf(initial)
    val deleted = mutableListOf<String>()
    val saved = mutableListOf<Pair<String, String>>()

    override fun saveRefreshToken(tokenHash: String, userId: String, ttlSeconds: Long) {
        tokens[tokenHash] = userId
        saved.add(tokenHash to userId)
    }

    override fun findUserIdByRefreshTokenHash(tokenHash: String): String? = tokens[tokenHash]

    override fun deleteRefreshToken(tokenHash: String) {
        tokens.remove(tokenHash)
        deleted.add(tokenHash)
    }
}

private class RefreshInMemoryAuthUserRepository(
    private val user: User,
) : AuthUserRepository {
    override fun findById(userId: UserId): User? =
        user.takeIf { it.id == userId }

    override fun findBySocialAccount(provider: SocialProvider, providerUserId: String): User? = null

    override fun findByEmail(email: String): User? = null

    override fun save(user: User): User = user
}
