package com.cleanreview.review.adapter.`in`.web.admin

import com.cleanreview.review.adapter.out.persistence.RetryJobJpaEntity
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.web.bind.annotation.RequestMapping

class AdminOperationsControllerTest {
    @Test
    fun `admin operations controller uses admin operations prefix`() {
        val mapping = AdminOperationsController::class.java.getAnnotation(RequestMapping::class.java)

        assertEquals("/admin/api/v1", mapping.value.single())
    }

    @Test
    fun `admin retry job response includes last error message`() {
        val retryJob = RetryJobJpaEntity(
            id = UUID.fromString("00000000-0000-0000-0000-000000000001"),
            topic = "review.collection.requested",
            eventType = "ReviewCollectionRequested",
            originalEventId = "evt-1",
            idempotencyKey = "idempotency-1",
            correlationId = "correlation-1",
            attempt = 2,
            maxAttempts = 3,
            nextAttemptAt = Instant.parse("2026-05-11T00:00:00Z"),
            lastErrorCode = "CollectionBlocked",
            lastErrorMessage = "Google returned a blocked search page.",
            status = "SCHEDULED",
            createdAt = Instant.parse("2026-05-11T00:00:00Z"),
            updatedAt = Instant.parse("2026-05-11T00:00:00Z"),
        )

        val response = AdminRetryJobResponse.from(retryJob)

        assertEquals("CollectionBlocked", response.lastErrorCode)
        assertEquals("Google returned a blocked search page.", response.lastErrorMessage)
    }
}
