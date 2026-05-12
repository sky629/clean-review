package com.cleanreview.auth.infrastructure.security

import kotlin.test.assertEquals
import org.junit.jupiter.api.Test

class AuthServiceSecurityConfigTest {
    @Test
    fun `declares oauth login endpoint as public`() {
        val rules = AuthServiceSecurityConfig.pathRules()

        assertEquals(ServiceAccess.PUBLIC, rules["/actuator/health"])
        assertEquals(ServiceAccess.PUBLIC, rules["/api/v1/auth/**"])
        assertEquals(ServiceAccess.PUBLIC, rules["/oauth2/**"])
        assertEquals(ServiceAccess.PUBLIC, rules["/login/oauth2/**"])
    }
}
