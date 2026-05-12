package com.cleanreview.notification.application.usecase

import com.cleanreview.notification.application.port.`in`.HandleAnalysisCompletedNotificationUseCasePort
import com.cleanreview.notification.application.port.out.NotificationSender
import com.cleanreview.notification.application.port.out.SendNotificationCommand
import com.cleanreview.notification.domain.model.NotificationChannel
import com.cleanreview.notification.domain.model.NotificationDelivery
import com.cleanreview.notification.domain.model.NotificationDeliveryId
import com.cleanreview.notification.domain.repository.NotificationDeliveryRepository
import java.util.UUID
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

data class AnalysisCompletedNotificationCommand(
    val sourceEventId: String,
    val targetId: UUID,
    val reportId: UUID,
    val trustScore: Double,
    val viralContaminationScore: Double,
    val summary: String,
)

@Service
class HandleAnalysisCompletedNotificationUseCase(
    private val notificationDeliveryRepository: NotificationDeliveryRepository,
    private val notificationSender: NotificationSender,
    @Value("\${clean-review.telegram.chat-id:}") private val telegramRecipient: String,
) : HandleAnalysisCompletedNotificationUseCasePort {
    @Transactional
    override fun execute(command: AnalysisCompletedNotificationCommand) {
        if (notificationDeliveryRepository.existsBySourceEventIdAndChannelAndRecipient(
                sourceEventId = command.sourceEventId,
                channel = NotificationChannel.TELEGRAM,
                recipient = telegramRecipient,
            )
        ) {
            return
        }

        val delivery = NotificationDelivery.create(
            id = NotificationDeliveryId(UUID.randomUUID()),
            notificationType = "REVIEW_ANALYSIS_COMPLETED",
            targetId = command.targetId,
            sourceEventId = command.sourceEventId,
            channel = NotificationChannel.TELEGRAM,
            recipient = telegramRecipient,
            message = buildMessage(command),
        )
        notificationDeliveryRepository.save(delivery)

        val sent = try {
            notificationSender.send(
                SendNotificationCommand(
                    channel = NotificationChannel.TELEGRAM,
                    recipient = telegramRecipient,
                    message = delivery.message,
                ),
            )
            delivery.markSent()
        } catch (exc: RuntimeException) {
            delivery.markFailed(exc::class.simpleName ?: "NOTIFICATION_SEND_FAILED", exc.message ?: "")
        }

        notificationDeliveryRepository.save(sent)
    }

    private fun buildMessage(command: AnalysisCompletedNotificationCommand): String =
        "리뷰 분석이 완료됐습니다. " +
            "신뢰도=${command.trustScore}, 바이럴 오염도=${command.viralContaminationScore}. " +
            command.summary
}
