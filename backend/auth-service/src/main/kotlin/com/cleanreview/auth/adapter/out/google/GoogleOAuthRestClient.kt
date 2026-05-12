package com.cleanreview.auth.adapter.out.google

import com.cleanreview.auth.application.port.out.GoogleOAuthClient
import com.cleanreview.auth.application.port.out.GoogleOAuthProfile
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Component
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.RestClient

@Component
class GoogleOAuthRestClient(
    @Value("\${clean-review.google.client-id}") private val clientId: String,
    @Value("\${clean-review.google.client-secret}") private val clientSecret: String,
) : GoogleOAuthClient {
    private val restClient = RestClient.create()
    private val mapType = object : ParameterizedTypeReference<Map<String, Any?>>() {}

    override fun fetchProfile(code: String, redirectUri: String): GoogleOAuthProfile {
        val tokenResponse = exchangeCode(code, redirectUri)
        val accessToken = tokenResponse["access_token"] as? String
            ?: error("Google token response did not include access_token.")
        val profile = fetchUserInfo(accessToken)

        return GoogleOAuthProfile(
            providerUserId = profile["sub"] as? String ?: error("Google profile did not include sub."),
            email = profile["email"] as? String ?: error("Google profile did not include email."),
            displayName = profile["name"] as? String ?: (profile["email"] as String),
            profileImageUrl = profile["picture"] as? String,
        )
    }

    private fun exchangeCode(code: String, redirectUri: String): Map<String, Any?> {
        val body = LinkedMultiValueMap<String, String>().apply {
            add("grant_type", "authorization_code")
            add("code", code)
            add("redirect_uri", redirectUri)
            add("client_id", clientId)
            add("client_secret", clientSecret)
        }

        return restClient.post()
            .uri("https://oauth2.googleapis.com/token")
            .body(body)
            .retrieve()
            .body(mapType)
            ?: emptyMap()
    }

    private fun fetchUserInfo(accessToken: String): Map<String, Any?> =
        restClient.get()
            .uri("https://openidconnect.googleapis.com/v1/userinfo")
            .header("Authorization", "Bearer $accessToken")
            .retrieve()
            .body(mapType)
            ?: emptyMap()
}
