package com.cleanreview.auth.adapter.out.jwt

import com.cleanreview.auth.application.port.out.IssuedTokens
import com.cleanreview.auth.application.port.out.JwtTokenIssuer
import com.cleanreview.auth.domain.User
import java.security.MessageDigest
import java.time.Instant
import java.util.Base64
import java.util.UUID
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class HmacJwtTokenIssuer(
    @Value("\${clean-review.jwt.secret}") private val secret: String,
    @Value("\${clean-review.jwt.issuer:clean-review}") private val issuer: String,
) : JwtTokenIssuer {
    override fun issue(user: User): IssuedTokens {
        val refreshToken = UUID.randomUUID().toString() + "." + UUID.randomUUID()
        val refreshTokenHash = sha256(refreshToken)
        return IssuedTokens(
            accessToken = signAccessToken(user),
            refreshToken = refreshToken,
            refreshTokenHash = refreshTokenHash,
            refreshTokenTtlSeconds = 2_592_000,
        )
    }

    private fun signAccessToken(user: User): String {
        val now = Instant.now().epochSecond
        val header = """{"alg":"HS256","typ":"JWT"}"""
        val payload = """{"iss":"$issuer","sub":"${user.id.value}","email":"${user.email}","roles":["${user.role.name}"],"iat":$now,"exp":${now + 3600}}"""
        val signingInput = "${base64Url(header.toByteArray())}.${base64Url(payload.toByteArray())}"
        return "$signingInput.${hmacSha256(signingInput)}"
    }

    private fun hmacSha256(value: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(secret.toByteArray(), "HmacSHA256"))
        return base64Url(mac.doFinal(value.toByteArray()))
    }

    private fun sha256(value: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray())
            .joinToString("") { "%02x".format(it) }

    private fun base64Url(bytes: ByteArray): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
}
