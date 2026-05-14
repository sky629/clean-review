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
@Table(name = "retry_jobs")
class RetryJobJpaEntity(
    @Id
    @Column(name = "id", nullable = false)
    val id: UUID = UUID.randomUUID(),

    @Column(name = "topic", nullable = false)
    val topic: String = "",

    @Column(name = "event_type", nullable = false)
    val eventType: String = "",

    @Column(name = "original_event_id", nullable = false)
    val originalEventId: String = "",

    @Column(name = "idempotency_key", nullable = false)
    val idempotencyKey: String = "",

    @Column(name = "consumer_name", nullable = false)
    val consumerName: String = "",

    @Column(name = "correlation_id", nullable = false)
    val correlationId: String = "",

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    val payload: Map<String, Any?> = emptyMap(),

    @Column(name = "attempt", nullable = false)
    val attempt: Int = 0,

    @Column(name = "max_attempts", nullable = false)
    val maxAttempts: Int = 0,

    @Column(name = "next_attempt_at", nullable = false)
    val nextAttemptAt: Instant = Instant.now(),

    @Column(name = "last_error_code")
    val lastErrorCode: String? = null,

    @Column(name = "last_error_message")
    val lastErrorMessage: String? = null,

    @Column(name = "status", nullable = false)
    val status: String = "",

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    val updatedAt: Instant = Instant.now(),
)
