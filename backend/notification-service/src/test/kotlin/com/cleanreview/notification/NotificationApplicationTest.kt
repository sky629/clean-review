package com.cleanreview.notification

import kotlin.test.assertEquals
import org.junit.jupiter.api.Test

class NotificationApplicationTest {
    @Test
    fun `declares notification service name`() {
        assertEquals("notification-service", NotificationApplication.serviceName)
    }
}
