package com.cleanreview.auth.adapter.out.persistence

import com.cleanreview.auth.domain.SocialAccount
import com.cleanreview.auth.domain.SocialProvider
import com.cleanreview.auth.domain.User
import com.cleanreview.auth.domain.UserId
import com.cleanreview.auth.domain.UserStatus
import com.cleanreview.common.security.UserRole
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "users")
class UserJpaEntity(
    @Id
    @Column(name = "id", nullable = false)
    val id: UUID = UUID.randomUUID(),

    @Column(name = "email", nullable = false)
    val email: String = "",

    @Column(name = "name", nullable = false)
    val name: String = "",

    @Column(name = "profile_image_url")
    val profileImageUrl: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    val role: UserRole = UserRole.USER,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    val status: UserStatus = UserStatus.ACTIVE,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    val updatedAt: Instant = Instant.now(),

    @OneToMany(mappedBy = "user", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.EAGER)
    val socialAccounts: MutableSet<SocialAccountJpaEntity> = linkedSetOf(),
) {
    fun toDomain(): User =
        User(
            id = UserId(id.toString()),
            email = email,
            displayName = name,
            profileImageUrl = profileImageUrl,
            role = role,
            status = status,
            socialAccounts = socialAccounts.map {
                SocialAccount(
                    userId = UserId(id.toString()),
                    provider = it.provider,
                    providerUserId = it.providerUserId,
                )
            }.toSet(),
        )

    companion object {
        fun from(user: User): UserJpaEntity {
            val entity = UserJpaEntity(
                id = UUID.fromString(user.id.value),
                email = user.email,
                name = user.displayName,
                profileImageUrl = user.profileImageUrl,
                role = user.role,
                status = user.status,
            )
            user.socialAccounts.forEach { account ->
                entity.socialAccounts.add(
                    SocialAccountJpaEntity(
                        user = entity,
                        provider = account.provider,
                        providerUserId = account.providerUserId,
                        email = user.email,
                        displayName = user.displayName,
                        profileImageUrl = user.profileImageUrl,
                    ),
                )
            }
            return entity
        }
    }
}
