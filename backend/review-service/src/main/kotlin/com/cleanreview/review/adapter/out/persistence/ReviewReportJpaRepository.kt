package com.cleanreview.review.adapter.out.persistence

import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository

interface ReviewReportJpaRepository : JpaRepository<ReviewReportJpaEntity, UUID> {
    fun findFirstByTargetIdOrderByCreatedAtDesc(targetId: UUID): ReviewReportJpaEntity?
}
