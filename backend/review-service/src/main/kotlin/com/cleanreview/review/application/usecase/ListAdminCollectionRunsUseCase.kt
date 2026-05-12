package com.cleanreview.review.application.usecase

import com.cleanreview.review.domain.model.CollectionRun
import com.cleanreview.review.domain.repository.CollectionRunRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ListAdminCollectionRunsUseCase(
    private val collectionRunRepository: CollectionRunRepository,
) {
    @Transactional(readOnly = true)
    fun execute(): List<CollectionRun> = collectionRunRepository.findAll()
}
