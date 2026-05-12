package com.cleanreview.notification.adapter.`in`.web.admin

import com.cleanreview.common.security.AdminOnly
import com.cleanreview.notification.application.usecase.ListAdminNotificationDeliveriesUseCase
import com.cleanreview.notification.domain.model.NotificationDelivery
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

data class AdminNotificationDeliveryResponse(
    val id: String,
    val notificationType: String,
    val targetId: String?,
    val sourceEventId: String,
    val channel: String,
    val recipient: String,
    val status: String,
    val createdAt: String,
) {
    companion object {
        fun from(delivery: NotificationDelivery): AdminNotificationDeliveryResponse =
            AdminNotificationDeliveryResponse(
                id = delivery.id.value.toString(),
                notificationType = delivery.notificationType,
                targetId = delivery.targetId?.toString(),
                sourceEventId = delivery.sourceEventId,
                channel = delivery.channel.name,
                recipient = delivery.recipient,
                status = delivery.status.name,
                createdAt = delivery.createdAt.toString(),
            )
    }
}

@AdminOnly
@RestController
@RequestMapping("/admin/api/v1/notification-deliveries")
class AdminNotificationDeliveryController(
    private val listAdminNotificationDeliveriesUseCase: ListAdminNotificationDeliveriesUseCase,
) {
    @GetMapping
    fun list(): List<AdminNotificationDeliveryResponse> =
        listAdminNotificationDeliveriesUseCase.execute().map { AdminNotificationDeliveryResponse.from(it) }
}
