package com.cleanreview.auth.application.port.out

data class GoogleOAuthProfile(
    val providerUserId: String,
    val email: String,
    val displayName: String,
    val profileImageUrl: String?,
)

interface GoogleOAuthClient {
    fun fetchProfile(code: String, redirectUri: String): GoogleOAuthProfile
}
