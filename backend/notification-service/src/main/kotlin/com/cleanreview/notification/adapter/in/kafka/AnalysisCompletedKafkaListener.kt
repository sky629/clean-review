package com.cleanreview.notification.adapter.`in`.kafka

import com.cleanreview.notification.application.port.`in`.HandleAnalysisCompletedNotificationUseCasePort
import com.cleanreview.notification.application.usecase.AnalysisCompletedNotificationCommand
import java.util.UUID
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

@Component
class AnalysisCompletedKafkaListener(
    private val handleAnalysisCompletedNotificationUseCase: HandleAnalysisCompletedNotificationUseCasePort,
) {
    @KafkaListener(
        topics = ["\${clean-review.kafka.topics.review-analysis-completed:review.analysis.completed}"],
        groupId = "\${spring.kafka.consumer.group-id:notification-service}",
    )
    fun onMessage(envelope: Map<String, Any?>) {
        if (envelope["event_type"] != "review.analysis.completed.v1") {
            return
        }

        val payload = envelope["payload"] as? Map<*, *> ?: error("payload is required")
        handleAnalysisCompletedNotificationUseCase.execute(
            AnalysisCompletedNotificationCommand(
                sourceEventId = requireString(envelope, "idempotency_key"),
                targetId = UUID.fromString(requireString(payload, "target_id")),
                reportId = UUID.fromString(requireString(payload, "report_id")),
                trustScore = requireNumber(payload, "trust_score"),
                viralContaminationScore = requireNumber(payload, "viral_contamination_score"),
                summary = requireString(payload, "summary"),
            ),
        )
    }

    private fun requireString(map: Map<*, *>, key: String): String {
        val value = map[key]
        require(value is String && value.isNotBlank()) { "$key is required" }
        return value
    }

    private fun requireNumber(map: Map<*, *>, key: String): Double {
        val value = map[key]
        require(value is Number) { "$key is required" }
        return value.toDouble()
    }
}
