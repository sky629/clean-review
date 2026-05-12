package com.cleanreview.review.adapter.`in`.web.admin

import com.cleanreview.common.security.AdminOnly
import com.cleanreview.review.application.usecase.ListAdminReviewTargetsUseCase
import com.cleanreview.review.domain.model.ReviewTarget
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

data class AdminReviewTargetSummaryResponse(
    val id: String,
    val createdBy: String,
    val type: String,
    val keyword: String,
    val status: String,
) {
    companion object {
        fun from(target: ReviewTarget): AdminReviewTargetSummaryResponse =
            AdminReviewTargetSummaryResponse(
                id = target.id.value.toString(),
                createdBy = target.createdBy.toString(),
                type = target.type.name,
                keyword = target.keyword,
                status = target.status.name,
            )
    }
}

@AdminOnly
@RestController
@RequestMapping("/admin/api/v1/review-targets")
class AdminReviewTargetController(
    private val listAdminReviewTargetsUseCase: ListAdminReviewTargetsUseCase,
) {
    @GetMapping
    fun list(): List<AdminReviewTargetSummaryResponse> =
        listAdminReviewTargetsUseCase.execute().map { AdminReviewTargetSummaryResponse.from(it) }
}
