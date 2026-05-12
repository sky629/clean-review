package com.cleanreview.review.domain.repository

import com.cleanreview.review.domain.model.CollectionRun
import com.cleanreview.review.domain.model.CollectionRunId
import com.cleanreview.review.domain.model.ReviewTargetId

interface CollectionRunRepository {
    fun save(collectionRun: CollectionRun): CollectionRun

    fun findById(id: CollectionRunId): CollectionRun?

    fun findAll(): List<CollectionRun>

    fun findLatestCompletedByTargetIdAndSource(
        targetId: ReviewTargetId,
        source: String,
    ): CollectionRun?

    fun findLatestOpenByTargetIdAndSource(
        targetId: ReviewTargetId,
        source: String,
    ): CollectionRun?
}
