package com.cleanreview.notification.infrastructure.security

import kotlin.test.assertEquals
import org.junit.jupiter.api.Test

class NotificationServiceSecurityConfigTest {
    @Test
    fun `declares public health and admin api path rules`() {
        val rules = NotificationServiceSecurityConfig.pathRules()

        assertEquals(ServiceAccess.PUBLIC, rules["/actuator/health"])
        assertEquals(ServiceAccess.ADMIN, rules["/admin/api/v1/**"])
    }
}
