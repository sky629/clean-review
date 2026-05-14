package com.cleanreview.review.adapter.out.persistence

import com.cleanreview.review.domain.model.Review
import com.cleanreview.review.domain.model.ReviewTargetId
import com.cleanreview.review.domain.repository.ReviewRepository
import org.springframework.stereotype.Repository

@Repository
class ReviewPersistenceAdapter(
    private val reviewJpaRepository: ReviewJpaRepository,
    private val reviewAnalysisJpaRepository: ReviewAnalysisJpaRepository,
) : ReviewRepository {
    override fun findAllByTargetId(targetId: ReviewTargetId): List<Review> =
        reviewJpaRepository.findAllByTargetIdOrderByCollectedAtDesc(targetId.value)
            .let { reviews ->
                if (reviews.isEmpty()) {
                    return@let emptyList()
                }
                val latestAnalysisByReviewId = reviewAnalysisJpaRepository
                    .findAllByReviewIdIn(reviews.map { it.id })
                    .groupBy { it.reviewId }
                    .mapValues { (_, analyses) -> analyses.maxBy { it.analyzedAt } }

                reviews.map { review -> review.toDomain(latestAnalysisByReviewId[review.id]) }
            }
  }
