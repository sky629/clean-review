package com.cleanreview.notification.application.port.out

import com.cleanreview.notification.domain.model.NotificationChannel

data class SendNotificationCommand(
    val channel: NotificationChannel,
    val recipient: String,
    val message: String,
)

interface NotificationSender {
    fun send(command: SendNotificationCommand)
}
