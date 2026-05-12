package com.cleanreview.review.adapter.out.persistence

import com.cleanreview.review.domain.model.CollectionRun
import com.cleanreview.review.domain.model.CollectionRunId
import com.cleanreview.review.domain.model.CollectionRunReason
import com.cleanreview.review.domain.model.CollectionRunStatus
import com.cleanreview.review.domain.model.ReviewTargetId
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "collection_runs")
class CollectionRunJpaEntity(
    @Id
    @Column(name = "id", nullable = false)
    val id: UUID = UUID.randomUUID(),

    @Column(name = "target_id", nullable = false)
    val targetId: UUID = UUID.randomUUID(),

    @Column(name = "source", nullable = false)
    val source: String = "",

    @Column(name = "keyword", nullable = false)
    val keyword: String = "",

    @Column(name = "idempotency_key", nullable = false, unique = true)
    val idempotencyKey: String = "",

    @Enumerated(EnumType.STRING)
    @Column(name = "run_reason", nullable = false)
    val runReason: CollectionRunReason = CollectionRunReason.INITIAL_BACKFILL,

    @Column(name = "window_from", nullable = false)
    val windowFrom: Instant = Instant.now(),

    @Column(name = "window_to", nullable = false)
    val windowTo: Instant = Instant.now(),

    @Column(name = "max_reviews", nullable = false)
    val maxReviews: Int = 100,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    val status: CollectionRunStatus = CollectionRunStatus.REQUESTED,

    @Column(name = "requested_at", nullable = false)
    val requestedAt: Instant = Instant.now(),

    @Column(name = "requested_by")
    val requestedBy: UUID? = null,

    @Column(name = "started_at")
    val startedAt: Instant? = null,

    @Column(name = "completed_at")
    val completedAt: Instant? = null,

    @Column(name = "failure_code")
    val failureCode: String? = null,

    @Column(name = "failure_message")
    val failureMessage: String? = null,
) {
    fun toDomain(): CollectionRun =
        CollectionRun(
            id = CollectionRunId(id),
            targetId = ReviewTargetId(targetId),
            source = source,
            keyword = keyword,
            idempotencyKey = idempotencyKey,
            runReason = runReason,
            windowFrom = windowFrom,
            windowTo = windowTo,
            maxReviews = maxReviews,
            status = status,
            requestedAt = requestedAt,
            requestedBy = requestedBy,
            startedAt = startedAt,
            completedAt = completedAt,
            failureCode = failureCode,
            failureMessage = failureMessage,
        )

    companion object {
        fun from(collectionRun: CollectionRun): CollectionRunJpaEntity =
            CollectionRunJpaEntity(
                id = collectionRun.id.value,
                targetId = collectionRun.targetId.value,
                source = collectionRun.source,
                keyword = collectionRun.keyword,
                idempotencyKey = collectionRun.idempotencyKey,
                runReason = collectionRun.runReason,
                windowFrom = collectionRun.windowFrom,
                windowTo = collectionRun.windowTo,
                maxReviews = collectionRun.maxReviews,
                status = collectionRun.status,
                requestedAt = collectionRun.requestedAt,
                requestedBy = collectionRun.requestedBy,
                startedAt = collectionRun.startedAt,
                completedAt = collectionRun.completedAt,
                failureCode = collectionRun.failureCode,
                failureMessage = collectionRun.failureMessage,
            )
    }
}
