package com.cleanreview.auth.token

import kotlin.test.assertEquals
import org.junit.jupiter.api.Test

class RedisTokenKeyServiceTest {
    @Test
    fun `builds namespaced redis keys for refresh tokens and denylisted access tokens`() {
        val keyService = RedisTokenKeyService(prefix = "clean-review")

        assertEquals("refresh_token:token-hash-1", keyService.refreshTokenKey("token-hash-1"))
        assertEquals("user_refresh_tokens:user-1", keyService.userRefreshTokensKey("user-1"))
        assertEquals("oauth_login_code:code-hash-1", keyService.oauthLoginCodeKey("code-hash-1"))
        assertEquals("clean-review:auth:denylist:jwt-1", keyService.accessTokenDenylistKey("jwt-1"))
    }
}
