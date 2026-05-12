package com.cleanreview.notification

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class NotificationApplication {
    companion object {
        const val serviceName = "notification-service"
    }
}

fun main(args: Array<String>) {
    runApplication<NotificationApplication>(*args)
}
