package com.cleanreview.auth.adapter.out.jwt

import com.cleanreview.auth.domain.SocialProvider
import com.cleanreview.auth.domain.User
import com.cleanreview.auth.domain.UserId
import java.util.Base64
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test

class HmacJwtTokenIssuerTest {
    @Test
    fun `issues signed access token with user claims and opaque refresh token`() {
        val issuer = HmacJwtTokenIssuer(
            secret = "01234567890123456789012345678901",
            issuer = "clean-review",
        )
        val user = User.register(
            id = UserId("user-1"),
            email = "owner@example.com",
            displayName = "Owner",
        ).linkSocialAccount(SocialProvider.GOOGLE, "google-123")

        val tokens = issuer.issue(user)
        val parts = tokens.accessToken.split(".")

        assertEquals(3, parts.size)
        assertTrue(tokens.refreshToken.isNotBlank())
        assertTrue(tokens.refreshTokenHash.isNotBlank())
        assertEquals(2_592_000, tokens.refreshTokenTtlSeconds)

        val payload = String(Base64.getUrlDecoder().decode(parts[1]))
        assertTrue(payload.contains("\"sub\":\"user-1\""))
        assertTrue(payload.contains("\"email\":\"owner@example.com\""))
        assertTrue(payload.contains("\"roles\":[\"USER\"]"))
    }
}
