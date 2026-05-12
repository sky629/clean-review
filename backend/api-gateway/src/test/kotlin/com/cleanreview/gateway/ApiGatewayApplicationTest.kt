package com.cleanreview.gateway

import kotlin.test.assertEquals
import org.junit.jupiter.api.Test

class ApiGatewayApplicationTest {
    @Test
    fun `declares api gateway service name`() {
        assertEquals("api-gateway", ApiGatewayApplication.serviceName)
    }
}
