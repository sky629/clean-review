package com.cleanreview.review.application.usecase

import com.cleanreview.review.domain.model.ReviewTarget
import com.cleanreview.review.domain.repository.ReviewTargetRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ListAdminReviewTargetsUseCase(
    private val reviewTargetRepository: ReviewTargetRepository,
) {
    @Transactional(readOnly = true)
    fun execute(): List<ReviewTarget> = reviewTargetRepository.findAll()
}
