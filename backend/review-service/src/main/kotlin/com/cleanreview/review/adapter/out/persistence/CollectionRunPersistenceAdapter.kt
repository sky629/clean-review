package com.cleanreview.review.adapter.out.persistence

import com.cleanreview.review.domain.model.CollectionRun
import com.cleanreview.review.domain.model.CollectionRunId
import com.cleanreview.review.domain.model.CollectionRunStatus
import com.cleanreview.review.domain.model.ReviewTargetId
import com.cleanreview.review.domain.repository.CollectionRunRepository
import org.springframework.stereotype.Repository

@Repository
class CollectionRunPersistenceAdapter(
    private val collectionRunJpaRepository: CollectionRunJpaRepository,
) : CollectionRunRepository {
    override fun save(collectionRun: CollectionRun): CollectionRun =
        collectionRunJpaRepository.save(CollectionRunJpaEntity.from(collectionRun)).toDomain()

    override fun findById(id: CollectionRunId): CollectionRun? =
        collectionRunJpaRepository.findById(id.value).map { it.toDomain() }.orElse(null)

    override fun findAll(): List<CollectionRun> =
        collectionRunJpaRepository.findAll().map { it.toDomain() }

    override fun findLatestCompletedByTargetIdAndSource(
        targetId: ReviewTargetId,
        source: String,
    ): CollectionRun? =
        collectionRunJpaRepository
            .findFirstByTargetIdAndSourceAndStatusOrderByCompletedAtDesc(
                targetId.value,
                source,
                CollectionRunStatus.COMPLETED,
            )
            ?.toDomain()

    override fun findLatestOpenByTargetIdAndSource(
        targetId: ReviewTargetId,
        source: String,
    ): CollectionRun? =
        collectionRunJpaRepository
            .findFirstByTargetIdAndSourceAndStatusInOrderByRequestedAtDesc(
                targetId.value,
                source,
                listOf(CollectionRunStatus.REQUESTED, CollectionRunStatus.RUNNING),
            )
            ?.toDomain()
}
