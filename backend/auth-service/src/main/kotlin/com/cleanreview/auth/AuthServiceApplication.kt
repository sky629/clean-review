package com.cleanreview.auth

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class AuthServiceApplication {
    companion object {
        const val serviceName = "auth-service"
    }
}

fun main(args: Array<String>) {
    runApplication<AuthServiceApplication>(*args)
}
