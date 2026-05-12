package com.cleanreview.review.application.usecase

import com.cleanreview.review.domain.model.Review
import com.cleanreview.review.domain.model.ReviewId
import com.cleanreview.review.domain.model.ReviewTarget
import com.cleanreview.review.domain.model.ReviewTargetId
import com.cleanreview.review.domain.model.ReviewTargetType
import com.cleanreview.review.domain.repository.ReviewRepository
import com.cleanreview.review.domain.repository.ReviewTargetRepository
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import org.junit.jupiter.api.Test

class ListMyReviewTargetReviewsUseCaseTest {
    @Test
    fun `lists reviews only when review target belongs to user`() {
        val userId = UUID.randomUUID()
        val target = ReviewTarget.create(
            id = ReviewTargetId(UUID.randomUUID()),
            createdBy = userId,
            keyword = "성수동 파스타 맛집",
            type = ReviewTargetType.PLACE,
        )
        val review = review(target.id)
        val useCase = ListMyReviewTargetReviewsUseCase(
            reviewTargetRepository = ReviewListInMemoryTargetRepository(listOf(target)),
            reviewRepository = ReviewListInMemoryReviewRepository(listOf(review)),
        )

        val result = useCase.execute(target.id, userId)

        assertEquals(listOf(review), result)
    }

    @Test
    fun `rejects review list when review target belongs to another user`() {
        val target = ReviewTarget.create(
            id = ReviewTargetId(UUID.randomUUID()),
            createdBy = UUID.randomUUID(),
            keyword = "성수동 파스타 맛집",
            type = ReviewTargetType.PLACE,
        )
        val useCase = ListMyReviewTargetReviewsUseCase(
            reviewTargetRepository = ReviewListInMemoryTargetRepository(listOf(target)),
            reviewRepository = ReviewListInMemoryReviewRepository(emptyList()),
        )

        assertFailsWith<ReviewTargetAccessDeniedException> {
            useCase.execute(target.id, UUID.randomUUID())
        }
    }
}

private fun review(targetId: ReviewTargetId): Review =
    Review(
        id = ReviewId(UUID.randomUUID()),
        targetId = targetId,
        source = "NAVER_BLOG",
        sourceReviewId = "naver-101",
        canonicalUrl = "https://blog.naver.com/reviews/naver-101",
        title = "성수동 파스타 후기",
        rawText = "웨이팅은 20분이었고 포장 상태가 좋았습니다.",
        summary = "웨이팅과 포장 상태가 구체적으로 언급된 후기입니다.",
        publishedAt = Instant.parse("2026-05-06T00:00:00Z"),
        status = "COLLECTED",
        viralScore = 8.0,
        qualityScore = 88.0,
        isSuspicious = false,
        usefulForReport = true,
        detectedPatterns = emptyList(),
        evidence = listOf("웨이팅은 20분"),
        collectedAt = Instant.parse("2026-05-08T00:00:00Z"),
    )

private class ReviewListInMemoryTargetRepository(
    targets: List<ReviewTarget>,
) : ReviewTargetRepository {
    private val targets = targets.associateBy { it.id }

    override fun save(target: ReviewTarget): ReviewTarget = target

    override fun findById(id: ReviewTargetId): ReviewTarget? = targets[id]

    override fun findAllByCreatedBy(userId: UUID): List<ReviewTarget> =
        targets.values.filter { it.createdBy == userId }

    override fun findAll(): List<ReviewTarget> = targets.values.toList()
}

private class ReviewListInMemoryReviewRepository(
    private val reviews: List<Review>,
) : ReviewRepository {
    override fun findAllByTargetId(targetId: ReviewTargetId): List<Review> =
        reviews.filter { it.targetId == targetId }
}
