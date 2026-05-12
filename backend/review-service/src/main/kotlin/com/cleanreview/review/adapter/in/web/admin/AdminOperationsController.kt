package com.cleanreview.review.adapter.`in`.web.admin

import com.cleanreview.common.security.AdminOnly
import com.cleanreview.review.adapter.out.persistence.DeadLetterEventJpaEntity
import com.cleanreview.review.adapter.out.persistence.DeadLetterEventJpaRepository
import com.cleanreview.review.adapter.out.persistence.RetryJobJpaEntity
import com.cleanreview.review.adapter.out.persistence.RetryJobJpaRepository
import com.cleanreview.review.adapter.out.persistence.ReviewAnalysisJpaEntity
import com.cleanreview.review.adapter.out.persistence.ReviewAnalysisJpaRepository
import com.cleanreview.review.adapter.out.persistence.ReviewJpaEntity
import com.cleanreview.review.adapter.out.persistence.ReviewJpaRepository
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

data class AdminReviewResponse(
    val id: String,
    val targetId: String,
    val collectionRunId: String?,
    val source: String,
    val sourceReviewId: String?,
    val status: String,
    val viralScore: Double?,
    val qualityScore: Double?,
    val usefulForReport: Boolean?,
    val collectedAt: String,
    val rawText: String,
) {
    companion object {
        fun from(
            review: ReviewJpaEntity,
            analysis: ReviewAnalysisJpaEntity?,
        ): AdminReviewResponse =
            AdminReviewResponse(
                id = review.id.toString(),
                targetId = review.targetId.toString(),
                collectionRunId = review.collectionRunId?.toString(),
                source = review.source,
                sourceReviewId = review.sourceReviewId,
                status = review.status,
                viralScore = analysis?.viralScore?.toDouble(),
                qualityScore = analysis?.qualityScore?.toDouble(),
                usefulForReport = analysis?.usefulForReport,
                collectedAt = review.collectedAt.toString(),
                rawText = review.rawText,
            )
    }
}

data class AdminAnalysisRunResponse(
    val id: String,
    val reviewId: String,
    val collectionRunId: String?,
    val analyzerVersion: String,
    val modelProvider: String,
    val modelName: String,
    val status: String,
    val viralScore: Double,
    val qualityScore: Double,
    val usefulForReport: Boolean,
    val analyzedAt: String,
) {
    companion object {
        fun from(analysis: ReviewAnalysisJpaEntity): AdminAnalysisRunResponse =
            AdminAnalysisRunResponse(
                id = analysis.id.toString(),
                reviewId = analysis.reviewId.toString(),
                collectionRunId = analysis.collectionRunId?.toString(),
                analyzerVersion = analysis.analyzerVersion,
                modelProvider = analysis.modelProvider,
                modelName = analysis.modelName,
                status = analysis.status,
                viralScore = analysis.viralScore.toDouble(),
                qualityScore = analysis.qualityScore.toDouble(),
                usefulForReport = analysis.usefulForReport,
                analyzedAt = analysis.analyzedAt.toString(),
            )
    }
}

data class AdminRetryJobResponse(
    val id: String,
    val topic: String,
    val eventType: String,
    val originalEventId: String,
    val attempt: Int,
    val maxAttempts: Int,
    val nextAttemptAt: String,
    val lastErrorCode: String?,
    val lastErrorMessage: String?,
    val status: String,
    val createdAt: String,
) {
    companion object {
        fun from(job: RetryJobJpaEntity): AdminRetryJobResponse =
            AdminRetryJobResponse(
                id = job.id.toString(),
                topic = job.topic,
                eventType = job.eventType,
                originalEventId = job.originalEventId,
                attempt = job.attempt,
                maxAttempts = job.maxAttempts,
                nextAttemptAt = job.nextAttemptAt.toString(),
                lastErrorCode = job.lastErrorCode,
                lastErrorMessage = job.lastErrorMessage,
                status = job.status,
                createdAt = job.createdAt.toString(),
            )
    }
}

data class AdminDeadLetterEventResponse(
    val id: String,
    val sourceTopic: String,
    val eventId: String,
    val eventType: String,
    val consumerName: String,
    val errorCode: String?,
    val errorMessage: String?,
    val failedAt: String,
) {
    companion object {
        fun from(event: DeadLetterEventJpaEntity): AdminDeadLetterEventResponse =
            AdminDeadLetterEventResponse(
                id = event.id.toString(),
                sourceTopic = event.sourceTopic,
                eventId = event.eventId,
                eventType = event.eventType,
                consumerName = event.consumerName,
                errorCode = event.errorCode,
                errorMessage = event.errorMessage,
                failedAt = event.failedAt.toString(),
            )
    }
}

@AdminOnly
@RestController
@RequestMapping("/admin/api/v1")
class AdminOperationsController(
    private val reviewJpaRepository: ReviewJpaRepository,
    private val reviewAnalysisJpaRepository: ReviewAnalysisJpaRepository,
    private val retryJobJpaRepository: RetryJobJpaRepository,
    private val deadLetterEventJpaRepository: DeadLetterEventJpaRepository,
) {
    @GetMapping("/reviews")
    fun listReviews(): List<AdminReviewResponse> =
        reviewJpaRepository.findAllByOrderByCollectedAtDesc()
            .map { review ->
                AdminReviewResponse.from(
                    review,
                    reviewAnalysisJpaRepository.findFirstByReviewIdOrderByAnalyzedAtDesc(review.id),
                )
            }

    @GetMapping("/analysis-runs")
    fun listAnalysisRuns(): List<AdminAnalysisRunResponse> =
        reviewAnalysisJpaRepository.findAllByOrderByAnalyzedAtDesc().map { AdminAnalysisRunResponse.from(it) }

    @GetMapping("/retry-jobs")
    fun listRetryJobs(): List<AdminRetryJobResponse> =
        retryJobJpaRepository.findAllByOrderByCreatedAtDesc().map { AdminRetryJobResponse.from(it) }

    @GetMapping("/dead-letters")
    fun listDeadLetters(): List<AdminDeadLetterEventResponse> =
        deadLetterEventJpaRepository.findAllByOrderByFailedAtDesc().map { AdminDeadLetterEventResponse.from(it) }
}
