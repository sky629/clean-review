package com.cleanreview.review.application.usecase

import com.cleanreview.review.domain.model.ReviewReport
import com.cleanreview.review.domain.model.ReviewTargetId
import com.cleanreview.review.domain.repository.ReviewReportRepository
import com.cleanreview.review.domain.repository.ReviewTargetRepository
import java.util.UUID
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

class ReviewReportNotFoundException : RuntimeException("Review report not found.")

@Service
class GetMyLatestReviewReportUseCase(
    private val reviewTargetRepository: ReviewTargetRepository,
    private val reviewReportRepository: ReviewReportRepository,
) {
    @Transactional(readOnly = true)
    fun execute(targetId: ReviewTargetId, userId: UUID): ReviewReport {
        val target = reviewTargetRepository.findById(targetId) ?: throw ReviewTargetNotFoundException()

        if (!target.isOwnedBy(userId)) {
            throw ReviewTargetAccessDeniedException()
        }

        return reviewReportRepository.findLatestByTargetId(targetId) ?: throw ReviewReportNotFoundException()
    }
}
