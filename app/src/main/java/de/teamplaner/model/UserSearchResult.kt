package de.teamplaner.model

data class UserSearchResult(
    val id: String,
    val name: String,
    val email: String
) {
    val label: String
        get() = "$name - $email"
}
