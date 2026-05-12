package com.cleanreview.auth.adapter.`in`.web

import kotlin.test.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping

class GoogleOAuthControllerTest {
    @Test
    fun `google oauth controller uses auth api prefix`() {
        val mapping = GoogleOAuthController::class.java.getAnnotation(RequestMapping::class.java)

        assertEquals("/api/v1/auth/google", mapping.value.single())
    }

    @Test
    fun `google oauth controller exposes spring oauth authorize url endpoint`() {
        val method = GoogleOAuthController::class.java.getDeclaredMethod("authorizeUrl", String::class.java)
        val mapping = method.getAnnotation(GetMapping::class.java)

        assertEquals("/authorize-url", mapping.value.single())
    }

    @Test
    fun `google oauth controller exchanges one time login code`() {
        val method = GoogleOAuthController::class.java.getDeclaredMethod(
            "exchangeLoginCode",
            ExchangeOAuthLoginCodeRequest::class.java,
        )
        val mapping = method.getAnnotation(PostMapping::class.java)

        assertEquals("/login-code/exchange", mapping.value.single())
    }
}
