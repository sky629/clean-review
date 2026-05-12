package com.cleanreview.review.adapter.out.persistence

import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository

interface ReviewAnalysisJpaRepository : JpaRepository<ReviewAnalysisJpaEntity, UUID> {
    fun findFirstByReviewIdOrderByAnalyzedAtDesc(reviewId: UUID): ReviewAnalysisJpaEntity?

    fun findAllByOrderByAnalyzedAtDesc(): List<ReviewAnalysisJpaEntity>
}
