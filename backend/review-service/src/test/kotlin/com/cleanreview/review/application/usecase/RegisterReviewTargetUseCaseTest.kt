package com.cleanreview.review.application.usecase

import com.cleanreview.review.application.command.RegisterReviewTargetCommand
import com.cleanreview.review.application.port.out.ReviewCollectionRequestedEvent
import com.cleanreview.review.domain.model.CollectionRun
import com.cleanreview.review.domain.model.CollectionRunId
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
import org.junit.jupiter.api.Test
import org.springframework.context.ApplicationEventPublisher

class RegisterReviewTargetUseCaseTest {
    @Test
    fun `registers review target and requests first collection`() {
        val reviewTargetRepository = RegisterInMemoryReviewTargetRepository()
        val collectionRunRepository = RegisterInMemoryCollectionRunRepository()
        val eventPublisher = RegisterRecordingReviewCollectionEventPublisher()
        val requestCollectionUseCase = RequestReviewCollectionUseCase(
            reviewTargetRepository = reviewTargetRepository,
            collectionRunRepository = collectionRunRepository,
              applicationEventPublisher = eventPublisher,
            clock = Clock.fixed(Instant.parse("2026-05-11T12:00:00Z"), ZoneOffset.UTC),
            initialBackfillDays = 30,
            resyncOverlapHours = 1,
            maxReviewsPerSource = 100,
        )
        val useCase = RegisterReviewTargetUseCase(
            reviewTargetRepository = reviewTargetRepository,
            requestReviewCollectionUseCase = requestCollectionUseCase,
        )
        val userId = UUID.randomUUID()

        val result = useCase.execute(
            RegisterReviewTargetCommand(
                userId = userId,
                type = ReviewTargetType.PLACE,
                keyword = "성수동 파스타 맛집",
            ),
        )

        assertEquals(userId, result.createdBy)
        assertEquals("성수동 파스타 맛집", result.keyword)
        assertEquals(listOf(result), reviewTargetRepository.findAllByCreatedBy(userId))

        val collectionRuns = collectionRunRepository.findAllByTargetId(result.id)
        assertEquals(listOf("NAVER_BLOG"), collectionRuns.map { it.source })
        collectionRuns.forEach { collectionRun ->
            assertEquals(CollectionRunStatus.REQUESTED, collectionRun.status)
            assertEquals("성수동 파스타 맛집", collectionRun.keyword)
            assertEquals("INITIAL_BACKFILL", collectionRun.runReason.name)
            assertEquals(Instant.parse("2026-04-11T12:00:00Z"), collectionRun.windowFrom)
            assertEquals(Instant.parse("2026-05-11T12:00:00Z"), collectionRun.windowTo)
            assertEquals(100, collectionRun.maxReviews)
        }

        assertEquals(listOf("NAVER_BLOG"), eventPublisher.published.map { it.source })
        eventPublisher.published.zip(collectionRuns).forEach { (event, collectionRun) ->
            assertEquals("review.collection.requested.v1", event.eventType)
            assertEquals(result.id.value.toString(), event.targetId)
            assertEquals(collectionRun.id.value.toString(), event.collectionRunId)
            assertEquals("성수동 파스타 맛집", event.keyword)
            assertEquals("INITIAL_BACKFILL", event.runReason)
            assertEquals("2026-04-11T12:00:00Z", event.windowFrom)
            assertEquals("2026-05-11T12:00:00Z", event.windowTo)
            assertEquals(100, event.maxReviews)
        }
    }

    @Test
    fun `registering the same keyword returns existing target without duplicate collection`() {
        val reviewTargetRepository = RegisterInMemoryReviewTargetRepository()
        val collectionRunRepository = RegisterInMemoryCollectionRunRepository()
        val eventPublisher = RegisterRecordingReviewCollectionEventPublisher()
        val requestCollectionUseCase = RequestReviewCollectionUseCase(
            reviewTargetRepository = reviewTargetRepository,
            collectionRunRepository = collectionRunRepository,
            applicationEventPublisher = eventPublisher,
            clock = Clock.fixed(Instant.parse("2026-05-11T12:00:00Z"), ZoneOffset.UTC),
            initialBackfillDays = 30,
            resyncOverlapHours = 1,
            maxReviewsPerSource = 100,
        )
        val useCase = RegisterReviewTargetUseCase(
            reviewTargetRepository = reviewTargetRepository,
            requestReviewCollectionUseCase = requestCollectionUseCase,
        )
        val userId = UUID.randomUUID()
        val command = RegisterReviewTargetCommand(
            userId = userId,
            type = ReviewTargetType.PLACE,
            keyword = " 성수동 파스타 맛집 ",
        )

        val first = useCase.execute(command)
        val second = useCase.execute(command.copy(keyword = "성수동 파스타 맛집"))

        assertEquals(first.id, second.id)
        assertEquals(1, reviewTargetRepository.findAllByCreatedBy(userId).size)
        assertEquals(1, collectionRunRepository.findAllByTargetId(first.id).size)
        assertEquals(1, eventPublisher.published.size)
    }
}

private class RegisterInMemoryReviewTargetRepository : ReviewTargetRepository {
    private val targets = linkedMapOf<ReviewTargetId, ReviewTarget>()

    override fun save(target: ReviewTarget): ReviewTarget {
        targets[target.id] = target
        return target
    }

    override fun findById(id: ReviewTargetId): ReviewTarget? = targets[id]

    override fun findAllByCreatedBy(userId: UUID): List<ReviewTarget> =
        targets.values.filter { it.createdBy == userId && !it.isDeleted() }

    override fun findActiveByCreatedByAndTypeAndKeyword(
        userId: UUID,
        type: com.cleanreview.review.domain.model.ReviewTargetType,
        keyword: String,
    ): ReviewTarget? =
        targets.values.firstOrNull {
            it.createdBy == userId &&
                it.type == type &&
                it.keyword.trim().equals(keyword.trim(), ignoreCase = true) &&
                !it.isDeleted()
        }

    override fun findAll(): List<ReviewTarget> =
        targets.values.filter { !it.isDeleted() }
}

private class RegisterInMemoryCollectionRunRepository : CollectionRunRepository {
    private val runs = mutableListOf<CollectionRun>()

    override fun save(collectionRun: CollectionRun): CollectionRun {
        runs.removeIf { it.id == collectionRun.id }
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

    fun findAllByTargetId(targetId: ReviewTargetId): List<CollectionRun> =
        runs.filter { it.targetId == targetId }
}

private class RegisterRecordingReviewCollectionEventPublisher : ApplicationEventPublisher {
    val published = mutableListOf<ReviewCollectionRequestedEvent>()

    override fun publishEvent(event: Any) {
        published.add(event as ReviewCollectionRequestedEvent)
    }
}
