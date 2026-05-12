package com.cleanreview.notification.adapter.out.persistence

import com.cleanreview.notification.domain.model.NotificationChannel
import com.cleanreview.notification.domain.model.NotificationDelivery
import com.cleanreview.notification.domain.model.NotificationDeliveryId
import java.util.UUID
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test

class NotificationDeliveryPersistenceAdapterTest {
    @Test
    fun `maps notification delivery domain to jpa entity and back`() {
        val delivery = NotificationDelivery.create(
            id = NotificationDeliveryId(UUID.randomUUID()),
            notificationType = "REVIEW_ANALYSIS_COMPLETED",
            targetId = UUID.randomUUID(),
            sourceEventId = "evt-1",
            channel = NotificationChannel.TELEGRAM,
            recipient = "chat-1",
            message = "리뷰 분석이 완료됐습니다.",
        ).markSent()

        val mapped = NotificationDeliveryJpaEntity.from(delivery).toDomain()

        assertEquals(delivery, mapped)
    }
}
