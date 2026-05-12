package com.cleanreview.notification.application.usecase

import com.cleanreview.notification.application.port.out.NotificationSender
import com.cleanreview.notification.application.port.out.SendNotificationCommand
import com.cleanreview.notification.domain.model.NotificationChannel
import com.cleanreview.notification.domain.model.NotificationDelivery
import com.cleanreview.notification.domain.model.NotificationDeliveryId
import com.cleanreview.notification.domain.repository.NotificationDeliveryRepository
import java.util.UUID
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test

class HandleAnalysisCompletedNotificationUseCaseTest {
    @Test
    fun `sends telegram notification and stores sent delivery`() {
        val repository = InMemoryNotificationDeliveryRepository()
        val sender = RecordingNotificationSender()
        val useCase = HandleAnalysisCompletedNotificationUseCase(
            notificationDeliveryRepository = repository,
            notificationSender = sender,
            telegramRecipient = "chat-1",
        )

        useCase.execute(
            AnalysisCompletedNotificationCommand(
                sourceEventId = "evt-1",
                targetId = UUID.randomUUID(),
                reportId = UUID.randomUUID(),
                trustScore = 91.0,
                viralContaminationScore = 12.0,
                summary = "실사용 후기 중심입니다.",
            ),
        )

        assertEquals(1, sender.sent.size)
        assertEquals(NotificationChannel.TELEGRAM, sender.sent.single().channel)
        assertEquals("chat-1", sender.sent.single().recipient)
        assertEquals("SENT", repository.findAll().single().status.name)
    }

    @Test
    fun `does not send duplicate delivery for same event channel and recipient`() {
        val repository = InMemoryNotificationDeliveryRepository()
        val sender = RecordingNotificationSender()
        val useCase = HandleAnalysisCompletedNotificationUseCase(repository, sender, "chat-1")
        val command = AnalysisCompletedNotificationCommand(
            sourceEventId = "evt-1",
            targetId = UUID.randomUUID(),
            reportId = UUID.randomUUID(),
            trustScore = 91.0,
            viralContaminationScore = 12.0,
            summary = "실사용 후기 중심입니다.",
        )

        useCase.execute(command)
        useCase.execute(command)

        assertEquals(1, sender.sent.size)
        assertEquals(1, repository.findAll().size)
    }
}

private class RecordingNotificationSender : NotificationSender {
    val sent = mutableListOf<SendNotificationCommand>()

    override fun send(command: SendNotificationCommand) {
        sent.add(command)
    }
}

private class InMemoryNotificationDeliveryRepository : NotificationDeliveryRepository {
    private val deliveries = linkedMapOf<NotificationDeliveryId, NotificationDelivery>()

    override fun save(delivery: NotificationDelivery): NotificationDelivery {
        deliveries[delivery.id] = delivery
        return delivery
    }

    override fun existsBySourceEventIdAndChannelAndRecipient(
        sourceEventId: String,
        channel: NotificationChannel,
        recipient: String,
    ): Boolean =
        deliveries.values.any {
            it.sourceEventId == sourceEventId &&
                it.channel == channel &&
                it.recipient == recipient
        }

    override fun findAll(): List<NotificationDelivery> = deliveries.values.toList()
}
