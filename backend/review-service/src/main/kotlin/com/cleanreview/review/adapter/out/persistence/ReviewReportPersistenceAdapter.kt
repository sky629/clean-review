package com.cleanreview.review.adapter.out.persistence

import com.cleanreview.review.domain.model.ReviewReport
import com.cleanreview.review.domain.model.ReviewTargetId
import com.cleanreview.review.domain.repository.ReviewReportRepository
import org.springframework.stereotype.Repository

@Repository
class ReviewReportPersistenceAdapter(
    private val reviewReportJpaRepository: ReviewReportJpaRepository,
) : ReviewReportRepository {
    override fun save(report: ReviewReport): ReviewReport =
        reviewReportJpaRepository.save(ReviewReportJpaEntity.from(report)).toDomain()

    override fun findLatestByTargetId(targetId: ReviewTargetId): ReviewReport? =
        reviewReportJpaRepository.findFirstByTargetIdOrderByCreatedAtDesc(targetId.value)?.toDomain()
}
