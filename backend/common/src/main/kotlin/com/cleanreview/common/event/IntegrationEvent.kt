package com.cleanreview.common.event

import java.time.Instant

data class IntegrationEvent(
    override val eventId: String,
    override val eventType: String,
    override val aggregateId: String,
    override val occurredAt: Instant,
    val payload: Map<String, Any?> = emptyMap(),
) : DomainEvent {
    fun isEmptyPayload(): Boolean = payload.isEmpty()
}
