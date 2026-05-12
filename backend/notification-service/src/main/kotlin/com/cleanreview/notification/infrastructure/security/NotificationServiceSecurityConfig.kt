package com.cleanreview.notification.infrastructure.security

import com.cleanreview.common.security.JwtAuthenticationConverter
import javax.crypto.spec.SecretKeySpec
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.web.SecurityFilterChain

enum class ServiceAccess {
    PUBLIC,
    ADMIN,
}

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
class NotificationServiceSecurityConfig {
    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain =
        http
            .csrf { it.disable() }
            .authorizeHttpRequests { requests ->
                requests
                    .requestMatchers("/actuator/health").permitAll()
                    .requestMatchers("/admin/api/v1/**").hasRole("ADMIN")
                    .anyRequest().authenticated()
            }
            .oauth2ResourceServer { oauth ->
                oauth.jwt { jwt ->
                    jwt.jwtAuthenticationConverter(JwtAuthenticationConverter())
                }
            }
            .build()

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
        fun pathRules(): Map<String, ServiceAccess> =
            linkedMapOf(
                "/actuator/health" to ServiceAccess.PUBLIC,
                "/admin/api/v1/**" to ServiceAccess.ADMIN,
            )
    }
}
