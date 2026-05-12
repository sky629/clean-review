package com.cleanreview.review.domain.model

import java.time.Instant
import java.util.UUID

data class CollectionRun(
    val id: CollectionRunId,
    val targetId: ReviewTargetId,
    val source: String,
    val keyword: String,
    val idempotencyKey: String,
    val runReason: CollectionRunReason,
    val windowFrom: Instant,
    val windowTo: Instant,
    val maxReviews: Int,
    val status: CollectionRunStatus,
    val requestedAt: Instant,
    val requestedBy: UUID?,
    val startedAt: Instant? = null,
    val completedAt: Instant? = null,
    val failureCode: String? = null,
    val failureMessage: String? = null,
) {
    companion object {
        fun requested(
            id: CollectionRunId,
            targetId: ReviewTargetId,
            source: String,
            keyword: String,
            runReason: CollectionRunReason,
            windowFrom: Instant,
            windowTo: Instant,
            maxReviews: Int,
            requestedBy: UUID?,
            requestedAt: Instant = Instant.now(),
        ): CollectionRun {
            require(source.isNotBlank()) { "Collection source must not be blank." }
            require(keyword.isNotBlank()) { "Collection keyword must not be blank." }
            require(windowFrom.isBefore(windowTo)) { "Collection window must be ordered." }
            require(maxReviews > 0) { "Collection max reviews must be positive." }

            return CollectionRun(
                id = id,
                targetId = targetId,
                source = source,
                keyword = keyword.trim(),
                idempotencyKey = listOf(
                    "review-collection-request",
                    targetId.value,
                    source,
                    runReason.name,
                    windowFrom.toString(),
                    windowTo.toString(),
                ).joinToString(":"),
                runReason = runReason,
                windowFrom = windowFrom,
                windowTo = windowTo,
                maxReviews = maxReviews,
                status = CollectionRunStatus.REQUESTED,
                requestedAt = requestedAt,
                requestedBy = requestedBy,
            )
        }
    }
}
