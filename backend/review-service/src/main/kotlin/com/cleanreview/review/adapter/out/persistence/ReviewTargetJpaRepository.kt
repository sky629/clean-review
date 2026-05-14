package com.cleanreview.review.adapter.out.persistence

import com.cleanreview.review.domain.model.ReviewTargetStatus
import com.cleanreview.review.domain.model.ReviewTargetType
import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface ReviewTargetJpaRepository : JpaRepository<ReviewTargetJpaEntity, UUID> {
    fun findAllByCreatedByAndStatusNot(
        createdBy: UUID,
        status: ReviewTargetStatus,
    ): List<ReviewTargetJpaEntity>

    @Query(
        """
        select target
          from ReviewTargetJpaEntity target
         where target.createdBy = :createdBy
           and target.type = :type
           and target.status <> :deletedStatus
           and lower(trim(target.keyword)) = lower(trim(:keyword))
        """,
    )
    fun findActiveByCreatedByAndTypeAndNormalizedKeyword(
        @Param("createdBy") createdBy: UUID,
        @Param("type") type: ReviewTargetType,
        @Param("keyword") keyword: String,
        @Param("deletedStatus") deletedStatus: ReviewTargetStatus,
    ): ReviewTargetJpaEntity?

    fun findAllByStatusNot(status: ReviewTargetStatus): List<ReviewTargetJpaEntity>
}
