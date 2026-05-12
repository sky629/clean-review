package com.cleanreview.review.adapter.`in`.web.admin

import kotlin.test.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.web.bind.annotation.RequestMapping

class AdminReviewTargetControllerTest {
    @Test
    fun `admin review target controller uses admin api prefix`() {
        val mapping = AdminReviewTargetController::class.java.getAnnotation(RequestMapping::class.java)

        assertEquals("/admin/api/v1/review-targets", mapping.value.single())
    }
}

