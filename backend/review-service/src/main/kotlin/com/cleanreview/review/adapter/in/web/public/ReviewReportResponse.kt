package com.cleanreview.review.adapter.`in`.web.public

import com.cleanreview.review.domain.model.ReviewReport

data class ReviewReportResponse(
    val id: String,
    val targetId: String,
    val collectionRunId: String,
    val viralContaminationScore: Double,
    val trustScore: Double,
    val summary: String,
    val pros: List<String>,
    val cons: List<String>,
    val evidenceReviewIds: List<String>,
    val modelName: String,
    val modelVersion: String,
) {
    companion object {
        fun from(report: ReviewReport): ReviewReportResponse =
            ReviewReportResponse(
                id = report.id.value.toString(),
                targetId = report.targetId.value.toString(),
                collectionRunId = report.collectionRunId.value.toString(),
                viralContaminationScore = report.viralContaminationScore,
                trustScore = report.trustScore,
                summary = report.summary,
                pros = report.pros,
                cons = report.cons,
                evidenceReviewIds = report.evidenceReviewIds.map { it.toString() },
                modelName = report.modelName,
                modelVersion = report.modelVersion,
            )
    }
}
