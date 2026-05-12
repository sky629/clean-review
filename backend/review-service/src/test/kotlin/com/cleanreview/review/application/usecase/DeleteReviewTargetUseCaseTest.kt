package com.cleanreview.review.application.usecase

import com.cleanreview.review.domain.model.ReviewTarget
import com.cleanreview.review.domain.model.ReviewTargetId
import com.cleanreview.review.domain.model.ReviewTargetStatus
import com.cleanreview.review.domain.model.ReviewTargetType
import com.cleanreview.review.domain.repository.ReviewTargetRepository
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import org.junit.jupiter.api.Test

class DeleteReviewTargetUseCaseTest {
    @Test
    fun `owner can delete own review target`() {
        val ownerId = UUID.randomUUID()
        val repository = DeleteInMemoryReviewTargetRepository()
        val target = repository.save(target(ownerId))

        DeleteReviewTargetUseCase(repository).execute(target.id, ownerId)

        assertEquals(ReviewTargetStatus.DELETED, repository.findById(target.id)?.status)
    }

    @Test
    fun `stranger cannot delete another user's review target`() {
        val repository = DeleteInMemoryReviewTargetRepository()
        val target = repository.save(target(UUID.randomUUID()))

        assertFailsWith<ReviewTargetAccessDeniedException> {
            DeleteReviewTargetUseCase(repository).execute(target.id, UUID.randomUUID())
        }
    }

    private fun target(ownerId: UUID): ReviewTarget =
        ReviewTarget.create(
            id = ReviewTargetId(UUID.randomUUID()),
            createdBy = ownerId,
            keyword = "성수동 파스타 맛집",
            type = ReviewTargetType.PLACE,
        )
}

private class DeleteInMemoryReviewTargetRepository : ReviewTargetRepository {
    private val targets = linkedMapOf<ReviewTargetId, ReviewTarget>()

    override fun save(target: ReviewTarget): ReviewTarget {
        targets[target.id] = target
        return target
    }

    override fun findById(id: ReviewTargetId): ReviewTarget? = targets[id]

    override fun findAllByCreatedBy(userId: UUID): List<ReviewTarget> =
        targets.values.filter { it.createdBy == userId && !it.isDeleted() }

    override fun findAll(): List<ReviewTarget> =
        targets.values.filter { !it.isDeleted() }
}
