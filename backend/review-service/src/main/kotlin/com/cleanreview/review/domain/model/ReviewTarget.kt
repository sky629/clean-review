package com.cleanreview.review.domain.model

import java.time.Instant
import java.util.UUID

data class ReviewTarget(
    val id: ReviewTargetId,
    val createdBy: UUID,
    val keyword: String,
    val type: ReviewTargetType,
    val status: ReviewTargetStatus,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    fun isOwnedBy(userId: UUID): Boolean = createdBy == userId

    fun isDeleted(): Boolean = status == ReviewTargetStatus.DELETED

    fun delete(now: Instant = Instant.now()): ReviewTarget =
        copy(status = ReviewTargetStatus.DELETED, updatedAt = now)

    fun updateDetails(
        keyword: String,
        type: ReviewTargetType,
        now: Instant = Instant.now(),
    ): ReviewTarget {
        require(keyword.isNotBlank()) { "Review target keyword must not be blank." }
        return copy(
            keyword = keyword.trim(),
            type = type,
            updatedAt = now,
        )
    }

    companion object {
        fun create(
            id: ReviewTargetId,
            createdBy: UUID,
            keyword: String,
            type: ReviewTargetType,
        ): ReviewTarget {
            require(keyword.isNotBlank()) { "Review target keyword must not be blank." }

            val now = Instant.now()

            return ReviewTarget(
                id = id,
                createdBy = createdBy,
                keyword = keyword.trim(),
                type = type,
                status = ReviewTargetStatus.ACTIVE,
                createdAt = now,
                updatedAt = now,
            )
        }
    }
}
