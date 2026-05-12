package com.cleanreview.common.idempotency

enum class IdempotencyStatus {
    PROCESSING,
    SUCCEEDED,
    FAILED,
}
