package com.cleanreview.common.security

import org.springframework.core.convert.converter.Converter
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.jwt.Jwt

class JwtAuthenticationConverter : Converter<Jwt, AbstractAuthenticationToken> {
    override fun convert(source: Jwt): AbstractAuthenticationToken {
        val roles = extractRoles(source)
        val authorities = buildSet {
            roles.mapTo(this) { SimpleGrantedAuthority("ROLE_${it.name}") }
            extractScopes(source).mapTo(this) { SimpleGrantedAuthority("SCOPE_$it") }
        }
        val principal = AuthenticatedUser(
            userId = source.subject,
            email = source.getClaimAsString("email"),
            roles = roles,
        )

        return AuthenticatedUserToken(principal, source, authorities)
    }

    private fun extractRoles(jwt: Jwt): Set<UserRole> {
        val claim = jwt.claims["roles"] ?: return emptySet()
        return when (claim) {
            is String -> claim.split(" ", ",")
            is Collection<*> -> claim.mapNotNull { it?.toString() }
            else -> emptyList()
        }.mapNotNull { UserRole.from(it) }.toSet()
    }

    private fun extractScopes(jwt: Jwt): Set<String> {
        val claim = jwt.claims["scope"] ?: jwt.claims["scp"] ?: return emptySet()
        return when (claim) {
            is String -> claim.split(" ")
            is Collection<*> -> claim.mapNotNull { it?.toString() }
            else -> emptyList()
        }.filter { it.isNotBlank() }.toSet()
    }
}

private class AuthenticatedUserToken(
    private val user: AuthenticatedUser,
    private val jwt: Jwt,
    authorities: Collection<GrantedAuthority>,
) : AbstractAuthenticationToken(authorities) {
    init {
        isAuthenticated = true
    }

    override fun getCredentials(): Any = jwt.tokenValue

    override fun getPrincipal(): Any = user
}
