package com.cleanreview.review.adapter.out.persistence

import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository

interface DeadLetterEventJpaRepository : JpaRepository<DeadLetterEventJpaEntity, UUID> {
    fun findAllByOrderByFailedAtDesc(): List<DeadLetterEventJpaEntity>
}
