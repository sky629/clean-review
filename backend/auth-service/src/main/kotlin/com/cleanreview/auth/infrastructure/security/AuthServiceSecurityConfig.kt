package com.cleanreview.auth.infrastructure.security

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.web.SecurityFilterChain

enum class ServiceAccess {
    PUBLIC,
}

@Configuration
@EnableWebSecurity
class AuthServiceSecurityConfig(
    private val oauth2LoginSuccessHandler: OAuth2LoginSuccessHandler,
) {
    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain =
        http
            .csrf { it.disable() }
            .authorizeHttpRequests { requests ->
                requests
                    .requestMatchers("/actuator/health", "/api/v1/auth/**", "/oauth2/**", "/login/oauth2/**").permitAll()
                    .anyRequest().authenticated()
            }
            .oauth2Login { oauth2 ->
                oauth2.successHandler(oauth2LoginSuccessHandler)
            }
            .build()

    companion object {
        fun pathRules(): Map<String, ServiceAccess> =
            linkedMapOf(
                "/actuator/health" to ServiceAccess.PUBLIC,
                "/api/v1/auth/**" to ServiceAccess.PUBLIC,
                "/oauth2/**" to ServiceAccess.PUBLIC,
                "/login/oauth2/**" to ServiceAccess.PUBLIC,
            )
    }
}
