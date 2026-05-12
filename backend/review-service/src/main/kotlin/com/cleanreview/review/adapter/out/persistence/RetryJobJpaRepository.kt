package com.cleanreview.review.adapter.out.persistence

import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository

interface RetryJobJpaRepository : JpaRepository<RetryJobJpaEntity, UUID> {
    fun findAllByOrderByCreatedAtDesc(): List<RetryJobJpaEntity>
}
