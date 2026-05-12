package com.cleanreview.common.security

data class AuthenticatedUser(
    val userId: String,
    val email: String?,
    val roles: Set<UserRole>,
) {
    fun hasRole(role: UserRole): Boolean = role in roles

    fun isAdmin(): Boolean = hasRole(UserRole.ADMIN)
}
