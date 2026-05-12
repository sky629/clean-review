package com.cleanreview.common.event

import java.time.Instant

interface DomainEvent {
    val eventId: String
    val eventType: String
    val aggregateId: String
    val occurredAt: Instant
}
