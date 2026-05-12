package com.cleanreview.review.adapter.out.persistence

import com.cleanreview.review.domain.model.Review
import com.cleanreview.review.domain.model.ReviewId
import com.cleanreview.review.domain.model.ReviewTargetId
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "reviews")
class ReviewJpaEntity(
    @Id
    @Column(name = "id", nullable = false)
    val id: UUID = UUID.randomUUID(),

    @Column(name = "target_id", nullable = false)
    val targetId: UUID = UUID.randomUUID(),

    @Column(name = "collection_run_id")
    val collectionRunId: UUID? = null,

    @Column(name = "source", nullable = false)
    val source: String = "",

    @Column(name = "source_review_id")
    val sourceReviewId: String? = null,

    @Column(name = "canonical_url", nullable = false)
    val canonicalUrl: String = "",

    @Column(name = "title")
    val title: String? = null,

    @Column(name = "raw_text", nullable = false)
    val rawText: String = "",

    @Column(name = "published_at")
    val publishedAt: Instant? = null,

    @Column(name = "collected_at", nullable = false)
    val collectedAt: Instant = Instant.now(),

    @Column(name = "status", nullable = false)
    val status: String = "",
) {
    fun toDomain(analysis: ReviewAnalysisJpaEntity?): Review =
        Review(
            id = ReviewId(id),
            targetId = ReviewTargetId(targetId),
            source = source,
            sourceReviewId = sourceReviewId,
            canonicalUrl = canonicalUrl,
            title = title,
            rawText = rawText,
            summary = analysis?.summary?.takeIf { it.isNotBlank() },
            publishedAt = publishedAt,
            status = status,
            viralScore = analysis?.viralScore?.toDouble(),
            qualityScore = analysis?.qualityScore?.toDouble(),
            isSuspicious = analysis?.isSuspicious,
            usefulForReport = analysis?.usefulForReport,
            detectedPatterns = analysis?.detectedPatterns ?: emptyList(),
            evidence = analysis?.evidence ?: emptyList(),
            collectedAt = collectedAt,
        )
}
