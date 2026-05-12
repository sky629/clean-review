package com.cleanreview.notification.adapter.out.persistence

import com.cleanreview.notification.domain.model.NotificationChannel
import com.cleanreview.notification.domain.model.NotificationDelivery
import com.cleanreview.notification.domain.repository.NotificationDeliveryRepository
import org.springframework.stereotype.Repository

@Repository
class NotificationDeliveryPersistenceAdapter(
    private val notificationDeliveryJpaRepository: NotificationDeliveryJpaRepository,
) : NotificationDeliveryRepository {
    override fun save(delivery: NotificationDelivery): NotificationDelivery =
        notificationDeliveryJpaRepository.save(NotificationDeliveryJpaEntity.from(delivery)).toDomain()

    override fun existsBySourceEventIdAndChannelAndRecipient(
        sourceEventId: String,
        channel: NotificationChannel,
        recipient: String,
    ): Boolean =
        notificationDeliveryJpaRepository.existsBySourceEventIdAndChannelAndRecipient(
            sourceEventId,
            channel,
            recipient,
        )

    override fun findAll(): List<NotificationDelivery> =
        notificationDeliveryJpaRepository.findAll().map { it.toDomain() }
}
