package com.cleanreview.review.domain.repository

import com.cleanreview.review.domain.model.ReviewReport
import com.cleanreview.review.domain.model.ReviewTargetId

interface ReviewReportRepository {
    fun save(report: ReviewReport): ReviewReport

    fun findLatestByTargetId(targetId: ReviewTargetId): ReviewReport?
}
