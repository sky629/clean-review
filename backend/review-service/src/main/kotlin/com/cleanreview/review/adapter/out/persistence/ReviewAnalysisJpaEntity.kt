package com.cleanreview.review.adapter.out.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes

@Entity
@Table(name = "review_analysis")
class ReviewAnalysisJpaEntity(
    @Id
    @Column(name = "id", nullable = false)
    val id: UUID = UUID.randomUUID(),

    @Column(name = "review_id", nullable = false)
    val reviewId: UUID = UUID.randomUUID(),

    @Column(name = "collection_run_id")
    val collectionRunId: UUID? = null,

    @Column(name = "analyzer_version", nullable = false)
    val analyzerVersion: String = "",

    @Column(name = "model_provider", nullable = false)
    val modelProvider: String = "",

    @Column(name = "model_name", nullable = false)
    val modelName: String = "",

    @Column(name = "model_version", nullable = false)
    val modelVersion: String = "",

    @Column(name = "viral_score", nullable = false)
    val viralScore: BigDecimal = BigDecimal.ZERO,

    @Column(name = "quality_score", nullable = false)
    val qualityScore: BigDecimal = BigDecimal.ZERO,

    @Column(name = "is_suspicious", nullable = false)
    val isSuspicious: Boolean = false,

    @Column(name = "useful_for_report", nullable = false)
    val usefulForReport: Boolean = true,

    @Column(name = "summary", nullable = false)
    val summary: String = "",

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "detected_patterns", nullable = false, columnDefinition = "jsonb")
    val detectedPatterns: List<String> = emptyList(),

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "evidence", nullable = false, columnDefinition = "jsonb")
    val evidence: List<String> = emptyList(),

    @Column(name = "analyzed_at", nullable = false)
    val analyzedAt: Instant = Instant.now(),

    @Column(name = "status", nullable = false)
    val status: String = "",
)
