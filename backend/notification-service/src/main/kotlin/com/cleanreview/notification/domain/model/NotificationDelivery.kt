package com.cleanreview.notification.domain.model

import java.time.Instant
import java.util.UUID

data class NotificationDelivery(
    val id: NotificationDeliveryId,
    val notificationType: String,
    val targetId: UUID?,
    val sourceEventId: String,
    val channel: NotificationChannel,
    val recipient: String,
    val status: NotificationDeliveryStatus,
    val message: String,
    val sentAt: Instant?,
    val failureCode: String?,
    val failureMessage: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    fun markSent(now: Instant = Instant.now()): NotificationDelivery =
        copy(
            status = NotificationDeliveryStatus.SENT,
            sentAt = now,
            failureCode = null,
            failureMessage = null,
            updatedAt = now,
        )

    fun markFailed(code: String, message: String, now: Instant = Instant.now()): NotificationDelivery =
        copy(
            status = NotificationDeliveryStatus.FAILED,
            failureCode = code,
            failureMessage = message,
            updatedAt = now,
        )

    companion object {
        fun create(
            id: NotificationDeliveryId,
            notificationType: String,
            targetId: UUID?,
            sourceEventId: String,
            channel: NotificationChannel,
            recipient: String,
            message: String,
        ): NotificationDelivery {
            require(notificationType.isNotBlank()) { "notificationType must not be blank." }
            require(sourceEventId.isNotBlank()) { "sourceEventId must not be blank." }
            require(recipient.isNotBlank()) { "recipient must not be blank." }
            require(message.isNotBlank()) { "message must not be blank." }
            val now = Instant.now()

            return NotificationDelivery(
                id = id,
                notificationType = notificationType,
                targetId = targetId,
                sourceEventId = sourceEventId,
                channel = channel,
                recipient = recipient,
                status = NotificationDeliveryStatus.PENDING,
                message = message,
                sentAt = null,
                failureCode = null,
                failureMessage = null,
                createdAt = now,
                updatedAt = now,
            )
        }
    }
}
