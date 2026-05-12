package com.cleanreview.auth.adapter.out.redis

import com.cleanreview.auth.application.port.out.RefreshTokenStore
import com.cleanreview.auth.token.RedisTokenKeyService
import java.time.Duration
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component

@Component
class RedisRefreshTokenStore(
    private val redisTemplate: StringRedisTemplate,
    private val redisTokenKeyService: RedisTokenKeyService,
) : RefreshTokenStore {
    override fun saveRefreshToken(tokenHash: String, userId: String, ttlSeconds: Long) {
        val ttl = Duration.ofSeconds(ttlSeconds)
        redisTemplate.opsForValue().set(redisTokenKeyService.refreshTokenKey(tokenHash), userId, ttl)
        redisTemplate.opsForSet().add(redisTokenKeyService.userRefreshTokensKey(userId), tokenHash)
        redisTemplate.expire(redisTokenKeyService.userRefreshTokensKey(userId), ttl)
    }

    override fun findUserIdByRefreshTokenHash(tokenHash: String): String? =
        redisTemplate.opsForValue().get(redisTokenKeyService.refreshTokenKey(tokenHash))

    override fun deleteRefreshToken(tokenHash: String) {
        val key = redisTokenKeyService.refreshTokenKey(tokenHash)
        val userId = redisTemplate.opsForValue().get(key)
        redisTemplate.delete(key)
        if (userId != null) {
            redisTemplate.opsForSet().remove(redisTokenKeyService.userRefreshTokensKey(userId), tokenHash)
        }
    }
}
