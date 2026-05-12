package com.cleanreview.auth.adapter.out.redis

import com.cleanreview.auth.application.port.out.OAuthLoginCodePayload
import com.cleanreview.auth.application.port.out.OAuthLoginCodeStore
import com.cleanreview.auth.token.RedisTokenKeyService
import com.cleanreview.common.security.UserRole
import java.time.Duration
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component

@Component
class RedisOAuthLoginCodeStore(
    private val redisTemplate: StringRedisTemplate,
    private val redisTokenKeyService: RedisTokenKeyService,
) : OAuthLoginCodeStore {
    override fun saveLoginCode(codeHash: String, payload: OAuthLoginCodePayload, ttlSeconds: Long) {
        redisTemplate.opsForValue().set(
            redisTokenKeyService.oauthLoginCodeKey(codeHash),
            payload.serialize(),
            Duration.ofSeconds(ttlSeconds),
        )
    }

    override fun consumeLoginCode(codeHash: String): OAuthLoginCodePayload? {
        val key = redisTokenKeyService.oauthLoginCodeKey(codeHash)
        val raw = redisTemplate.opsForValue().getAndDelete(key) ?: return null
        return raw.deserializePayload()
    }

    private fun OAuthLoginCodePayload.serialize(): String =
        listOf(accessToken, userId, email, role.name).joinToString("\n")

    private fun String.deserializePayload(): OAuthLoginCodePayload {
        val parts = split("\n", limit = 4)
        require(parts.size == 4) { "Invalid OAuth login code payload." }
        return OAuthLoginCodePayload(
            accessToken = parts[0],
            userId = parts[1],
            email = parts[2],
            role = UserRole.valueOf(parts[3]),
        )
    }
}
