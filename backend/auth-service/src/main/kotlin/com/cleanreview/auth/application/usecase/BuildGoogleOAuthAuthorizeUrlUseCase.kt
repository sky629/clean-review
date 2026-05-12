package com.cleanreview.auth.application.usecase

import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

data class BuildGoogleOAuthAuthorizeUrlCommand(
    val redirectUri: String,
)

data class BuildGoogleOAuthAuthorizeUrlResult(
    val authorizationUrl: String,
)

@Service
class BuildGoogleOAuthAuthorizeUrlUseCase(
    @Value("\${clean-review.oauth.authorization-url:/oauth2/authorization/google}") private val authorizationUrl: String,
    @Value("\${clean-review.frontend.auth-success-uri:http://localhost:5173/oauth/google/callback}") private val authSuccessUri: String,
    @Value("\${clean-review.frontend.allowed-redirect-uris:}") private val allowedRedirectUris: String,
) {
    fun execute(command: BuildGoogleOAuthAuthorizeUrlCommand): BuildGoogleOAuthAuthorizeUrlResult {
        require(command.redirectUri.isNotBlank()) { "Redirect URI must not be blank." }
        require(isAllowedRedirectUri(command.redirectUri)) { "Redirect URI is not allowed." }

        val query = linkedMapOf(
            "redirect_uri" to command.redirectUri,
        ).entries.joinToString("&") { (key, value) ->
            "${encode(key)}=${encode(value)}"
        }
        return BuildGoogleOAuthAuthorizeUrlResult(
            authorizationUrl = "$authorizationUrl?$query",
        )
    }

    private fun encode(value: String): String =
        URLEncoder.encode(value, StandardCharsets.UTF_8)

    private fun isAllowedRedirectUri(redirectUri: String): Boolean {
        if (redirectUri == authSuccessUri) {
            return true
        }

        return allowedRedirectUris.split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .any { allowed -> redirectUri == allowed || redirectUri.startsWith("$allowed/") }
    }
}
