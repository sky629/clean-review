package com.cleanreview.review.application.usecase

import com.cleanreview.review.domain.model.ReviewTarget
import com.cleanreview.review.domain.model.ReviewTargetId
import com.cleanreview.review.domain.model.ReviewTargetType
import com.cleanreview.review.domain.repository.ReviewTargetRepository
import java.util.UUID
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test

class ListMyReviewTargetsUseCaseTest {
    @Test
    fun `lists only targets owned by requesting user`() {
        val ownerId = UUID.randomUUID()
        val otherId = UUID.randomUUID()
        val repository = ListInMemoryReviewTargetRepository()
        val owned = repository.save(target(ownerId, "성수동 파스타 후기"))
        repository.save(target(otherId, "다른 상품 후기"))

        val result = ListMyReviewTargetsUseCase(repository).execute(ownerId)

        assertEquals(listOf(owned), result)
    }

    private fun target(ownerId: UUID, keyword: String): ReviewTarget =
        ReviewTarget.create(
            id = ReviewTargetId(UUID.randomUUID()),
            createdBy = ownerId,
            keyword = keyword,
            type = ReviewTargetType.PRODUCT,
        )
}

private class ListInMemoryReviewTargetRepository : ReviewTargetRepository {
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
