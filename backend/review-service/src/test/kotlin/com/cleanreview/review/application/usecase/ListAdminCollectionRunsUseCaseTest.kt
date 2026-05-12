package com.cleanreview.review.application.usecase

import com.cleanreview.review.domain.model.CollectionRun
import com.cleanreview.review.domain.model.CollectionRunId
import com.cleanreview.review.domain.model.CollectionRunReason
import com.cleanreview.review.domain.model.CollectionRunStatus
import com.cleanreview.review.domain.model.ReviewTargetId
import com.cleanreview.review.domain.repository.CollectionRunRepository
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test

class ListAdminCollectionRunsUseCaseTest {
    @Test
    fun `admin lists collection run history`() {
        val repository = AdminRunInMemoryCollectionRunRepository()
        val first = repository.save(run("성수동 파스타 맛집"))
        val second = repository.save(run("무선 청소기 후기"))

        val result = ListAdminCollectionRunsUseCase(repository).execute()

        assertEquals(listOf(first, second), result)
    }

    private fun run(keyword: String): CollectionRun =
        CollectionRun.requested(
            id = CollectionRunId(UUID.randomUUID()),
            targetId = ReviewTargetId(UUID.randomUUID()),
            source = "NAVER_BLOG",
            keyword = keyword,
            runReason = CollectionRunReason.INITIAL_BACKFILL,
            windowFrom = Instant.parse("2026-04-11T12:00:00Z"),
            windowTo = Instant.parse("2026-05-11T12:00:00Z"),
            maxReviews = 100,
            requestedBy = UUID.randomUUID(),
        )
}

private class AdminRunInMemoryCollectionRunRepository : CollectionRunRepository {
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
}
