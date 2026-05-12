package com.cleanreview.auth.application.usecase

import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test

class BuildGoogleOAuthAuthorizeUrlUseCaseTest {
    @Test
    fun `builds spring oauth authorization url for allowed redirect uri`() {
        val useCase = BuildGoogleOAuthAuthorizeUrlUseCase(
            authorizationUrl = "http://localhost:8080/oauth2/authorization/google",
            authSuccessUri = "http://localhost:5173/oauth/google/callback",
            allowedRedirectUris = "",
        )

        val result = useCase.execute(
            BuildGoogleOAuthAuthorizeUrlCommand("http://localhost:5173/oauth/google/callback"),
        )

        assertTrue(result.authorizationUrl.startsWith("http://localhost:8080/oauth2/authorization/google?"))
    }

    @Test
    fun `rejects untrusted redirect uri`() {
        val useCase = BuildGoogleOAuthAuthorizeUrlUseCase(
            authorizationUrl = "http://localhost:8080/oauth2/authorization/google",
            authSuccessUri = "http://localhost:5173/oauth/google/callback",
            allowedRedirectUris = "",
        )

        assertFailsWith<IllegalArgumentException> {
            useCase.execute(BuildGoogleOAuthAuthorizeUrlCommand("https://evil.example/callback"))
        }
    }
}
