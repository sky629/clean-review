package com.cleanreview.review.adapter.out.persistence

import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository

interface CollectionRunJpaRepository : JpaRepository<CollectionRunJpaEntity, UUID> {
    fun findAllByOrderByRequestedAtDesc(): List<CollectionRunJpaEntity>

    fun findFirstByTargetIdAndSourceAndStatusOrderByCompletedAtDesc(
        targetId: UUID,
        source: String,
        status: com.cleanreview.review.domain.model.CollectionRunStatus,
    ): CollectionRunJpaEntity?

    fun findFirstByTargetIdAndSourceAndStatusInOrderByRequestedAtDesc(
        targetId: UUID,
        source: String,
        statuses: Collection<com.cleanreview.review.domain.model.CollectionRunStatus>,
    ): CollectionRunJpaEntity?
}
