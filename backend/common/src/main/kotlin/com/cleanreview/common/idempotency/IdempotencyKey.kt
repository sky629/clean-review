package com.cleanreview.common.idempotency

data class IdempotencyKey(
    val scope: String,
    val value: String,
    val ownerId: String,
) {
    init {
        require(scope.isNotBlank()) { "Idempotency scope must not be blank." }
        require(value.isNotBlank()) { "Idempotency key value must not be blank." }
        require(ownerId.isNotBlank()) { "Idempotency owner must not be blank." }
    }
}
