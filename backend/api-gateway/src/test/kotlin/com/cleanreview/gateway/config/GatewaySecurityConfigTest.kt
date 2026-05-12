package com.cleanreview.gateway.config

import kotlin.test.assertEquals
import org.junit.jupiter.api.Test

class GatewaySecurityConfigTest {
    @Test
    fun `declares public authenticated and admin path rules`() {
        val rules = GatewaySecurityConfig.pathRules()

        assertEquals(GatewayAccess.PUBLIC, rules["/actuator/health"])
        assertEquals(GatewayAccess.PUBLIC, rules["/api/v1/auth/**"])
        assertEquals(GatewayAccess.PUBLIC, rules["/oauth2/**"])
        assertEquals(GatewayAccess.PUBLIC, rules["/login/oauth2/**"])
        assertEquals(GatewayAccess.ADMIN, rules["/admin/api/v1/**"])
        assertEquals(GatewayAccess.AUTHENTICATED, rules["/api/v1/**"])
    }

    @Test
    fun `parses frontend allowed origins for browser login callbacks`() {
        val origins = GatewaySecurityConfig.corsAllowedOrigins("http://localhost:5173, https://clean-review.example")

        assertEquals(listOf("http://localhost:5173", "https://clean-review.example"), origins)
    }
}
