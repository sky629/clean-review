package com.cleanreview.review.domain.model

import java.time.Instant

data class Review(
    val id: ReviewId,
    val targetId: ReviewTargetId,
    val source: String,
    val sourceReviewId: String?,
    val canonicalUrl: String,
    val title: String?,
    val rawText: String,
    val summary: String?,
    val publishedAt: Instant?,
    val status: String,
    val viralScore: Double?,
    val qualityScore: Double?,
    val isSuspicious: Boolean?,
    val usefulForReport: Boolean?,
    val detectedPatterns: List<String>,
    val evidence: List<String>,
    val collectedAt: Instant,
)
