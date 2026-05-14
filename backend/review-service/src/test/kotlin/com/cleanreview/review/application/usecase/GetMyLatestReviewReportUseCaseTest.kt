package com.cleanreview.review.application.usecase

import com.cleanreview.review.domain.model.CollectionRunId
import com.cleanreview.review.domain.model.ReviewReport
import com.cleanreview.review.domain.model.ReviewReportId
import com.cleanreview.review.domain.model.ReviewTarget
import com.cleanreview.review.domain.model.ReviewTargetId
import com.cleanreview.review.domain.model.ReviewTargetType
import com.cleanreview.review.domain.repository.ReviewReportRepository
import com.cleanreview.review.domain.repository.ReviewTargetRepository
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import org.junit.jupiter.api.Test

class GetMyLatestReviewReportUseCaseTest {
    @Test
    fun `owner can read latest report for own target`() {
        val ownerId = UUID.randomUUID()
        val targetRepository = ReportInMemoryReviewTargetRepository()
        val reportRepository = ReportInMemoryReviewReportRepository()
        val target = targetRepository.save(target(ownerId))
        val report = reportRepository.save(report(target.id))

        val result = GetMyLatestReviewReportUseCase(
            reviewTargetRepository = targetRepository,
            reviewReportRepository = reportRepository,
        ).execute(target.id, ownerId)

        assertEquals(report, result)
    }

    @Test
    fun `stranger cannot read report for another user's target`() {
        val targetRepository = ReportInMemoryReviewTargetRepository()
        val reportRepository = ReportInMemoryReviewReportRepository()
        val target = targetRepository.save(target(UUID.randomUUID()))

        assertFailsWith<ReviewTargetAccessDeniedException> {
            GetMyLatestReviewReportUseCase(
                reviewTargetRepository = targetRepository,
                reviewReportRepository = reportRepository,
            ).execute(target.id, UUID.randomUUID())
        }
    }

    private fun target(ownerId: UUID): ReviewTarget =
        ReviewTarget.create(
            id = ReviewTargetId(UUID.randomUUID()),
            createdBy = ownerId,
            keyword = "성수동 파스타 맛집",
            type = ReviewTargetType.PLACE,
        )

    private fun report(targetId: ReviewTargetId): ReviewReport =
        ReviewReport.create(
            id = ReviewReportId(UUID.randomUUID()),
            targetId = targetId,
            collectionRunId = CollectionRunId(UUID.randomUUID()),
            analyzerVersion = "viral-detector-0.1.0",
            modelProvider = "google",
            modelName = "gemini-2.5-flash",
            modelVersion = "gemini-2.5-flash",
            viralContaminationScore = 18.0,
            trustScore = 91.0,
            summary = "실사용 후기 중심입니다.",
            pros = listOf("구체적인 맛 평가"),
            cons = emptyList(),
            evidenceReviewIds = emptyList(),
            reportHash = "hash",
        )
}

private class ReportInMemoryReviewTargetRepository : ReviewTargetRepository {
    private val targets = linkedMapOf<ReviewTargetId, ReviewTarget>()

    override fun save(target: ReviewTarget): ReviewTarget {
        targets[target.id] = target
        return target
    }

    override fun findById(id: ReviewTargetId): ReviewTarget? = targets[id]

    override fun findAllByCreatedBy(userId: UUID): List<ReviewTarget> =
        targets.values.filter { it.createdBy == userId && !it.isDeleted() }

    override fun findActiveByCreatedByAndTypeAndKeyword(
        userId: UUID,
        type: com.cleanreview.review.domain.model.ReviewTargetType,
        keyword: String,
    ): ReviewTarget? =
        targets.values.firstOrNull {
            it.createdBy == userId &&
                it.type == type &&
                it.keyword.trim().equals(keyword.trim(), ignoreCase = true) &&
                !it.isDeleted()
        }

    override fun findAll(): List<ReviewTarget> =
        targets.values.filter { !it.isDeleted() }
}

private class ReportInMemoryReviewReportRepository : ReviewReportRepository {
    private val reports = mutableListOf<ReviewReport>()

    override fun save(report: ReviewReport): ReviewReport {
        reports.removeIf { it.id == report.id }
        reports.add(report)
        return report
    }

    override fun findLatestByTargetId(targetId: ReviewTargetId): ReviewReport? =
        reports.lastOrNull { it.targetId == targetId }
}
