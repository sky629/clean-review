package com.cleanreview.review.adapter.out.persistence

import com.cleanreview.review.domain.model.CollectionRunId
import com.cleanreview.review.domain.model.ReviewReport
import com.cleanreview.review.domain.model.ReviewReportId
import com.cleanreview.review.domain.model.ReviewTargetId
import java.util.UUID
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test

class ReviewReportPersistenceAdapterTest {
    @Test
    fun `maps review report domain to jpa entity and back`() {
        val report = ReviewReport.create(
            id = ReviewReportId(UUID.randomUUID()),
            targetId = ReviewTargetId(UUID.randomUUID()),
            collectionRunId = CollectionRunId(UUID.randomUUID()),
            analyzerVersion = "viral-detector-0.1.0",
            modelProvider = "google",
            modelName = "gemini-2.5-flash",
            modelVersion = "gemini-2.5-flash",
            viralContaminationScore = 12.0,
            trustScore = 88.0,
            summary = "구체적인 경험이 충분합니다.",
            pros = listOf("청결도 언급"),
            cons = listOf("가격 불만"),
            evidenceReviewIds = listOf(UUID.randomUUID()),
            reportHash = "hash",
        )

        val entity = ReviewReportJpaEntity.from(report)
        val mapped = entity.toDomain()

        assertEquals(report, mapped)
    }
}
