package com.cleanreview.review.domain.model

import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test

class ReviewTargetTest {
    @Test
    fun `creates active review target owned by requester`() {
        val ownerId = UUID.randomUUID()
        val target = ReviewTarget.create(
            id = ReviewTargetId(UUID.randomUUID()),
            createdBy = ownerId,
            keyword = "성수동 파스타 맛집",
            type = ReviewTargetType.PLACE,
        )

        assertEquals(ReviewTargetStatus.ACTIVE, target.status)
        assertEquals("성수동 파스타 맛집", target.keyword)
        assertTrue(target.isOwnedBy(ownerId))
        assertFalse(target.isDeleted())
    }

    @Test
    fun `marks target as deleted without changing original owner`() {
        val ownerId = UUID.randomUUID()
        val target = ReviewTarget.create(
            id = ReviewTargetId(UUID.randomUUID()),
            createdBy = ownerId,
            keyword = "무선 청소기 실사용 후기",
            type = ReviewTargetType.PRODUCT,
        )

        val deleted = target.delete()

        assertEquals(ownerId, deleted.createdBy)
        assertEquals(ReviewTargetStatus.DELETED, deleted.status)
        assertTrue(deleted.isDeleted())
    }
}
