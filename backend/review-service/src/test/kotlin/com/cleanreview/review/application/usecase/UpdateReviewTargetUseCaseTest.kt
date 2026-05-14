package com.cleanreview.review.application.usecase

import com.cleanreview.review.domain.model.ReviewTarget
import com.cleanreview.review.domain.model.ReviewTargetId
import com.cleanreview.review.domain.model.ReviewTargetType
import com.cleanreview.review.domain.repository.ReviewTargetRepository
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import org.junit.jupiter.api.Test

class UpdateReviewTargetUseCaseTest {
    @Test
    fun `owner can update own review target`() {
        val ownerId = UUID.randomUUID()
        val repository = UpdateInMemoryReviewTargetRepository()
        val target = repository.save(target(ownerId))
        val useCase = UpdateReviewTargetUseCase(repository)

        val updated = useCase.execute(
            id = target.id,
            userId = ownerId,
            keyword = "무선 청소기 실사용 후기",
            type = ReviewTargetType.PRODUCT,
        )

        assertEquals("무선 청소기 실사용 후기", updated.keyword)
        assertEquals(ReviewTargetType.PRODUCT, updated.type)
    }

    @Test
    fun `stranger cannot update another user's review target`() {
        val repository = UpdateInMemoryReviewTargetRepository()
        val target = repository.save(target(UUID.randomUUID()))

        assertFailsWith<ReviewTargetAccessDeniedException> {
            UpdateReviewTargetUseCase(repository).execute(
                id = target.id,
                userId = UUID.randomUUID(),
                keyword = "다른 키워드",
                type = ReviewTargetType.PLACE,
            )
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

private class UpdateInMemoryReviewTargetRepository : ReviewTargetRepository {
    private val targets = linkedMapOf<ReviewTargetId, ReviewTarget>()

    override fun save(target: ReviewTarget): ReviewTarget {
        targets[target.id] = target
        return target
    }

    override fun findById(id: ReviewTargetId): ReviewTarget? = targets[id]

    override fun findAllByCreatedBy(userId: UUID): List<ReviewTarget> =
        targets.values.filter { it.createdBy == userId && !it.isDeleted() }

    override fun findActiveByCreatedByAndTypeAndKeyword(
        userId: UUID,
        type: com.cleanreview.review.domain.model.ReviewTargetType,
        keyword: String,
    ): ReviewTarget? =
        targets.values.firstOrNull {
            it.createdBy == userId &&
                it.type == type &&
                it.keyword.trim().equals(keyword.trim(), ignoreCase = true) &&
                !it.isDeleted()
        }

    override fun findAll(): List<ReviewTarget> =
        targets.values.filter { !it.isDeleted() }
}
