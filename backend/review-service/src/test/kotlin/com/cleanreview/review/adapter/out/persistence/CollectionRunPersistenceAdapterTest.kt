package com.cleanreview.review.adapter.out.persistence

import com.cleanreview.review.domain.model.CollectionRun
import com.cleanreview.review.domain.model.CollectionRunId
import com.cleanreview.review.domain.model.CollectionRunReason
import com.cleanreview.review.domain.model.CollectionRunStatus
import com.cleanreview.review.domain.model.ReviewTargetId
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test

class CollectionRunPersistenceAdapterTest {
    @Test
    fun `maps collection run domain to jpa entity and back`() {
        val run = CollectionRun.requested(
            id = CollectionRunId(UUID.randomUUID()),
            targetId = ReviewTargetId(UUID.randomUUID()),
            source = "NAVER_BLOG",
            keyword = "성수동 파스타 맛집",
            runReason = CollectionRunReason.INITIAL_BACKFILL,
            windowFrom = Instant.parse("2026-04-11T12:00:00Z"),
            windowTo = Instant.parse("2026-05-11T12:00:00Z"),
            maxReviews = 100,
            requestedBy = UUID.randomUUID(),
        )

        val entity = CollectionRunJpaEntity.from(run)
        val mapped = entity.toDomain()

        assertEquals(run.id, mapped.id)
        assertEquals(run.targetId, mapped.targetId)
        assertEquals(run.source, mapped.source)
        assertEquals(run.keyword, mapped.keyword)
        assertEquals(run.idempotencyKey, mapped.idempotencyKey)
        assertEquals(CollectionRunReason.INITIAL_BACKFILL, mapped.runReason)
        assertEquals(run.windowFrom, mapped.windowFrom)
        assertEquals(run.windowTo, mapped.windowTo)
        assertEquals(100, mapped.maxReviews)
        assertEquals(CollectionRunStatus.REQUESTED, mapped.status)
    }
}
