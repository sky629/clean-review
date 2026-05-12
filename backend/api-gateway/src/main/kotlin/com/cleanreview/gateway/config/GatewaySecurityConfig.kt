package com.cleanreview.gateway.config

import com.cleanreview.common.security.JwtAuthenticationConverter
import javax.crypto.spec.SecretKeySpec
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

enum class GatewayAccess {
    PUBLIC,
    AUTHENTICATED,
    ADMIN,
}

@Configuration
@EnableWebSecurity
class GatewaySecurityConfig {
    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain =
        http
            .cors { }
            .csrf { it.disable() }
            .authorizeHttpRequests { requests ->
                requests
                    .requestMatchers("/actuator/health", "/api/v1/auth/**", "/oauth2/**", "/login/oauth2/**").permitAll()
                    .requestMatchers("/admin/api/v1/**").hasRole("ADMIN")
                    .requestMatchers("/api/v1/**").authenticated()
                    .anyRequest().authenticated()
            }
            .oauth2ResourceServer { oauth ->
                oauth.jwt { jwt ->
                    jwt.jwtAuthenticationConverter(JwtAuthenticationConverter())
                }
            }
            .build()

    @Bean
    fun corsConfigurationSource(
        @Value("\${clean-review.cors.allowed-origins}") allowedOrigins: String,
    ): CorsConfigurationSource {
        val configuration =
            CorsConfiguration().apply {
                this.allowedOrigins = corsAllowedOrigins(allowedOrigins)
                allowedMethods = listOf("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                allowedHeaders = listOf("Authorization", "Content-Type")
                allowCredentials = true
            }
        return UrlBasedCorsConfigurationSource().apply {
            registerCorsConfiguration("/**", configuration)
        }
    }

    @Bean
    fun jwtDecoder(
        @Value("\${clean-review.jwt.secret}") secret: String,
    ): JwtDecoder {
        val key = SecretKeySpec(secret.toByteArray(), "HmacSHA256")
        return NimbusJwtDecoder.withSecretKey(key)
            .macAlgorithm(MacAlgorithm.HS256)
            .build()
    }

    companion object {
        fun pathRules(): Map<String, GatewayAccess> =
            linkedMapOf(
                "/actuator/health" to GatewayAccess.PUBLIC,
                "/api/v1/auth/**" to GatewayAccess.PUBLIC,
                "/oauth2/**" to GatewayAccess.PUBLIC,
                "/login/oauth2/**" to GatewayAccess.PUBLIC,
                "/admin/api/v1/**" to GatewayAccess.ADMIN,
                "/api/v1/**" to GatewayAccess.AUTHENTICATED,
            )

        fun corsAllowedOrigins(value: String): List<String> =
            value.split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
    }
}
