package com.cleanreview.gateway.config

import kotlin.test.assertContains
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test

class GatewayRouteConfigTest {
    @Test
    fun `declares msa routes with stable path ownership`() {
        val routes = GatewayRouteConfig.defaultRoutes()

        assertEquals("auth-service", routes["/api/v1/auth/**"])
        assertEquals("auth-service", routes["/oauth2/**"])
        assertEquals("auth-service", routes["/login/oauth2/**"])
        assertEquals("review-service", routes["/api/v1/review-targets/**"])
        assertEquals("review-service", routes["/admin/api/v1/review-targets/**"])
        assertEquals("review-service", routes["/admin/api/v1/collection-runs/**"])
        assertEquals("review-service", routes["/admin/api/v1/reviews/**"])
        assertEquals("review-service", routes["/admin/api/v1/analysis-runs/**"])
        assertEquals("review-service", routes["/admin/api/v1/retry-jobs/**"])
        assertEquals("review-service", routes["/admin/api/v1/dead-letters/**"])
        assertEquals("notification-service", routes["/admin/api/v1/notification-deliveries/**"])
    }

    @Test
    fun `admin notification route is explicit instead of falling through to review service`() {
        val routes = GatewayRouteConfig.defaultRoutes()

        assertContains(routes.keys, "/admin/api/v1/notification-deliveries/**")
        assertEquals("notification-service", routes["/admin/api/v1/notification-deliveries/**"])
    }
}
