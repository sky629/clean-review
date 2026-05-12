package com.cleanreview.review.application.usecase

import com.cleanreview.review.domain.model.ReviewTarget
import com.cleanreview.review.domain.repository.ReviewTargetRepository
import java.util.UUID
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ListMyReviewTargetsUseCase(
    private val reviewTargetRepository: ReviewTargetRepository,
) {
    @Transactional(readOnly = true)
    fun execute(userId: UUID): List<ReviewTarget> = reviewTargetRepository.findAllByCreatedBy(userId)
}
