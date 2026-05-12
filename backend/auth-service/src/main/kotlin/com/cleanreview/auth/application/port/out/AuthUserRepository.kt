package com.cleanreview.auth.application.port.out

import com.cleanreview.auth.domain.SocialProvider
import com.cleanreview.auth.domain.User
import com.cleanreview.auth.domain.UserId

interface AuthUserRepository {
    fun findById(userId: UserId): User?

    fun findBySocialAccount(provider: SocialProvider, providerUserId: String): User?

    fun findByEmail(email: String): User?

    fun save(user: User): User
}
