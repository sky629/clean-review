package com.cleanreview.review.adapter.`in`.web.public

import com.cleanreview.common.security.AuthenticatedUser
import com.cleanreview.review.application.command.RegisterReviewTargetCommand
import com.cleanreview.review.application.usecase.DeleteReviewTargetUseCase
import com.cleanreview.review.application.usecase.GetMyLatestReviewReportUseCase
import com.cleanreview.review.application.usecase.ListMyReviewTargetsUseCase
import com.cleanreview.review.application.usecase.ListMyReviewTargetReviewsUseCase
import com.cleanreview.review.application.usecase.RegisterReviewTargetUseCase
import com.cleanreview.review.application.usecase.RequestReviewCollectionUseCase
import com.cleanreview.review.application.usecase.UpdateReviewTargetUseCase
import com.cleanreview.review.domain.model.ReviewTargetId
import com.cleanreview.review.domain.model.ReviewTargetType
import java.util.UUID
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/review-targets")
class ReviewTargetController(
    private val registerReviewTargetUseCase: RegisterReviewTargetUseCase,
    private val listMyReviewTargetsUseCase: ListMyReviewTargetsUseCase,
    private val deleteReviewTargetUseCase: DeleteReviewTargetUseCase,
    private val getMyLatestReviewReportUseCase: GetMyLatestReviewReportUseCase,
    private val updateReviewTargetUseCase: UpdateReviewTargetUseCase,
    private val listMyReviewTargetReviewsUseCase: ListMyReviewTargetReviewsUseCase,
    private val requestReviewCollectionUseCase: RequestReviewCollectionUseCase,
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @AuthenticationPrincipal user: AuthenticatedUser,
        @RequestBody request: CreateReviewTargetRequest,
    ): ReviewTargetResponse {
        val target = registerReviewTargetUseCase.execute(
            RegisterReviewTargetCommand(
                userId = UUID.fromString(user.userId),
                type = ReviewTargetType.valueOf(request.type),
                keyword = request.keyword,
            ),
        )

        return ReviewTargetResponse.from(target)
    }

    @GetMapping
    fun listMine(
        @AuthenticationPrincipal user: AuthenticatedUser,
    ): List<ReviewTargetResponse> =
        listMyReviewTargetsUseCase.execute(UUID.fromString(user.userId))
            .map { ReviewTargetResponse.from(it) }

    @DeleteMapping("/{reviewTargetId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(
        @AuthenticationPrincipal user: AuthenticatedUser,
        @PathVariable reviewTargetId: UUID,
    ) {
        deleteReviewTargetUseCase.execute(
            id = ReviewTargetId(reviewTargetId),
            userId = UUID.fromString(user.userId),
        )
    }

    @PutMapping("/{reviewTargetId}")
    fun update(
        @AuthenticationPrincipal user: AuthenticatedUser,
        @PathVariable reviewTargetId: UUID,
        @RequestBody request: UpdateReviewTargetRequest,
    ): ReviewTargetResponse =
        ReviewTargetResponse.from(
            updateReviewTargetUseCase.execute(
                id = ReviewTargetId(reviewTargetId),
                userId = UUID.fromString(user.userId),
                keyword = request.keyword,
                type = ReviewTargetType.valueOf(request.type),
            ),
        )

    @GetMapping("/{reviewTargetId}/report")
    fun getLatestReport(
        @AuthenticationPrincipal user: AuthenticatedUser,
        @PathVariable reviewTargetId: UUID,
    ): ReviewReportResponse =
        ReviewReportResponse.from(
            getMyLatestReviewReportUseCase.execute(
                targetId = ReviewTargetId(reviewTargetId),
                userId = UUID.fromString(user.userId),
            ),
        )

    @GetMapping("/{reviewTargetId}/reviews")
    fun listReviews(
        @AuthenticationPrincipal user: AuthenticatedUser,
        @PathVariable reviewTargetId: UUID,
    ): List<ReviewResponse> =
        listMyReviewTargetReviewsUseCase.execute(
            targetId = ReviewTargetId(reviewTargetId),
            userId = UUID.fromString(user.userId),
        ).map { ReviewResponse.from(it) }

    @PostMapping("/{reviewTargetId}/collection-runs")
    @ResponseStatus(HttpStatus.ACCEPTED)
    fun requestCollection(
        @AuthenticationPrincipal user: AuthenticatedUser,
        @PathVariable reviewTargetId: UUID,
    ): CollectionRunResponse =
        CollectionRunResponse.from(
            requestReviewCollectionUseCase.requestManualResync(
                targetId = ReviewTargetId(reviewTargetId),
                requestedBy = UUID.fromString(user.userId),
            ),
        )
}
