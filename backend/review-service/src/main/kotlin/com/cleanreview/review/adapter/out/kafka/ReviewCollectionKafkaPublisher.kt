package com.cleanreview.review.adapter.out.kafka

import com.cleanreview.review.application.port.out.ReviewCollectionEventPublisher
import com.cleanreview.review.application.port.out.ReviewCollectionRequestedEvent
import java.time.Instant
import java.util.UUID
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component

@Component
class ReviewCollectionKafkaPublisher(
    private val kafkaTemplate: KafkaTemplate<String, Map<String, Any?>>,
    @Value("\${clean-review.kafka.topics.review-collection-requested:review.collection.requested}")
    private val topic: String,
) : ReviewCollectionEventPublisher {
    override fun publish(event: ReviewCollectionRequestedEvent) {
        val eventId = UUID.randomUUID().toString()
        val envelope = mapOf(
            "event_id" to eventId,
            "event_type" to event.eventType,
            "schema_version" to 1,
            "occurred_at" to Instant.now().toString(),
            "correlation_id" to eventId,
            "idempotency_key" to "review-collection-request:${event.targetId}",
            "aggregate_id" to event.targetId,
            "payload" to mapOf(
                "collection_run_id" to event.collectionRunId,
                "target_id" to event.targetId,
                "source" to event.source,
                "keyword" to event.keyword,
                "requested_by" to event.requestedBy,
                "window_from" to event.windowFrom,
                "window_to" to event.windowTo,
                "max_reviews" to event.maxReviews,
                "run_reason" to event.runReason,
            ),
        )

        kafkaTemplate.send(topic, event.targetId, envelope)
    }
}
