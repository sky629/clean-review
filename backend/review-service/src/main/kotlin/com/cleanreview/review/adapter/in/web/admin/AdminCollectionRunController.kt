package com.cleanreview.review.adapter.`in`.web.admin

import com.cleanreview.common.security.AdminOnly
import com.cleanreview.review.application.usecase.ListAdminCollectionRunsUseCase
import com.cleanreview.review.domain.model.CollectionRun
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

data class AdminCollectionRunResponse(
    val id: String,
    val targetId: String,
    val source: String,
    val keyword: String,
    val status: String,
    val requestedAt: String,
    val failureCode: String?,
    val failureMessage: String?,
) {
    companion object {
        fun from(collectionRun: CollectionRun): AdminCollectionRunResponse =
            AdminCollectionRunResponse(
                id = collectionRun.id.value.toString(),
                targetId = collectionRun.targetId.value.toString(),
                source = collectionRun.source,
                keyword = collectionRun.keyword,
                status = collectionRun.status.name,
                requestedAt = collectionRun.requestedAt.toString(),
                failureCode = collectionRun.failureCode,
                failureMessage = collectionRun.failureMessage,
            )
    }
}

@AdminOnly
@RestController
@RequestMapping("/admin/api/v1/collection-runs")
class AdminCollectionRunController(
    private val listAdminCollectionRunsUseCase: ListAdminCollectionRunsUseCase,
) {
    @GetMapping
    fun list(): List<AdminCollectionRunResponse> =
        listAdminCollectionRunsUseCase.execute().map { AdminCollectionRunResponse.from(it) }
}
