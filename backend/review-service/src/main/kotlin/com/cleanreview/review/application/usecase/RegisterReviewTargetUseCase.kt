package com.cleanreview.review.application.usecase

import com.cleanreview.review.application.command.RegisterReviewTargetCommand
import com.cleanreview.review.domain.model.ReviewTarget
import com.cleanreview.review.domain.model.ReviewTargetId
import com.cleanreview.review.domain.repository.ReviewTargetRepository
import java.util.UUID
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class RegisterReviewTargetUseCase(
    private val reviewTargetRepository: ReviewTargetRepository,
    private val requestReviewCollectionUseCase: RequestReviewCollectionUseCase,
) {
    @Transactional
    fun execute(command: RegisterReviewTargetCommand): ReviewTarget {
        reviewTargetRepository.findActiveByCreatedByAndTypeAndKeyword(
            userId = command.userId,
            type = command.type,
            keyword = command.keyword,
        )?.let { return it }

        val target = reviewTargetRepository.save(
            ReviewTarget.create(
                id = ReviewTargetId(UUID.randomUUID()),
                createdBy = command.userId,
                keyword = command.keyword,
                type = command.type,
            ),
        )

        requestReviewCollectionUseCase.requestInitialBackfill(target, command.userId)

        return target
    }
}
