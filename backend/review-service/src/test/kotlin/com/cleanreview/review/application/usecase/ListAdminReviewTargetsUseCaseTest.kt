package com.cleanreview.review.application.usecase

import com.cleanreview.review.domain.model.ReviewTarget
import com.cleanreview.review.domain.model.ReviewTargetId
import com.cleanreview.review.domain.model.ReviewTargetType
import com.cleanreview.review.domain.repository.ReviewTargetRepository
import java.util.UUID
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test

class ListAdminReviewTargetsUseCaseTest {
    @Test
    fun `admin lists every non-deleted review target`() {
        val repository = AdminTargetInMemoryReviewTargetRepository()
        val first = repository.save(target("성수동 파스타 후기"))
        val second = repository.save(target("무선 청소기 후기"))

        val result = ListAdminReviewTargetsUseCase(repository).execute()

        assertEquals(listOf(first, second), result)
    }

    private fun target(keyword: String): ReviewTarget =
        ReviewTarget.create(
            id = ReviewTargetId(UUID.randomUUID()),
            createdBy = UUID.randomUUID(),
            keyword = keyword,
            type = ReviewTargetType.PRODUCT,
        )
}

private class AdminTargetInMemoryReviewTargetRepository : ReviewTargetRepository {
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
