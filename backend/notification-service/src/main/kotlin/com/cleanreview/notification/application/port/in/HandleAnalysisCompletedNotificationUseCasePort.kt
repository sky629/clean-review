package com.cleanreview.notification.application.port.`in`

import com.cleanreview.notification.application.usecase.AnalysisCompletedNotificationCommand

interface HandleAnalysisCompletedNotificationUseCasePort {
    fun execute(command: AnalysisCompletedNotificationCommand)
}
