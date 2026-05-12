package com.cleanreview.auth.adapter.out.persistence

import com.cleanreview.auth.domain.SocialProvider
import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository

interface SocialAccountJpaRepository : JpaRepository<SocialAccountJpaEntity, UUID> {
    fun findByProviderAndProviderUserId(
        provider: SocialProvider,
        providerUserId: String,
    ): SocialAccountJpaEntity?
}
