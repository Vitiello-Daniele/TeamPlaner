package de.teamplaner.ui.state

class IdGenerator {
    fun create(prefix: String): String {
        val cleanPrefix = prefix
            .lowercase()
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')
            .ifBlank { "id" }

        return "$cleanPrefix-${System.currentTimeMillis()}-${(1000..9999).random()}"
    }
}
