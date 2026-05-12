package com.cleanreview.common.security

enum class UserRole {
    ADMIN,
    USER,
    REVIEWER,
    ;

    companion object {
        fun from(value: String): UserRole? = entries.firstOrNull { it.name == value.uppercase() }
    }
}
