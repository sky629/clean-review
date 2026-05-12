package com.cleanreview.auth.application.usecase

import com.cleanreview.auth.application.port.out.OAuthLoginCodePayload
import com.cleanreview.auth.application.port.out.OAuthLoginCodeStore
import java.security.MessageDigest
import org.springframework.stereotype.Service

data class ExchangeOAuthLoginCodeCommand(
    val code: String,
)

data class ExchangeOAuthLoginCodeResult(
    val accessToken: String,
    val userId: String,
    val email: String,
    val role: String,
)

object OAuthLoginCodeHasher {
    fun sha256(value: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray())
            .joinToString("") { "%02x".format(it) }
}

@Service
class ExchangeOAuthLoginCodeUseCase(
    private val oauthLoginCodeStore: OAuthLoginCodeStore,
) {
    fun execute(command: ExchangeOAuthLoginCodeCommand): ExchangeOAuthLoginCodeResult {
        require(command.code.isNotBlank()) { "OAuth login code must not be blank." }

        val payload = oauthLoginCodeStore.consumeLoginCode(OAuthLoginCodeHasher.sha256(command.code))
            ?: throw InvalidOAuthLoginCodeException()

        return payload.toResult()
    }

    private fun OAuthLoginCodePayload.toResult(): ExchangeOAuthLoginCodeResult =
        ExchangeOAuthLoginCodeResult(
            accessToken = accessToken,
            userId = userId,
            email = email,
            role = role.name,
        )
}

class InvalidOAuthLoginCodeException : RuntimeException("Invalid OAuth login code.")
