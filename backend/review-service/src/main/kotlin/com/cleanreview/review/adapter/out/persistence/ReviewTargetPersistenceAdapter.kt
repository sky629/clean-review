package com.cleanreview.review.adapter.out.persistence

import com.cleanreview.review.domain.model.ReviewTarget
import com.cleanreview.review.domain.model.ReviewTargetId
import com.cleanreview.review.domain.model.ReviewTargetStatus
import com.cleanreview.review.domain.model.ReviewTargetType
import com.cleanreview.review.domain.repository.ReviewTargetRepository
import java.util.UUID
import org.springframework.stereotype.Repository

@Repository
class ReviewTargetPersistenceAdapter(
    private val reviewTargetJpaRepository: ReviewTargetJpaRepository,
) : ReviewTargetRepository {
    override fun save(target: ReviewTarget): ReviewTarget =
        reviewTargetJpaRepository.save(ReviewTargetJpaEntity.from(target)).toDomain()

    override fun findById(id: ReviewTargetId): ReviewTarget? =
        reviewTargetJpaRepository.findById(id.value).map { it.toDomain() }.orElse(null)

    override fun findAllByCreatedBy(userId: UUID): List<ReviewTarget> =
        reviewTargetJpaRepository
            .findAllByCreatedByAndStatusNot(userId, ReviewTargetStatus.DELETED)
            .map { it.toDomain() }

    override fun findActiveByCreatedByAndTypeAndKeyword(
        userId: UUID,
        type: ReviewTargetType,
        keyword: String,
    ): ReviewTarget? =
        reviewTargetJpaRepository
            .findActiveByCreatedByAndTypeAndNormalizedKeyword(
                userId,
                type,
                keyword,
                ReviewTargetStatus.DELETED,
            )
            ?.toDomain()

    override fun findAll(): List<ReviewTarget> =
        reviewTargetJpaRepository.findAllByStatusNot(ReviewTargetStatus.DELETED).map { it.toDomain() }
}
