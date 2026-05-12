package com.cleanreview.notification.adapter.out.persistence

import com.cleanreview.notification.domain.model.NotificationChannel
import com.cleanreview.notification.domain.model.NotificationDelivery
import com.cleanreview.notification.domain.model.NotificationDeliveryId
import com.cleanreview.notification.domain.model.NotificationDeliveryStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "notification_deliveries")
class NotificationDeliveryJpaEntity(
    @Id
    @Column(name = "id", nullable = false)
    val id: UUID = UUID.randomUUID(),

    @Column(name = "notification_type", nullable = false)
    val notificationType: String = "",

    @Column(name = "target_id")
    val targetId: UUID? = null,

    @Column(name = "source_event_id", nullable = false)
    val sourceEventId: String = "",

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false)
    val channel: NotificationChannel = NotificationChannel.TELEGRAM,

    @Column(name = "recipient", nullable = false)
    val recipient: String = "",

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    val status: NotificationDeliveryStatus = NotificationDeliveryStatus.PENDING,

    @Column(name = "message", nullable = false)
    val message: String = "",

    @Column(name = "sent_at")
    val sentAt: Instant? = null,

    @Column(name = "failure_code")
    val failureCode: String? = null,

    @Column(name = "failure_message")
    val failureMessage: String? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    val updatedAt: Instant = Instant.now(),
) {
    fun toDomain(): NotificationDelivery =
        NotificationDelivery(
            id = NotificationDeliveryId(id),
            notificationType = notificationType,
            targetId = targetId,
            sourceEventId = sourceEventId,
            channel = channel,
            recipient = recipient,
            status = status,
            message = message,
            sentAt = sentAt,
            failureCode = failureCode,
            failureMessage = failureMessage,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )

    companion object {
        fun from(delivery: NotificationDelivery): NotificationDeliveryJpaEntity =
            NotificationDeliveryJpaEntity(
                id = delivery.id.value,
                notificationType = delivery.notificationType,
                targetId = delivery.targetId,
                sourceEventId = delivery.sourceEventId,
                channel = delivery.channel,
                recipient = delivery.recipient,
                status = delivery.status,
                message = delivery.message,
                sentAt = delivery.sentAt,
                failureCode = delivery.failureCode,
                failureMessage = delivery.failureMessage,
                createdAt = delivery.createdAt,
                updatedAt = delivery.updatedAt,
            )
    }
}
