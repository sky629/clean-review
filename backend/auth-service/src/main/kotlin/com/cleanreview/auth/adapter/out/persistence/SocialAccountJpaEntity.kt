package com.cleanreview.auth.adapter.out.persistence

import com.cleanreview.auth.domain.SocialProvider
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "social_accounts")
class SocialAccountJpaEntity(
    @Id
    @Column(name = "id", nullable = false)
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: UserJpaEntity = UserJpaEntity(),

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false)
    val provider: SocialProvider = SocialProvider.GOOGLE,

    @Column(name = "provider_user_id", nullable = false)
    val providerUserId: String = "",

    @Column(name = "email", nullable = false)
    val email: String = "",

    @Column(name = "display_name")
    val displayName: String? = null,

    @Column(name = "profile_image_url")
    val profileImageUrl: String? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    val updatedAt: Instant = Instant.now(),
)
