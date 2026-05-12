package com.cleanreview.auth.domain

import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test

class UserTest {
    @Test
    fun `links a social account to an active user`() {
        val user = User.register(
            id = UserId("user-1"),
            email = "owner@example.com",
            displayName = "Owner",
        )

        val linked = user.linkSocialAccount(
            provider = SocialProvider.GOOGLE,
            providerUserId = "google-123",
        )

        assertEquals(UserStatus.ACTIVE, linked.status)
        assertTrue(linked.socialAccounts.any { it.provider == SocialProvider.GOOGLE })
        assertEquals("user-1", linked.socialAccounts.single().userId.value)
    }
}
