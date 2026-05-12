package com.cleanreview.review.infrastructure.security

import kotlin.test.assertEquals
import org.junit.jupiter.api.Test

class ReviewServiceSecurityConfigTest {
    @Test
    fun `declares public authenticated and admin path rules`() {
        val rules = ReviewServiceSecurityConfig.pathRules()

        assertEquals(ServiceAccess.PUBLIC, rules["/actuator/health"])
        assertEquals(ServiceAccess.ADMIN, rules["/admin/api/v1/**"])
        assertEquals(ServiceAccess.AUTHENTICATED, rules["/api/v1/**"])
    }
}
