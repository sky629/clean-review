package com.cleanreview.auth.infrastructure.cookie

import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseCookie
import org.springframework.stereotype.Component

@Component
class AuthCookieFactory(
    @Value("\${clean-review.cookie.secure:false}") private val secure: Boolean,
    @Value("\${clean-review.cookie.same-site:Lax}") private val sameSite: String,
    @Value("\${clean-review.cookie.path:/}") private val path: String,
    @Value("\${clean-review.cookie.domain:}") private val domain: String,
    @Value("\${clean-review.cookie.refresh-token-max-age-seconds:2592000}") private val refreshTokenMaxAgeSeconds: Long,
) {
    fun addRefreshTokenCookie(refreshToken: String, response: HttpServletResponse) {
        response.addHeader(
            HttpHeaders.SET_COOKIE,
            createRefreshTokenCookie(refreshToken, refreshTokenMaxAgeSeconds).toString(),
        )
    }

    fun deleteRefreshTokenCookie(response: HttpServletResponse) {
        response.addHeader(
            HttpHeaders.SET_COOKIE,
            createRefreshTokenCookie("", 0).toString(),
        )
    }

    private fun createRefreshTokenCookie(value: String, maxAge: Long): ResponseCookie {
        val builder = ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, value)
            .httpOnly(true)
            .secure(secure)
            .path(path)
            .sameSite(sameSite)
            .maxAge(maxAge)

        if (domain.isNotBlank()) {
            builder.domain(domain)
        }

        return builder.build()
    }

    companion object {
        const val REFRESH_TOKEN_COOKIE_NAME = "refresh_token"
    }
}
