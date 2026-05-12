package com.cleanreview.common.idempotency

import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test

class IdempotencyRecordTest {
    @Test
    fun `knows whether a processing lock has expired`() {
        val clock = Clock.fixed(Instant.parse("2026-05-07T12:00:00Z"), ZoneOffset.UTC)
        val key = IdempotencyKey(scope = "review-target:create", value = "client-key-1", ownerId = "user-1")

        val active = IdempotencyRecord.processing(
            key = key,
            requestHash = "hash",
            lockedUntil = Instant.parse("2026-05-07T12:00:30Z"),
        )
        val expired = active.copy(lockedUntil = Instant.parse("2026-05-07T11:59:59Z"))

        assertFalse(active.isLockExpired(clock))
        assertTrue(expired.isLockExpired(clock))
    }
}
