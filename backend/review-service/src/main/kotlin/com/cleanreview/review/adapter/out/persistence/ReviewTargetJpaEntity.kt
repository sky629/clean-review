package com.cleanreview.review.adapter.out.persistence

import com.cleanreview.review.domain.model.ReviewTarget
import com.cleanreview.review.domain.model.ReviewTargetId
import com.cleanreview.review.domain.model.ReviewTargetStatus
import com.cleanreview.review.domain.model.ReviewTargetType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "review_targets")
class ReviewTargetJpaEntity(
    @Id
    @Column(name = "id", nullable = false)
    val id: UUID = UUID.randomUUID(),

    @Column(name = "created_by", nullable = false)
    val createdBy: UUID = UUID.randomUUID(),

    @Column(name = "keyword", nullable = false)
    val keyword: String = "",

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    val type: ReviewTargetType = ReviewTargetType.PLACE,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    val status: ReviewTargetStatus = ReviewTargetStatus.ACTIVE,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    val updatedAt: Instant = Instant.now(),
) {
    fun toDomain(): ReviewTarget =
        ReviewTarget(
            id = ReviewTargetId(id),
            createdBy = createdBy,
            keyword = keyword,
            type = type,
            status = status,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )

    companion object {
        fun from(target: ReviewTarget): ReviewTargetJpaEntity =
            ReviewTargetJpaEntity(
                id = target.id.value,
                createdBy = target.createdBy,
                keyword = target.keyword,
                type = target.type,
                status = target.status,
                createdAt = target.createdAt,
                updatedAt = target.updatedAt,
            )
    }
}
