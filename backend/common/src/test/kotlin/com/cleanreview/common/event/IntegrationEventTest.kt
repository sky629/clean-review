package com.cleanreview.common.event

import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import org.junit.jupiter.api.Test

class IntegrationEventTest {
    @Test
    fun `captures stable event metadata`() {
        val occurredAt = Instant.parse("2026-05-07T00:00:00Z")

        val event = IntegrationEvent(
            eventId = "evt-1",
            eventType = "review-target.created",
            aggregateId = "target-1",
            occurredAt = occurredAt,
            payload = mapOf("title" to "Clean Review"),
        )

        assertEquals("evt-1", event.eventId)
        assertEquals("review-target.created", event.eventType)
        assertEquals("target-1", event.aggregateId)
        assertEquals(occurredAt, event.occurredAt)
        assertFalse(event.isEmptyPayload())
    }
}
