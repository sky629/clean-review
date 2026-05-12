package com.cleanreview.notification.application.usecase

import com.cleanreview.notification.domain.model.NotificationDelivery
import com.cleanreview.notification.domain.repository.NotificationDeliveryRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ListAdminNotificationDeliveriesUseCase(
    private val notificationDeliveryRepository: NotificationDeliveryRepository,
) {
    @Transactional(readOnly = true)
    fun execute(): List<NotificationDelivery> = notificationDeliveryRepository.findAll()
}
