package com.cleanreview.notification.adapter.`in`.kafka

import com.cleanreview.notification.application.port.`in`.HandleAnalysisCompletedNotificationUseCasePort
import com.cleanreview.notification.application.usecase.AnalysisCompletedNotificationCommand
import java.util.UUID
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test

class AnalysisCompletedKafkaListenerTest {
    @Test
    fun `maps analysis completed envelope to notification command`() {
        val useCase = RecordingHandleAnalysisCompletedNotificationUseCase()
        val listener = AnalysisCompletedKafkaListener(useCase)
        val targetId = UUID.randomUUID()
        val reportId = UUID.randomUUID()

        listener.onMessage(
            mapOf(
                  "event_id" to "evt-1",
                  "event_type" to "review.analysis.completed.v1",
                  "idempotency_key" to "review-analysis-completed:run-1",
                  "aggregate_id" to targetId.toString(),
                "payload" to mapOf(
                    "target_id" to targetId.toString(),
                    "report_id" to reportId.toString(),
                    "trust_score" to 91.0,
                    "viral_contamination_score" to 12.0,
                    "summary" to "실사용 후기 중심입니다.",
                ),
            ),
        )

        assertEquals("review-analysis-completed:run-1", useCase.commands.single().sourceEventId)
        assertEquals(targetId, useCase.commands.single().targetId)
        assertEquals(reportId, useCase.commands.single().reportId)
    }
}

private class RecordingHandleAnalysisCompletedNotificationUseCase :
    HandleAnalysisCompletedNotificationUseCasePort {
    val commands = mutableListOf<AnalysisCompletedNotificationCommand>()

    override fun execute(command: AnalysisCompletedNotificationCommand) {
        commands.add(command)
    }
}
