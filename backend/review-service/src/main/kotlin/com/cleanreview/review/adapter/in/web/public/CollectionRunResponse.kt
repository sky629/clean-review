package com.cleanreview.review.adapter.`in`.web.public

import com.cleanreview.review.domain.model.CollectionRun

data class CollectionRunResponse(
    val id: String,
    val targetId: String,
    val source: String,
    val keyword: String,
    val status: String,
    val runReason: String,
    val windowFrom: String,
    val windowTo: String,
    val maxReviews: Int,
) {
    companion object {
        fun from(collectionRun: CollectionRun): CollectionRunResponse =
            CollectionRunResponse(
                id = collectionRun.id.value.toString(),
                targetId = collectionRun.targetId.value.toString(),
                source = collectionRun.source,
                keyword = collectionRun.keyword,
                status = collectionRun.status.name,
                runReason = collectionRun.runReason.name,
                windowFrom = collectionRun.windowFrom.toString(),
                windowTo = collectionRun.windowTo.toString(),
                maxReviews = collectionRun.maxReviews,
            )
    }
}
