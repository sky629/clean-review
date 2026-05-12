package com.cleanreview.review.application.command

import com.cleanreview.review.domain.model.ReviewTargetType
import java.util.UUID

data class RegisterReviewTargetCommand(
    val userId: UUID,
    val type: ReviewTargetType,
    val keyword: String,
)
