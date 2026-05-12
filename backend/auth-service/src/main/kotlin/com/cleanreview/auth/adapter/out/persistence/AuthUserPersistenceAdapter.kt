package com.cleanreview.auth.adapter.out.persistence

import com.cleanreview.auth.application.port.out.AuthUserRepository
import com.cleanreview.auth.domain.SocialProvider
import com.cleanreview.auth.domain.User
import com.cleanreview.auth.domain.UserId
import java.util.UUID
import org.springframework.stereotype.Repository

@Repository
class AuthUserPersistenceAdapter(
    private val userJpaRepository: UserJpaRepository,
    private val socialAccountJpaRepository: SocialAccountJpaRepository,
) : AuthUserRepository {
    override fun findById(userId: UserId): User? =
        userJpaRepository.findById(UUID.fromString(userId.value)).orElse(null)?.toDomain()

    override fun findBySocialAccount(provider: SocialProvider, providerUserId: String): User? =
        socialAccountJpaRepository.findByProviderAndProviderUserId(provider, providerUserId)
            ?.user
            ?.toDomain()

    override fun findByEmail(email: String): User? =
        userJpaRepository.findByEmail(email)?.toDomain()

    override fun save(user: User): User =
        userJpaRepository.save(UserJpaEntity.from(user)).toDomain()
}
