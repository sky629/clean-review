package com.cleanreview.review.adapter.`in`.web.public

import kotlin.test.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping

class ReviewTargetControllerTest {
    @Test
    fun `public review target controller uses public api prefix`() {
        val mapping = ReviewTargetController::class.java.getAnnotation(RequestMapping::class.java)

        assertEquals("/api/v1/review-targets", mapping.value.single())
    }

    @Test
    fun `public review target controller exposes update endpoint`() {
        val method = ReviewTargetController::class.java.getDeclaredMethod(
            "update",
            com.cleanreview.common.security.AuthenticatedUser::class.java,
            java.util.UUID::class.java,
            UpdateReviewTargetRequest::class.java,
        )
        val mapping = method.getAnnotation(PutMapping::class.java)

        assertEquals("/{reviewTargetId}", mapping.value.single())
    }

    @Test
    fun `public review target controller exposes reviews endpoint`() {
        val method = ReviewTargetController::class.java.getDeclaredMethod(
            "listReviews",
            com.cleanreview.common.security.AuthenticatedUser::class.java,
            java.util.UUID::class.java,
        )
        val mapping = method.getAnnotation(GetMapping::class.java)

        assertEquals("/{reviewTargetId}/reviews", mapping.value.single())
    }

    @Test
    fun `public review target controller exposes manual collection endpoint`() {
        val method = ReviewTargetController::class.java.getDeclaredMethod(
            "requestCollection",
            com.cleanreview.common.security.AuthenticatedUser::class.java,
            java.util.UUID::class.java,
        )
        val mapping = method.getAnnotation(PostMapping::class.java)

        assertEquals("/{reviewTargetId}/collection-runs", mapping.value.single())
    }
}
