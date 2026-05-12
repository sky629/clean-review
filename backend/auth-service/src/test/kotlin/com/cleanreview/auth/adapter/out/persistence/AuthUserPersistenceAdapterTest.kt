package com.cleanreview.auth.adapter.out.persistence

import com.cleanreview.auth.domain.SocialProvider
import com.cleanreview.auth.domain.User
import com.cleanreview.auth.domain.UserId
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test

class AuthUserPersistenceAdapterTest {
    @Test
    fun `maps user domain to jpa entity and back`() {
        val user = User.register(
            id = UserId("11111111-1111-1111-1111-111111111111"),
            email = "owner@example.com",
            displayName = "Owner",
        ).linkSocialAccount(SocialProvider.GOOGLE, "google-123")

        val mapped = UserJpaEntity.from(user).toDomain()

        assertEquals(user.id, mapped.id)
        assertEquals(user.email, mapped.email)
        assertEquals(user.displayName, mapped.displayName)
        assertEquals(user.socialAccounts, mapped.socialAccounts)
    }
}
