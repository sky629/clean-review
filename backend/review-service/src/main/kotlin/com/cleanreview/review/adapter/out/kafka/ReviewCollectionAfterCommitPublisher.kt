package com.cleanreview.review.adapter.out.kafka

import com.cleanreview.review.application.port.out.ReviewCollectionEventPublisher
import com.cleanreview.review.application.port.out.ReviewCollectionRequestedEvent
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class ReviewCollectionAfterCommitPublisher(
    private val reviewCollectionEventPublisher: ReviewCollectionEventPublisher,
) {
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun publishAfterCommit(event: ReviewCollectionRequestedEvent) {
        reviewCollectionEventPublisher.publish(event)
    }
}
