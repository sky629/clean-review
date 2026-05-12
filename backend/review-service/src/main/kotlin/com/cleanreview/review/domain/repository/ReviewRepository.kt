package com.cleanreview.review.domain.repository

import com.cleanreview.review.domain.model.Review
import com.cleanreview.review.domain.model.ReviewTargetId

interface ReviewRepository {
    fun findAllByTargetId(targetId: ReviewTargetId): List<Review>
}
