package com.cleanreview.review.domain.repository

import com.cleanreview.review.domain.model.ReviewTarget
import com.cleanreview.review.domain.model.ReviewTargetId
import java.util.UUID

interface ReviewTargetRepository {
    fun save(target: ReviewTarget): ReviewTarget

    fun findById(id: ReviewTargetId): ReviewTarget?

    fun findAllByCreatedBy(userId: UUID): List<ReviewTarget>

    fun findAll(): List<ReviewTarget>
}
