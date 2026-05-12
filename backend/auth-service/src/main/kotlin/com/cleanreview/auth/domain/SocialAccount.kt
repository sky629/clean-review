package com.cleanreview.auth.domain

data class SocialAccount(
    val userId: UserId,
    val provider: SocialProvider,
    val providerUserId: String,
) {
    init {
        require(providerUserId.isNotBlank()) { "Provider user id must not be blank." }
    }
}
