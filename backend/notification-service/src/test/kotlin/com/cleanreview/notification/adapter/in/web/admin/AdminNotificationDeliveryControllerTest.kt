package com.cleanreview.notification.adapter.`in`.web.admin

import kotlin.test.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.web.bind.annotation.RequestMapping

class AdminNotificationDeliveryControllerTest {
    @Test
    fun `admin notification delivery controller uses admin api prefix`() {
        val mapping = AdminNotificationDeliveryController::class.java.getAnnotation(RequestMapping::class.java)

        assertEquals("/admin/api/v1/notification-deliveries", mapping.value.single())
    }
}
