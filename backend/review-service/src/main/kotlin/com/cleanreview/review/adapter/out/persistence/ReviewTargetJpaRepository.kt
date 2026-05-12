package com.cleanreview.review.adapter.out.persistence

import com.cleanreview.review.domain.model.ReviewTargetStatus
import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository

interface ReviewTargetJpaRepository : JpaRepository<ReviewTargetJpaEntity, UUID> {
    fun findAllByCreatedByAndStatusNot(
        createdBy: UUID,
        status: ReviewTargetStatus,
    ): List<ReviewTargetJpaEntity>

    fun findAllByStatusNot(status: ReviewTargetStatus): List<ReviewTargetJpaEntity>
}
