package com.cleanreview.review.application.usecase

import com.cleanreview.review.domain.model.Review
import com.cleanreview.review.domain.model.ReviewTargetId
import com.cleanreview.review.domain.repository.ReviewRepository
import com.cleanreview.review.domain.repository.ReviewTargetRepository
import java.util.UUID
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ListMyReviewTargetReviewsUseCase(
    private val reviewTargetRepository: ReviewTargetRepository,
    private val reviewRepository: ReviewRepository,
) {
    @Transactional(readOnly = true)
    fun execute(targetId: ReviewTargetId, userId: UUID): List<Review> {
        val target = reviewTargetRepository.findById(targetId) ?: throw ReviewTargetNotFoundException()

        if (!target.isOwnedBy(userId)) {
            throw ReviewTargetAccessDeniedException()
        }

        return reviewRepository.findAllByTargetId(targetId)
    }
}
