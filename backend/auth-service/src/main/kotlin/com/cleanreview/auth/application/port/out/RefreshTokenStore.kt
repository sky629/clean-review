package com.cleanreview.auth.application.port.out

interface RefreshTokenStore {
    fun saveRefreshToken(tokenHash: String, userId: String, ttlSeconds: Long)

    fun findUserIdByRefreshTokenHash(tokenHash: String): String?

    fun deleteRefreshToken(tokenHash: String)
}
