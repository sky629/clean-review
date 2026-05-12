package com.cleanreview.gateway.config

import java.net.URI
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions.route
import org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions.http
import org.springframework.cloud.gateway.server.mvc.predicate.GatewayRequestPredicates.path
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.function.RouterFunction
import org.springframework.web.servlet.function.RouterFunctions
import org.springframework.web.servlet.function.ServerResponse

@ConfigurationProperties(prefix = "clean-review.routes")
data class GatewayRouteProperties(
    var authServiceUri: String = "http://localhost:8081",
    var reviewServiceUri: String = "http://localhost:8082",
    var notificationServiceUri: String = "http://localhost:8083",
)

@Configuration
@EnableConfigurationProperties(GatewayRouteProperties::class)
class GatewayRouteConfig(
    private val properties: GatewayRouteProperties,
) {
    @Bean
    fun gatewayRoutes(): RouterFunction<ServerResponse> {
        val builder: RouterFunctions.Builder = route("clean-review-gateway")

        routeDefinitions(properties).forEach { definition ->
            builder.route(path(definition.pathPattern), http(URI.create(definition.uri)))
        }

        return builder.build()
    }

    companion object {
        fun defaultRoutes(): Map<String, String> =
            routeDefinitions(GatewayRouteProperties()).associate { it.pathPattern to it.serviceName }

        private fun routeDefinitions(properties: GatewayRouteProperties): List<RouteDefinition> =
            listOf(
                RouteDefinition("/api/v1/auth/**", "auth-service", properties.authServiceUri),
                RouteDefinition("/oauth2/**", "auth-service", properties.authServiceUri),
                RouteDefinition("/login/oauth2/**", "auth-service", properties.authServiceUri),
                RouteDefinition("/admin/api/v1/notification-deliveries/**", "notification-service", properties.notificationServiceUri),
                RouteDefinition("/admin/api/v1/review-targets/**", "review-service", properties.reviewServiceUri),
                RouteDefinition("/admin/api/v1/collection-runs/**", "review-service", properties.reviewServiceUri),
                RouteDefinition("/admin/api/v1/reviews/**", "review-service", properties.reviewServiceUri),
                RouteDefinition("/admin/api/v1/analysis-runs/**", "review-service", properties.reviewServiceUri),
                RouteDefinition("/admin/api/v1/retry-jobs/**", "review-service", properties.reviewServiceUri),
                RouteDefinition("/admin/api/v1/dead-letters/**", "review-service", properties.reviewServiceUri),
                RouteDefinition("/api/v1/review-targets/**", "review-service", properties.reviewServiceUri),
            )
    }
}

private data class RouteDefinition(
    val pathPattern: String,
    val serviceName: String,
    val uri: String,
)
