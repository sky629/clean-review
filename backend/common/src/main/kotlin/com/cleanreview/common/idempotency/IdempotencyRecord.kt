package com.cleanreview.common.idempotency

import java.time.Clock
import java.time.Instant

data class IdempotencyRecord(
    val key: IdempotencyKey,
    val requestHash: String,
    val status: IdempotencyStatus,
    val lockedUntil: Instant,
) {
    fun isLockExpired(clock: Clock): Boolean = lockedUntil.isBefore(clock.instant())

    companion object {
        fun processing(
            key: IdempotencyKey,
            requestHash: String,
            lockedUntil: Instant,
        ): IdempotencyRecord = IdempotencyRecord(
            key = key,
            requestHash = requestHash,
            status = IdempotencyStatus.PROCESSING,
            lockedUntil = lockedUntil,
        )
    }
}
