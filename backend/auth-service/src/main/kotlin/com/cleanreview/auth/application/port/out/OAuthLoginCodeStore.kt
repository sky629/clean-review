package com.cleanreview.auth.application.port.out

import com.cleanreview.common.security.UserRole

data class OAuthLoginCodePayload(
    val accessToken: String,
    val userId: String,
    val email: String,
    val role: UserRole,
)

interface OAuthLoginCodeStore {
    fun saveLoginCode(codeHash: String, payload: OAuthLoginCodePayload, ttlSeconds: Long)

    fun consumeLoginCode(codeHash: String): OAuthLoginCodePayload?
}
