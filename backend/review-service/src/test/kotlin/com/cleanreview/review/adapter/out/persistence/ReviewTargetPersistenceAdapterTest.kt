package com.cleanreview.review.adapter.out.persistence

import com.cleanreview.review.domain.model.ReviewTarget
import com.cleanreview.review.domain.model.ReviewTargetId
import com.cleanreview.review.domain.model.ReviewTargetType
import java.util.UUID
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test

class ReviewTargetPersistenceAdapterTest {
    @Test
    fun `maps review target domain to jpa entity and back`() {
        val target = ReviewTarget.create(
            id = ReviewTargetId(UUID.randomUUID()),
            createdBy = UUID.randomUUID(),
            keyword = "성수동 파스타 맛집",
            type = ReviewTargetType.PLACE,
        )

        val entity = ReviewTargetJpaEntity.from(target)
        val mapped = entity.toDomain()

        assertEquals(target, mapped)
    }
}
