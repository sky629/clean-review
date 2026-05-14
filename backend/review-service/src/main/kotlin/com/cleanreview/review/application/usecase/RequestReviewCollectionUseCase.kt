package com.cleanreview.review.application.usecase

import com.cleanreview.review.application.port.out.ReviewCollectionRequestedEvent
import com.cleanreview.review.domain.model.CollectionRun
import com.cleanreview.review.domain.model.CollectionRunId
import com.cleanreview.review.domain.model.CollectionRunReason
import com.cleanreview.review.domain.model.ReviewCollectionSource
import com.cleanreview.review.domain.model.ReviewTarget
import com.cleanreview.review.domain.model.ReviewTargetId
import com.cleanreview.review.domain.repository.CollectionRunRepository
import com.cleanreview.review.domain.repository.ReviewTargetRepository
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.UUID
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class RequestReviewCollectionUseCase(
    private val reviewTargetRepository: ReviewTargetRepository,
    private val collectionRunRepository: CollectionRunRepository,
    private val applicationEventPublisher: ApplicationEventPublisher,
    private val clock: Clock,
    @Value("\${clean-review.collection.initial-backfill-days:30}")
    private val initialBackfillDays: Long,
    @Value("\${clean-review.collection.resync-overlap-hours:1}")
    private val resyncOverlapHours: Long,
    @Value("\${clean-review.collection.max-reviews-per-source:100}")
    private val maxReviewsPerSource: Int,
) {
    @Transactional
    fun requestInitialBackfill(target: ReviewTarget, requestedBy: UUID): CollectionRun =
        requestCollection(
            target = target,
            requestedBy = requestedBy,
            runReason = CollectionRunReason.INITIAL_BACKFILL,
            windowFrom = Instant.now(clock).minus(Duration.ofDays(initialBackfillDays)),
            windowTo = Instant.now(clock),
        )

    @Transactional
    fun requestManualResync(targetId: ReviewTargetId, requestedBy: UUID): CollectionRun {
        val target = reviewTargetRepository.findById(targetId) ?: throw ReviewTargetNotFoundException()
        if (target.createdBy != requestedBy || target.isDeleted()) {
            throw ReviewTargetAccessDeniedException()
        }
        val source = ReviewCollectionSource.NAVER_BLOG.name
        collectionRunRepository.findLatestOpenByTargetIdAndSource(targetId, source)?.let {
            return it
        }

        val now = Instant.now(clock)
        val latestCompleted = collectionRunRepository
            .findLatestCompletedByTargetIdAndSource(targetId, source)
            ?.completedAt
        val windowFrom = latestCompleted
            ?.minus(Duration.ofHours(resyncOverlapHours))
            ?: now.minus(Duration.ofDays(initialBackfillDays))

        return requestCollection(
            target = target,
            requestedBy = requestedBy,
            runReason = CollectionRunReason.MANUAL_RESYNC,
            windowFrom = windowFrom,
            windowTo = now,
        )
    }

    private fun requestCollection(
        target: ReviewTarget,
        requestedBy: UUID,
        runReason: CollectionRunReason,
        windowFrom: Instant,
        windowTo: Instant,
    ): CollectionRun {
        val collectionRun = collectionRunRepository.save(
            CollectionRun.requested(
                id = CollectionRunId(UUID.randomUUID()),
                targetId = target.id,
                source = ReviewCollectionSource.NAVER_BLOG.name,
                keyword = target.keyword,
                runReason = runReason,
                windowFrom = windowFrom,
                windowTo = windowTo,
                maxReviews = maxReviewsPerSource,
                requestedBy = requestedBy,
                requestedAt = Instant.now(clock),
            ),
        )

        applicationEventPublisher.publishEvent(
            ReviewCollectionRequestedEvent(
                idempotencyKey = collectionRun.idempotencyKey,
                collectionRunId = collectionRun.id.value.toString(),
                targetId = target.id.value.toString(),
                source = collectionRun.source,
                keyword = collectionRun.keyword,
                requestedBy = requestedBy.toString(),
                windowFrom = collectionRun.windowFrom.toString(),
                windowTo = collectionRun.windowTo.toString(),
                maxReviews = collectionRun.maxReviews,
                runReason = collectionRun.runReason.name,
            ),
        )

        return collectionRun
    }
}
