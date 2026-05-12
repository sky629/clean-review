package com.cleanreview.review

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class ReviewServiceApplication {
    companion object {
        const val serviceName = "review-service"
    }
}

fun main(args: Array<String>) {
    runApplication<ReviewServiceApplication>(*args)
}
