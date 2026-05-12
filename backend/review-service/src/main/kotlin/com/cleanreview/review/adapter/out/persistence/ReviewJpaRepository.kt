package com.cleanreview.review.adapter.out.persistence

import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository

interface ReviewJpaRepository : JpaRepository<ReviewJpaEntity, UUID> {
    fun findAllByTargetIdOrderByCollectedAtDesc(targetId: UUID): List<ReviewJpaEntity>

    fun findAllByOrderByCollectedAtDesc(): List<ReviewJpaEntity>
}
