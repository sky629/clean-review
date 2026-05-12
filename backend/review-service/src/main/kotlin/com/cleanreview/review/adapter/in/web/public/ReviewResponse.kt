package com.cleanreview.review.adapter.`in`.web.public

import com.cleanreview.review.domain.model.Review

data class ReviewResponse(
    val id: String,
    val targetId: String,
    val source: String,
    val sourceReviewId: String?,
    val canonicalUrl: String,
    val title: String?,
    val rawText: String,
    val summary: String?,
    val publishedAt: String?,
    val status: String,
    val viralScore: Double?,
    val qualityScore: Double?,
    val isSuspicious: Boolean?,
    val usefulForReport: Boolean?,
    val detectedPatterns: List<String>,
    val evidence: List<String>,
) {
    companion object {
        fun from(review: Review): ReviewResponse =
            ReviewResponse(
                id = review.id.value.toString(),
                targetId = review.targetId.value.toString(),
                source = review.source,
                sourceReviewId = review.sourceReviewId,
                canonicalUrl = review.canonicalUrl,
                title = review.title,
                rawText = review.rawText,
                summary = review.summary,
                publishedAt = review.publishedAt?.toString(),
                status = review.status,
                viralScore = review.viralScore,
                qualityScore = review.qualityScore,
                isSuspicious = review.isSuspicious,
                usefulForReport = review.usefulForReport,
                detectedPatterns = review.detectedPatterns,
                evidence = review.evidence,
            )
    }
}
