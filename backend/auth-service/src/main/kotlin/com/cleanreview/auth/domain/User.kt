package com.cleanreview.auth.domain

import com.cleanreview.common.security.UserRole

data class User(
    val id: UserId,
    val email: String,
    val displayName: String,
    val profileImageUrl: String?,
    val role: UserRole,
    val status: UserStatus,
    val socialAccounts: Set<SocialAccount>,
) {
    fun linkSocialAccount(provider: SocialProvider, providerUserId: String): User {
        val account = SocialAccount(id, provider, providerUserId)
        return copy(socialAccounts = socialAccounts + account)
    }

    companion object {
        fun register(id: UserId, email: String, displayName: String): User {
            require(email.isNotBlank()) { "Email must not be blank." }
            require(displayName.isNotBlank()) { "Display name must not be blank." }
            return User(
                id = id,
                email = email,
                displayName = displayName,
                profileImageUrl = null,
                role = UserRole.USER,
                status = UserStatus.ACTIVE,
                socialAccounts = emptySet(),
            )
        }
    }
}
