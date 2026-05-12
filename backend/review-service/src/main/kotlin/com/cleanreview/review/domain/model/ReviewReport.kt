package com.cleanreview.review.domain.model

import java.time.Instant
import java.util.UUID

data class ReviewReport(
    val id: ReviewReportId,
    val targetId: ReviewTargetId,
    val collectionRunId: CollectionRunId,
    val analyzerVersion: String,
    val modelProvider: String,
    val modelName: String,
    val modelVersion: String,
    val viralContaminationScore: Double,
    val trustScore: Double,
    val summary: String,
    val pros: List<String>,
    val cons: List<String>,
    val evidenceReviewIds: List<UUID>,
    val reportHash: String,
    val createdAt: Instant,
) {
    companion object {
        fun create(
            id: ReviewReportId,
            targetId: ReviewTargetId,
            collectionRunId: CollectionRunId,
            analyzerVersion: String,
            modelProvider: String,
            modelName: String,
            modelVersion: String,
            viralContaminationScore: Double,
            trustScore: Double,
            summary: String,
            pros: List<String>,
            cons: List<String>,
            evidenceReviewIds: List<UUID>,
            reportHash: String,
            createdAt: Instant = Instant.now(),
        ): ReviewReport {
            require(viralContaminationScore in 0.0..100.0) {
                "Viral contamination score must be between 0 and 100."
            }
            require(trustScore in 0.0..100.0) { "Trust score must be between 0 and 100." }
            require(summary.isNotBlank()) { "Report summary must not be blank." }

            return ReviewReport(
                id = id,
                targetId = targetId,
                collectionRunId = collectionRunId,
                analyzerVersion = analyzerVersion,
                modelProvider = modelProvider,
                modelName = modelName,
                modelVersion = modelVersion,
                viralContaminationScore = viralContaminationScore,
                trustScore = trustScore,
                summary = summary.trim(),
                pros = pros,
                cons = cons,
                evidenceReviewIds = evidenceReviewIds,
                reportHash = reportHash,
                createdAt = createdAt,
            )
        }
    }
}
