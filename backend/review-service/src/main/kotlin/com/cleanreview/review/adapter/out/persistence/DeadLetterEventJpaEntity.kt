package com.cleanreview.review.adapter.out.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes

@Entity
@Table(name = "dead_letter_events")
class DeadLetterEventJpaEntity(
    @Id
    @Column(name = "id", nullable = false)
    val id: UUID = UUID.randomUUID(),

    @Column(name = "source_topic", nullable = false)
    val sourceTopic: String = "",

    @Column(name = "event_id", nullable = false)
    val eventId: String = "",

    @Column(name = "event_type", nullable = false)
    val eventType: String = "",

    @Column(name = "consumer_name", nullable = false)
    val consumerName: String = "",

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    val payload: Map<String, Any?> = emptyMap(),

    @Column(name = "error_code")
    val errorCode: String? = null,

    @Column(name = "error_message")
    val errorMessage: String? = null,

    @Column(name = "failed_at", nullable = false)
    val failedAt: Instant = Instant.now(),
)
