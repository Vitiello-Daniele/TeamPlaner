package de.teamplaner.data.auth

data class AuthSession(
    val token: String,
    val user: AuthUser
)

data class AuthUser(
    val id: String,
    val name: String,
    val email: String
)
