package com.cleanreview.gateway

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class ApiGatewayApplication {
    companion object {
        const val serviceName = "api-gateway"
    }
}

fun main(args: Array<String>) {
    runApplication<ApiGatewayApplication>(*args)
}
