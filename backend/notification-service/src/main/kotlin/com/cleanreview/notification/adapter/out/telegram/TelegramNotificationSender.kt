package com.cleanreview.notification.adapter.out.telegram

import com.cleanreview.notification.application.port.out.NotificationSender
import com.cleanreview.notification.application.port.out.SendNotificationCommand
import com.cleanreview.notification.domain.model.NotificationChannel
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

@Component
class TelegramNotificationSender(
    @Value("\${clean-review.telegram.bot-token:}") private val botToken: String,
) : NotificationSender {
    private val restClient = RestClient.create()

    override fun send(command: SendNotificationCommand) {
        if (command.channel != NotificationChannel.TELEGRAM) {
            error("Unsupported notification channel: ${command.channel}")
        }
        if (botToken.isBlank()) {
            return
        }

        restClient.post()
            .uri("https://api.telegram.org/bot{token}/sendMessage", botToken)
            .body(mapOf("chat_id" to command.recipient, "text" to command.message))
            .retrieve()
            .toBodilessEntity()
    }
}
