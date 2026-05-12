package com.cleanreview.auth.token

import org.springframework.stereotype.Service

@Service
class RedisTokenKeyService(
    private val prefix: String = "clean-review",
) {
    fun refreshTokenKey(tokenHash: String): String = "refresh_token:$tokenHash"

    fun userRefreshTokensKey(userId: String): String = "user_refresh_tokens:$userId"

    fun oauthLoginCodeKey(codeHash: String): String = "oauth_login_code:$codeHash"

    fun accessTokenDenylistKey(jwtId: String): String = "$prefix:auth:denylist:$jwtId"
}
