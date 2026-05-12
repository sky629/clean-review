package com.cleanreview.review.adapter.out.persistence

import com.cleanreview.review.domain.model.CollectionRunId
import com.cleanreview.review.domain.model.ReviewReport
import com.cleanreview.review.domain.model.ReviewReportId
import com.cleanreview.review.domain.model.ReviewTargetId
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes

@Entity
@Table(name = "review_reports")
class ReviewReportJpaEntity(
    @Id
    @Column(name = "id", nullable = false)
    val id: UUID = UUID.randomUUID(),

    @Column(name = "target_id", nullable = false)
    val targetId: UUID = UUID.randomUUID(),

    @Column(name = "collection_run_id", nullable = false)
    val collectionRunId: UUID = UUID.randomUUID(),

    @Column(name = "analyzer_version", nullable = false)
    val analyzerVersion: String = "",

    @Column(name = "model_provider", nullable = false)
    val modelProvider: String = "",

    @Column(name = "model_name", nullable = false)
    val modelName: String = "",

    @Column(name = "model_version", nullable = false)
    val modelVersion: String = "",

    @Column(name = "viral_contamination_score", nullable = false)
    val viralContaminationScore: Double = 0.0,

    @Column(name = "trust_score", nullable = false)
    val trustScore: Double = 0.0,

    @Column(name = "summary", nullable = false)
    val summary: String = "",

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "pros", nullable = false, columnDefinition = "jsonb")
    val pros: List<String> = emptyList(),

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "cons", nullable = false, columnDefinition = "jsonb")
    val cons: List<String> = emptyList(),

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "evidence_review_ids", nullable = false, columnDefinition = "jsonb")
    val evidenceReviewIds: List<UUID> = emptyList(),

    @Column(name = "report_hash", nullable = false)
    val reportHash: String = "",

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),
) {
    fun toDomain(): ReviewReport =
        ReviewReport(
            id = ReviewReportId(id),
            targetId = ReviewTargetId(targetId),
            collectionRunId = CollectionRunId(collectionRunId),
            analyzerVersion = analyzerVersion,
            modelProvider = modelProvider,
            modelName = modelName,
            modelVersion = modelVersion,
            viralContaminationScore = viralContaminationScore,
            trustScore = trustScore,
            summary = summary,
            pros = pros,
            cons = cons,
            evidenceReviewIds = evidenceReviewIds,
            reportHash = reportHash,
            createdAt = createdAt,
        )

    companion object {
        fun from(report: ReviewReport): ReviewReportJpaEntity =
            ReviewReportJpaEntity(
                id = report.id.value,
                targetId = report.targetId.value,
                collectionRunId = report.collectionRunId.value,
                analyzerVersion = report.analyzerVersion,
                modelProvider = report.modelProvider,
                modelName = report.modelName,
                modelVersion = report.modelVersion,
                viralContaminationScore = report.viralContaminationScore,
                trustScore = report.trustScore,
                summary = report.summary,
                pros = report.pros,
                cons = report.cons,
                evidenceReviewIds = report.evidenceReviewIds,
                reportHash = report.reportHash,
                createdAt = report.createdAt,
            )
    }
}
