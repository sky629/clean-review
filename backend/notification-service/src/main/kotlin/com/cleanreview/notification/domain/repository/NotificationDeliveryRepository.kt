package com.cleanreview.notification.domain.repository

import com.cleanreview.notification.domain.model.NotificationChannel
import com.cleanreview.notification.domain.model.NotificationDelivery

interface NotificationDeliveryRepository {
    fun save(delivery: NotificationDelivery): NotificationDelivery

    fun existsBySourceEventIdAndChannelAndRecipient(
        sourceEventId: String,
        channel: NotificationChannel,
        recipient: String,
    ): Boolean

    fun findAll(): List<NotificationDelivery>
}
