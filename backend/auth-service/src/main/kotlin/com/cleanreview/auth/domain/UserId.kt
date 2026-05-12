package com.cleanreview.auth.domain

@JvmInline
value class UserId(val value: String) {
    init {
        require(value.isNotBlank()) { "User id must not be blank." }
    }
}
