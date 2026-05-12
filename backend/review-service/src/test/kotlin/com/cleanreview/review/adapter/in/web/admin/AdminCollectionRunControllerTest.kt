package com.cleanreview.review.adapter.`in`.web.admin

import com.cleanreview.review.domain.model.CollectionRun
import com.cleanreview.review.domain.model.CollectionRunId
import com.cleanreview.review.domain.model.CollectionRunReason
import com.cleanreview.review.domain.model.CollectionRunStatus
import com.cleanreview.review.domain.model.ReviewTargetId
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.web.bind.annotation.RequestMapping

class AdminCollectionRunControllerTest {
    @Test
    fun `admin collection run controller uses admin api prefix`() {
        val mapping = AdminCollectionRunController::class.java.getAnnotation(RequestMapping::class.java)

        assertEquals("/admin/api/v1/collection-runs", mapping.value.single())
    }

    @Test
    fun `admin collection run response includes failure reason`() {
        val failedRun = CollectionRun(
            id = CollectionRunId(UUID.fromString("00000000-0000-0000-0000-000000000001")),
            targetId = ReviewTargetId(UUID.fromString("00000000-0000-0000-0000-000000000002")),
            source = "NAVER_BLOG",
            keyword = "성수동 파스타",
            idempotencyKey = "review-collection-request:target:NAVER_BLOG",
            runReason = CollectionRunReason.MANUAL_RESYNC,
            windowFrom = Instant.parse("2026-05-10T23:00:00Z"),
            windowTo = Instant.parse("2026-05-11T00:00:00Z"),
            maxReviews = 100,
            status = CollectionRunStatus.FAILED,
            requestedAt = Instant.parse("2026-05-11T00:00:00Z"),
            requestedBy = UUID.fromString("00000000-0000-0000-0000-000000000003"),
            failureCode = "CollectionBlocked",
            failureMessage = "Naver blocked the collection page.",
        )

        val response = AdminCollectionRunResponse.from(failedRun)

        assertEquals("CollectionBlocked", response.failureCode)
        assertEquals("Naver blocked the collection page.", response.failureMessage)
    }
}
