package com.cleanreview.common.security

import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.security.oauth2.jwt.Jwt

class JwtAuthenticationConverterTest {
    @Test
    fun `converts jwt claims into authenticated user principal and authorities`() {
        val jwt = Jwt.withTokenValue("token")
            .header("alg", "none")
            .subject("user-123")
            .claim("email", "owner@example.com")
            .claim("roles", listOf("ADMIN", "USER"))
            .claim("scope", "reviews.read notifications.write")
            .build()

        val authentication = JwtAuthenticationConverter().convert(jwt)

        val principal = assertIs<AuthenticatedUser>(authentication.principal)
        assertEquals("user-123", principal.userId)
        assertEquals("owner@example.com", principal.email)
        assertTrue(principal.hasRole(UserRole.ADMIN))
        assertEquals(
            setOf("ROLE_ADMIN", "ROLE_USER", "SCOPE_reviews.read", "SCOPE_notifications.write"),
            authentication.authorities.map { it.authority }.toSet(),
        )
    }
}
