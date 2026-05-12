package com.cleanreview.review.application.usecase

import com.cleanreview.review.domain.model.ReviewTarget
import com.cleanreview.review.domain.model.ReviewTargetId
import com.cleanreview.review.domain.model.ReviewTargetType
import com.cleanreview.review.domain.repository.ReviewTargetRepository
import java.util.UUID
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UpdateReviewTargetUseCase(
    private val reviewTargetRepository: ReviewTargetRepository,
) {
    @Transactional
    fun execute(
        id: ReviewTargetId,
        userId: UUID,
        keyword: String,
        type: ReviewTargetType,
    ): ReviewTarget {
        val target = reviewTargetRepository.findById(id)
            ?: throw ReviewTargetNotFoundException()
        if (!target.isOwnedBy(userId)) {
            throw ReviewTargetAccessDeniedException()
        }

        return reviewTargetRepository.save(
            target.updateDetails(
                keyword = keyword,
                type = type,
            ),
        )
    }
}
