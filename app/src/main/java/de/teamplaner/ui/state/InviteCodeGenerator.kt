package de.teamplaner.ui.state

class InviteCodeGenerator {
    fun create(teamName: String, number: Int): String {
        val codeBase = teamName
            .filter { it.isLetterOrDigit() }
            .uppercase()
            .take(4)

        return codeBase.ifBlank { "TEAM" } + number.toString().padStart(2, '0')
    }
}
