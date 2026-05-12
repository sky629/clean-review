package com.cleanreview.notification.adapter.out.persistence

import com.cleanreview.notification.domain.model.NotificationChannel
import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository

interface NotificationDeliveryJpaRepository : JpaRepository<NotificationDeliveryJpaEntity, UUID> {
    fun existsBySourceEventIdAndChannelAndRecipient(
        sourceEventId: String,
        channel: NotificationChannel,
        recipient: String,
    ): Boolean
}
