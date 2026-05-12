package com.cleanreview.review.adapter.out.persistence

import java.time.Instant
import java.util.UUID
import java.math.BigDecimal
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test

class ReviewPersistenceAdapterTest {
    @Test
    fun `maps review and latest analysis to domain review`() {
        val reviewId = UUID.randomUUID()
        val targetId = UUID.randomUUID()
        val review = ReviewJpaEntity(
            id = reviewId,
            targetId = targetId,
            source = "NAVER_BLOG",
            sourceReviewId = "naver-101",
            canonicalUrl = "https://blog.naver.com/reviews/naver-101",
            title = "성수동 파스타 후기",
            rawText = "웨이팅은 20분이었고 포장 상태가 좋았습니다.",
            publishedAt = Instant.parse("2026-05-06T00:00:00Z"),
            collectedAt = Instant.parse("2026-05-08T00:00:00Z"),
            status = "COLLECTED",
        )
        val analysis = ReviewAnalysisJpaEntity(
            id = UUID.randomUUID(),
            reviewId = reviewId,
            viralScore = BigDecimal("8.0"),
            qualityScore = BigDecimal("88.0"),
            isSuspicious = false,
            usefulForReport = true,
            summary = "웨이팅과 포장 상태가 구체적으로 언급된 후기입니다.",
            detectedPatterns = emptyList(),
            evidence = listOf("웨이팅은 20분"),
            analyzedAt = Instant.parse("2026-05-08T00:01:00Z"),
        )

        val mapped = review.toDomain(analysis)

        assertEquals(reviewId, mapped.id.value)
        assertEquals(targetId, mapped.targetId.value)
        assertEquals(8.0, mapped.viralScore)
        assertEquals(88.0, mapped.qualityScore)
        assertEquals(false, mapped.isSuspicious)
        assertEquals(true, mapped.usefulForReport)
        assertEquals("웨이팅과 포장 상태가 구체적으로 언급된 후기입니다.", mapped.summary)
        assertEquals(listOf("웨이팅은 20분"), mapped.evidence)
    }
}
