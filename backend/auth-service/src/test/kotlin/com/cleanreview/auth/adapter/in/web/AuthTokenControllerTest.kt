package com.cleanreview.auth.adapter.`in`.web

import jakarta.servlet.http.HttpServletResponse
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping

class AuthTokenControllerTest {
    @Test
    fun `auth token controller uses auth api prefix`() {
        val mapping = AuthTokenController::class.java.getAnnotation(RequestMapping::class.java)

        assertEquals("/api/v1/auth", mapping.value.single())
    }

    @Test
    fun `auth token controller exposes refresh and logout endpoints`() {
        val refresh = AuthTokenController::class.java.getDeclaredMethod(
            "refresh",
            String::class.java,
            HttpServletResponse::class.java,
        )
        val logout = AuthTokenController::class.java.getDeclaredMethod(
            "logout",
            String::class.java,
            HttpServletResponse::class.java,
        )

        assertEquals("/refresh", refresh.getAnnotation(PostMapping::class.java).value.single())
        assertEquals("/logout", logout.getAnnotation(PostMapping::class.java).value.single())
    }
}
