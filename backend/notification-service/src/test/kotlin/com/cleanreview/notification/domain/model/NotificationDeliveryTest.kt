package com.cleanreview.notification.domain.model

import java.util.UUID
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test

class NotificationDeliveryTest {
    @Test
    fun `creates pending delivery for analysis completion`() {
        val delivery = NotificationDelivery.create(
            id = NotificationDeliveryId(UUID.randomUUID()),
            notificationType = "REVIEW_ANALYSIS_COMPLETED",
            targetId = UUID.randomUUID(),
            sourceEventId = "evt-1",
            channel = NotificationChannel.TELEGRAM,
            recipient = "chat-1",
            message = "리뷰 분석이 완료됐습니다.",
        )

        assertEquals(NotificationDeliveryStatus.PENDING, delivery.status)
        assertEquals("evt-1", delivery.sourceEventId)
    }

    @Test
    fun `marks delivery as sent`() {
        val delivery = NotificationDelivery.create(
            id = NotificationDeliveryId(UUID.randomUUID()),
            notificationType = "REVIEW_ANALYSIS_COMPLETED",
            targetId = UUID.randomUUID(),
            sourceEventId = "evt-1",
            channel = NotificationChannel.TELEGRAM,
            recipient = "chat-1",
            message = "리뷰 분석이 완료됐습니다.",
        )

        val sent = delivery.markSent()

        assertEquals(NotificationDeliveryStatus.SENT, sent.status)
    }
}
