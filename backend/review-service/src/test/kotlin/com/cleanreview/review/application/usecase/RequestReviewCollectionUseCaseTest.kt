package com.cleanreview.review.application.usecase

import com.cleanreview.review.application.port.out.ReviewCollectionEventPublisher
import com.cleanreview.review.application.port.out.ReviewCollectionRequestedEvent
import com.cleanreview.review.domain.model.CollectionRun
import com.cleanreview.review.domain.model.CollectionRunId
import com.cleanreview.review.domain.model.CollectionRunReason
import com.cleanreview.review.domain.model.CollectionRunStatus
import com.cleanreview.review.domain.model.ReviewTarget
import com.cleanreview.review.domain.model.ReviewTargetId
import com.cleanreview.review.domain.model.ReviewTargetType
import com.cleanreview.review.domain.repository.CollectionRunRepository
import com.cleanreview.review.domain.repository.ReviewTargetRepository
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import org.junit.jupiter.api.Test

class RequestReviewCollectionUseCaseTest {
    private val clock = Clock.fixed(Instant.parse("2026-05-11T14:00:00Z"), ZoneOffset.UTC)

    @Test
    fun `manual resync overlaps one hour from latest completed collection`() {
        val userId = UUID.randomUUID()
        val target = target(userId)
        val targetRepository = RequestInMemoryReviewTargetRepository(listOf(target))
        val collectionRunRepository = RequestInMemoryCollectionRunRepository()
        collectionRunRepository.save(
            run(target.id, userId)
                .copy(
                    status = CollectionRunStatus.COMPLETED,
                    completedAt = Instant.parse("2026-05-11T12:20:00Z"),
                ),
        )
        val eventPublisher = RequestRecordingReviewCollectionEventPublisher()

        val result = useCase(targetRepository, collectionRunRepository, eventPublisher)
            .requestManualResync(target.id, userId)

        assertEquals(CollectionRunReason.MANUAL_RESYNC, result.runReason)
        assertEquals(Instant.parse("2026-05-11T11:20:00Z"), result.windowFrom)
        assertEquals(Instant.parse("2026-05-11T14:00:00Z"), result.windowTo)
        assertEquals(100, result.maxReviews)
        assertEquals("NAVER_BLOG", result.source)
        assertEquals("MANUAL_RESYNC", eventPublisher.published.single().runReason)
        assertEquals("2026-05-11T11:20:00Z", eventPublisher.published.single().windowFrom)
    }

    @Test
    fun `manual resync falls back to recent thirty days without completed collection`() {
        val userId = UUID.randomUUID()
        val target = target(userId)
        val eventPublisher = RequestRecordingReviewCollectionEventPublisher()

        val result = useCase(
            RequestInMemoryReviewTargetRepository(listOf(target)),
            RequestInMemoryCollectionRunRepository(),
            eventPublisher,
        ).requestManualResync(target.id, userId)

        assertEquals(CollectionRunReason.MANUAL_RESYNC, result.runReason)
        assertEquals(Instant.parse("2026-04-11T14:00:00Z"), result.windowFrom)
        assertEquals(Instant.parse("2026-05-11T14:00:00Z"), result.windowTo)
    }

    @Test
    fun `manual resync rejects another user's review target`() {
        val target = target(UUID.randomUUID())

        assertFailsWith<ReviewTargetAccessDeniedException> {
            useCase(
                RequestInMemoryReviewTargetRepository(listOf(target)),
                RequestInMemoryCollectionRunRepository(),
                RequestRecordingReviewCollectionEventPublisher(),
            ).requestManualResync(target.id, UUID.randomUUID())
        }
    }

    @Test
    fun `manual resync idempotency key includes window so repeated runs do not collide`() {
        val userId = UUID.randomUUID()
        val target = target(userId)
        val first = useCase(
            RequestInMemoryReviewTargetRepository(listOf(target)),
            RequestInMemoryCollectionRunRepository(),
            RequestRecordingReviewCollectionEventPublisher(),
        ).requestManualResync(target.id, userId)
        val second = useCase(
            RequestInMemoryReviewTargetRepository(listOf(target)),
            RequestInMemoryCollectionRunRepository(),
            RequestRecordingReviewCollectionEventPublisher(),
            Clock.fixed(Instant.parse("2026-05-11T14:01:00Z"), ZoneOffset.UTC),
        ).requestManualResync(target.id, userId)

        assert(first.idempotencyKey != second.idempotencyKey)
    }

    @Test
    fun `manual resync returns existing open collection without publishing duplicate event`() {
        val userId = UUID.randomUUID()
        val target = target(userId)
        val collectionRunRepository = RequestInMemoryCollectionRunRepository()
        val openRun = run(target.id, userId).copy(status = CollectionRunStatus.RUNNING)
        collectionRunRepository.save(openRun)
        val eventPublisher = RequestRecordingReviewCollectionEventPublisher()

        val result = useCase(
            RequestInMemoryReviewTargetRepository(listOf(target)),
            collectionRunRepository,
            eventPublisher,
        ).requestManualResync(target.id, userId)

        assertEquals(openRun.id, result.id)
        assertEquals(CollectionRunStatus.RUNNING, result.status)
        assertEquals(emptyList(), eventPublisher.published)
    }


    private fun useCase(
        targetRepository: ReviewTargetRepository,
        collectionRunRepository: CollectionRunRepository,
        eventPublisher: ReviewCollectionEventPublisher,
        testClock: Clock = clock,
    ): RequestReviewCollectionUseCase =
        RequestReviewCollectionUseCase(
            reviewTargetRepository = targetRepository,
            collectionRunRepository = collectionRunRepository,
            reviewCollectionEventPublisher = eventPublisher,
            clock = testClock,
            initialBackfillDays = 30,
            resyncOverlapHours = 1,
            maxReviewsPerSource = 100,
        )
}

private fun target(userId: UUID): ReviewTarget =
    ReviewTarget.create(
        id = ReviewTargetId(UUID.randomUUID()),
        createdBy = userId,
        keyword = "성수동 파스타 맛집",
        type = ReviewTargetType.PLACE,
    )

private fun run(targetId: ReviewTargetId, userId: UUID): CollectionRun =
    CollectionRun.requested(
        id = CollectionRunId(UUID.randomUUID()),
        targetId = targetId,
        source = "NAVER_BLOG",
        keyword = "성수동 파스타 맛집",
        runReason = CollectionRunReason.INITIAL_BACKFILL,
        windowFrom = Instant.parse("2026-04-11T12:00:00Z"),
        windowTo = Instant.parse("2026-05-11T12:00:00Z"),
        maxReviews = 100,
        requestedBy = userId,
    )

private class RequestInMemoryReviewTargetRepository(
    targets: List<ReviewTarget>,
) : ReviewTargetRepository {
    private val targets = targets.associateBy { it.id }

    override fun save(target: ReviewTarget): ReviewTarget = target

    override fun findById(id: ReviewTargetId): ReviewTarget? = targets[id]

    override fun findAllByCreatedBy(userId: UUID): List<ReviewTarget> =
        targets.values.filter { it.createdBy == userId && !it.isDeleted() }

    override fun findAll(): List<ReviewTarget> =
        targets.values.filter { !it.isDeleted() }
}

private class RequestInMemoryCollectionRunRepository : CollectionRunRepository {
    private val runs = mutableListOf<CollectionRun>()

    override fun save(collectionRun: CollectionRun): CollectionRun {
        runs.add(collectionRun)
        return collectionRun
    }

    override fun findById(id: CollectionRunId): CollectionRun? = runs.firstOrNull { it.id == id }

    override fun findAll(): List<CollectionRun> = runs.toList()

    override fun findLatestCompletedByTargetIdAndSource(
        targetId: ReviewTargetId,
        source: String,
    ): CollectionRun? =
        runs
            .filter { it.targetId == targetId && it.source == source && it.status == CollectionRunStatus.COMPLETED }
            .maxByOrNull { it.completedAt ?: Instant.MIN }

    override fun findLatestOpenByTargetIdAndSource(
        targetId: ReviewTargetId,
        source: String,
    ): CollectionRun? =
        runs
            .filter {
                it.targetId == targetId &&
                    it.source == source &&
                    it.status in setOf(CollectionRunStatus.REQUESTED, CollectionRunStatus.RUNNING)
            }
            .maxByOrNull { it.requestedAt }
}

private class RequestRecordingReviewCollectionEventPublisher : ReviewCollectionEventPublisher {
    val published = mutableListOf<ReviewCollectionRequestedEvent>()

    override fun publish(event: ReviewCollectionRequestedEvent) {
        published.add(event)
    }
}
