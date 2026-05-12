package com.cleanreview.review.domain.model

import java.util.UUID
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test

class ReviewReportTest {
    @Test
    fun `creates report for completed analysis result`() {
        val targetId = ReviewTargetId(UUID.randomUUID())
        val collectionRunId = CollectionRunId(UUID.randomUUID())

        val report = ReviewReport.create(
            id = ReviewReportId(UUID.randomUUID()),
            targetId = targetId,
            collectionRunId = collectionRunId,
            analyzerVersion = "viral-detector-0.1.0",
            modelProvider = "google",
            modelName = "gemini-2.5-flash",
            modelVersion = "gemini-2.5-flash",
            viralContaminationScore = 37.5,
            trustScore = 82.0,
            summary = "광고성 문구는 일부 있으나 구체적인 방문 경험이 많습니다.",
            pros = listOf("청결 언급이 반복됨"),
            cons = listOf("웨이팅 불만이 있음"),
            evidenceReviewIds = listOf(UUID.randomUUID()),
            reportHash = "report-hash",
        )

        assertEquals(targetId, report.targetId)
        assertEquals(collectionRunId, report.collectionRunId)
        assertEquals("gemini-2.5-flash", report.modelName)
        assertEquals(82.0, report.trustScore)
    }
}
