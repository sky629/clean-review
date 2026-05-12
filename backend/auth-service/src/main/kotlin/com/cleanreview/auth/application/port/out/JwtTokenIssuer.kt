package com.cleanreview.auth.application.port.out

import com.cleanreview.auth.domain.User

data class IssuedTokens(
    val accessToken: String,
    val refreshToken: String,
    val refreshTokenHash: String,
    val refreshTokenTtlSeconds: Long,
)

interface JwtTokenIssuer {
    fun issue(user: User): IssuedTokens
}
