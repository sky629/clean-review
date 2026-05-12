package com.cleanreview.review.application.usecase

import com.cleanreview.review.domain.model.ReviewTargetId
import com.cleanreview.review.domain.repository.ReviewTargetRepository
import java.util.UUID
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

class ReviewTargetNotFoundException : RuntimeException("Review target not found.")

class ReviewTargetAccessDeniedException : RuntimeException("Review target access denied.")

@Service
class DeleteReviewTargetUseCase(
    private val reviewTargetRepository: ReviewTargetRepository,
) {
    @Transactional
    fun execute(id: ReviewTargetId, userId: UUID) {
        val target = reviewTargetRepository.findById(id) ?: throw ReviewTargetNotFoundException()

        if (!target.isOwnedBy(userId)) {
            throw ReviewTargetAccessDeniedException()
        }

        reviewTargetRepository.save(target.delete())
    }
}
