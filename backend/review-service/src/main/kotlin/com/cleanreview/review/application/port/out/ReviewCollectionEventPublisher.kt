package com.cleanreview.review.application.port.out

data class ReviewCollectionRequestedEvent(
    val eventType: String = "review.collection.requested.v1",
    val collectionRunId: String,
    val targetId: String,
    val source: String,
    val keyword: String,
    val requestedBy: String?,
    val windowFrom: String,
    val windowTo: String,
    val maxReviews: Int,
    val runReason: String,
)

interface ReviewCollectionEventPublisher {
    fun publish(event: ReviewCollectionRequestedEvent)
}
