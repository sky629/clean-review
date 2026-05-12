package com.cleanreview.review.adapter.`in`.web.public

import com.cleanreview.review.domain.model.ReviewTarget

data class ReviewTargetResponse(
    val id: String,
    val createdBy: String,
    val type: String,
    val keyword: String,
    val status: String,
) {
    companion object {
        fun from(target: ReviewTarget): ReviewTargetResponse =
            ReviewTargetResponse(
                id = target.id.value.toString(),
                createdBy = target.createdBy.toString(),
                type = target.type.name,
                keyword = target.keyword,
                status = target.status.name,
            )
    }
}
