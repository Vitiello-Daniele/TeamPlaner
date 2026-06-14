package de.teamplaner.model

data class Duty(
    val type: DutyType,
    val title: String,
    val description: String
)

enum class DutyType(val label: String, val defaultDescription: String) {
    Jersey("Trikotdienst", "Trikots nach dem Spiel mitnehmen und waschen"),
    Drinks("Getränkedienst", "Getränke für das Team organisieren"),
    Cleanup("Aufräumen", "Kabine und Material nach dem Termin aufräumen"),
    Driving("Fahrdienst", "Fahrt zum Termin organisieren"),
    Material("Materialdienst", "Bälle, Hütchen und weiteres Material vorbereiten"),
    Other("Sonstiges", "Eigener Dienst")
}

data class DutyAssignment(
    val event: TeamEvent,
    val duty: Duty,
    val member: TeamMember
)
